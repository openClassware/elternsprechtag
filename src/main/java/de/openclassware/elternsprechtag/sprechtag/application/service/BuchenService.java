package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Ereignisse;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege.LehrauftragDaten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungAngelegt;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungenBestaetigt;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsziel;
import de.openclassware.elternsprechtag.sprechtag.domain.Ereignis;
import de.openclassware.elternsprechtag.sprechtag.domain.Familie;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrauftragId;
import de.openclassware.elternsprechtag.sprechtag.domain.Notiz;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schreibt einen Buchungsvorgang fest — alles oder nichts.
 *
 * <p>Dies ist die <b>einzige</b> Stelle im Projekt, die mehrere Aggregate in einer Transaktion
 * ändert. N Terminwünsche heißen N Slots bei N verschiedenen Lehrkräften, also N
 * {@link Termin}-Aggregate; dass eine Familie nicht mit zwei Terminen und zwei Absagen dasteht, ist
 * eine Produktentscheidung. Der Bruch der Regel „eine Transaktion, ein Aggregat" ist in ADR 0005
 * benannt und begründet.
 *
 * <p>Der Mechanismus: Jedes Aggregat wird sofort gespeichert, damit ein Versionskonflikt hier im
 * {@code try} auftritt und nicht erst beim Commit. Die Prüfung selbst liegt im Aggregat
 * ({@link Termin#buche}); ein Konflikt am optimistischen Lock ist fachlich derselbe Fall und wird zu
 * {@link TerminBelegtException}.
 *
 * <p>Die Ereignisse werden den Aggregaten abgeholt und erst am Ende zu <em>einem</em>
 * {@link BuchungenBestaetigt} gebündelt — eine Familie mit vier Terminen bekommt eine Mail. Rollt die
 * Transaktion zurück, wird nie gebündelt und nie veröffentlicht.
 */
@RequiredArgsConstructor
@Service
class BuchenService implements Buchen {

  private final Termine termine;
  private final Lehrauftraege lehrauftraege;
  private final Ereignisse ereignisse;

  @Override
  @Transactional
  public int buchen(BuchungsAnfrage anfrage) {
    Familie familie =
        new Familie(anfrage.elternName(), anfrage.schuelerName(), anfrage.elternEmail());
    LocalDateTime jetzt = LocalDateTime.now();
    List<BuchungId> gebucht = new ArrayList<>();
    try {
      for (BuchungsWunsch wunsch : anfrage.wuensche()) {
        TerminId terminId = TerminId.von(wunsch.terminId());
        Termin termin =
            termine
                .lade(terminId)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Termin nicht gefunden: " + wunsch.terminId()));

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
      ereignisse.veroeffentliche(new BuchungenBestaetigt(gebucht));
    }
    return gebucht.size();
  }

  /**
   * Friert den Lehrauftrag zum Buchungsziel ein. Ab hier ist die Buchung von den Stammdaten
   * unabhängig; die {@link LehrauftragId} bleibt nur als Herkunftsspur.
   */
  private Buchungsziel ziel(BuchungsWunsch wunsch) {
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
   */
  private static List<BuchungId> angelegteBuchungen(Termin termin) {
    List<BuchungId> ids = new ArrayList<>();
    for (Ereignis ereignis : termin.ereignisseAbholen()) {
      if (ereignis instanceof BuchungAngelegt angelegt) {
        ids.add(angelegt.buchung());
      }
    }
    return ids;
  }
}
