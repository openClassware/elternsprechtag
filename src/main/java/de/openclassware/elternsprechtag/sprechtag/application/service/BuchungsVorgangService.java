package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Ereignisse;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege.LehrauftragDaten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.Anlass;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungAngelegt;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungenBestaetigt;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsziel;
import de.openclassware.elternsprechtag.sprechtag.domain.Ereignis;
import de.openclassware.elternsprechtag.sprechtag.domain.Familie;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrauftragId;
import de.openclassware.elternsprechtag.sprechtag.domain.Notiz;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import de.openclassware.elternsprechtag.sprechtag.domain.ZeitkonfliktException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Die geteilte Mechanik hinter jedem Weg, der einen Buchungsvorgang festschreibt — heute
 * {@code BuchenService} (Eltern-Submit) und {@code NachtragenService} (Organizer-Nachtrag), beide
 * mit ihren eigenen Vorbedingungen davor.
 *
 * <p>Dies ist die <b>einzige</b> Stelle im Projekt, die mehrere Aggregate in einer Transaktion
 * ändert. N Terminwünsche heißen N Slots bei N verschiedenen Lehrkräften, also N {@link
 * Termin}-Aggregate; dass eine Familie nicht mit zwei Terminen und zwei Absagen dasteht, ist eine
 * Produktentscheidung. Der Bruch der Regel „eine Transaktion, ein Aggregat" ist in ADR 0005 benannt
 * und begründet — sie nennt ausdrücklich auch den Organizer-Nachtrag.
 *
 * <p>Der Mechanismus: Jedes Aggregat wird sofort gespeichert, damit ein Versionskonflikt hier im
 * {@code try} auftritt und nicht erst beim Commit. Die Prüfung selbst liegt im Aggregat ({@link
 * Termin#buche}); ein Konflikt am optimistischen Lock ist fachlich derselbe Fall und wird zu
 * {@link TerminBelegtException}.
 *
 * <p>Die Ereignisse werden den Aggregaten abgeholt und erst am Ende zu <em>einem</em> {@link
 * BuchungenBestaetigt} gebündelt — eine Familie mit vier Terminen bekommt eine Mail. Rollt die
 * Transaktion zurück, wird nie gebündelt und nie veröffentlicht.
 *
 * <p>Keine Basisklasse, keine Vererbung: Die Aufrufer rufen diesen Service genauso auf wie jeden
 * anderen Kollaborator. Bewusst nicht {@code @Transactional} — die Transaktionsgrenze gehört dem
 * Use Case, der auch die eigene Vorbedingung (z. B. Zeitkonflikt- oder Veröffentlichungsprüfung)
 * innerhalb derselben Transaktion durchsetzt.
 */
@RequiredArgsConstructor
@Service
class BuchungsVorgangService {

  private final Termine termine;
  private final Sprechtage sprechtage;
  private final Lehrauftraege lehrauftraege;
  private final Ereignisse ereignisse;

  /** Ein einzelner gewünschter Termin, unabhängig davon, über welchen Port er hereinkam. */
  record Wunsch(UUID lehrauftragId, UUID terminId, String notiz) {}

  /**
   * Die Sprechtage der gewünschten Termine, je Wunsch einer — damit jeder Aufrufer seine eigene
   * Vorbedingung am Aggregat fragen kann, bevor irgendetwas geschrieben wird. Mehrfaches Laden
   * desselben Sprechtags ist hingenommen: Ein Vorgang trägt wenige Wünsche.
   */
  List<Sprechtag> sprechtageDer(List<Wunsch> wuensche) {
    List<Sprechtag> geladen = new ArrayList<>();
    for (Wunsch wunsch : wuensche) {
      Termin termin =
          termine
              .lade(TerminId.von(wunsch.terminId()))
              .orElseThrow(
                  () -> new IllegalArgumentException("Termin nicht gefunden: " + wunsch.terminId()));
      geladen.add(
          sprechtage
              .lade(termin.sprechtag())
              .orElseThrow(
                  () -> new IllegalStateException("Termin ohne Sprechtag: " + termin.id().wert())));
    }
    return geladen;
  }

  /**
   * Lädt jeden gewünschten Termin genau einmal und weist den Vorgang ab, sobald zwei Wünsche auf
   * dieselbe Uhrzeit fallen — Aussagen über mehrere {@link Termin}e kann kein einzelnes Aggregat
   * treffen, deshalb sitzt die Prüfung hier statt in {@link Termin#buche}. Gilt nur innerhalb
   * dieses einen Vorgangs; zwei Familien in getrennten Vorgängen dürfen dieselbe Uhrzeit bei
   * verschiedenen Lehrkräften wählen.
   */
  List<Termin> ladeUndPruefeZeitkonflikt(List<Wunsch> wuensche) {
    List<Termin> geladen = new ArrayList<>();
    Set<LocalTime> uhrzeiten = new HashSet<>();
    for (Wunsch wunsch : wuensche) {
      TerminId terminId = TerminId.von(wunsch.terminId());
      Termin termin =
          termine
              .lade(terminId)
              .orElseThrow(
                  () -> new IllegalArgumentException("Termin nicht gefunden: " + wunsch.terminId()));
      if (!uhrzeiten.add(termin.zeitraum().uhrzeit())) {
        throw new ZeitkonfliktException(
            "Zwei Wünsche dieses Vorgangs fallen auf dieselbe Uhrzeit: "
                + termin.zeitraum().uhrzeit());
      }
      geladen.add(termin);
    }
    return geladen;
  }

  /**
   * Schreibt den Vorgang als N Buchungen fest — alles oder nichts — und veröffentlicht am Ende
   * genau ein gebündeltes {@link BuchungenBestaetigt}, sofern mindestens eine Buchung entstand.
   * {@code geladen} muss aus {@link #ladeUndPruefeZeitkonflikt(List)} desselben Vorgangs stammen,
   * in derselben Reihenfolge wie {@code wuensche}.
   */
  int schreibeFest(Familie familie, List<Wunsch> wuensche, List<Termin> geladen) {
    LocalDateTime jetzt = LocalDateTime.now();
    List<BuchungId> gebucht = new ArrayList<>();
    try {
      for (int i = 0; i < wuensche.size(); i++) {
        Wunsch wunsch = wuensche.get(i);
        Termin termin = geladen.get(i);
        termin.buche(familie, ziel(wunsch), Notiz.vielleicht(wunsch.notiz()).orElse(null), jetzt);
        // Sofort speichern, damit ein Versionskonflikt hier auftritt und nicht erst beim Commit,
        // wo ihn kein catch mehr in TerminBelegtException übersetzen könnte.
        termine.speichere(termin);
        gebucht.addAll(angelegteBuchungen(termin));
      }
    } catch (OptimisticLockingFailureException konflikt) {
      // Zwei Familien haben denselben Slot gleichzeitig gebucht — fachlich derselbe Fall.
      throw new TerminBelegtException("Ein gewählter Termin wurde soeben vergeben.");
    }
    if (!gebucht.isEmpty()) {
      // Eltern-Submit und Organizer-Nachtrag sind derselbe Anlass — beide listen den vollständigen
      // Vorgang. Das Umbuchen baut sein Ereignis nicht über diesen Service, weil es Storno und
      // Neubuchung in einem Zug mischt (siehe UmbuchenService).
      ereignisse.veroeffentliche(new BuchungenBestaetigt(gebucht, Anlass.BUCHUNG));
    }
    return gebucht.size();
  }

  /**
   * Friert den Lehrauftrag zum Buchungsziel ein. Ab hier ist die Buchung von den Stammdaten
   * unabhängig; die {@link LehrauftragId} bleibt nur als Herkunftsspur.
   */
  private Buchungsziel ziel(Wunsch wunsch) {
    LehrauftragId lehrauftragId = LehrauftragId.von(wunsch.lehrauftragId());
    LehrauftragDaten auftrag =
        lehrauftraege
            .lade(lehrauftragId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Lehrauftrag nicht gefunden: " + wunsch.lehrauftragId()));
    return new Buchungsziel(
        auftrag.id(),
        auftrag.lehrkraft(),
        auftrag.anzeigeName(),
        auftrag.kuerzel(),
        auftrag.klasse(),
        auftrag.fach());
  }

  /**
   * Holt dem Aggregat ab, was es gemeldet hat, und behält die Buchungs-Ids. Abgeholt wird genau
   * einmal — der Puffer ist danach leer, und nur deshalb bündelt der Vorgang jede Buchung einmal.
   *
   * <p>Paketsichtbar statt privat: {@code UmbuchenService} mischt Storno und Neubuchung in einem
   * Zug und braucht dieselbe Abholung für sein eigenes, gebündeltes Ereignis.
   */
  static List<BuchungId> angelegteBuchungen(Termin termin) {
    List<BuchungId> ids = new ArrayList<>();
    for (Ereignis ereignis : termin.ereignisseAbholen()) {
      if (ereignis instanceof BuchungAngelegt angelegt) {
        ids.add(angelegt.buchung());
      }
    }
    return ids;
  }
}
