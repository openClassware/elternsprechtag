package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.AuswertungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege.LehrauftragDaten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baut den Terminplan je beteiligter Lehrkraft.
 *
 * <p>Das Read-Modell entsteht aus <b>drei</b> Ports und wird hier in Java zusammengefügt: die
 * beteiligten Lehrkräfte aus der Schulorganisation, die Buchungszeilen und die Anzahl entfallener
 * Termine aus den Query-Ports dieses Kontexts. Kein SQL-Statement joint über die Kontextgrenze —
 * ein Sprechtag hat rund 30 Lehrkräfte, die Kosten sind vernachlässigbar (ADR 0003).
 *
 * <p>Beteiligt ist eine Lehrkraft, die einen Lehrauftrag in einer teilnehmenden Klasse hat — deshalb
 * erscheint auch, wer keine Buchung hat.
 */
@RequiredArgsConstructor
@Service
class AuswertenService implements Auswerten {

  private final SprechtagAnsichten sprechtagAnsichten;
  private final Lehrauftraege lehrauftraege;
  private final BuchungsAnsichten buchungsAnsichten;
  private final TerminAnsichten terminAnsichten;

  @Override
  @Transactional(readOnly = true)
  public Optional<SprechtagAuswertung> werteAus(UUID sprechtagId) {
    SprechtagId id = SprechtagId.von(sprechtagId);
    Optional<SprechtagAnsichten.Kopf> gefunden = sprechtagAnsichten.kopf(id);
    if (gefunden.isEmpty()) {
      return Optional.empty();
    }
    SprechtagAnsichten.Kopf kopf = gefunden.get();

    Map<UUID, Integer> entfalleneJeLehrkraft = new LinkedHashMap<>();
    for (TerminAnsichten.EntfalleneZeile zeile : terminAnsichten.entfalleneJeLehrkraft(id)) {
      entfalleneJeLehrkraft.put(zeile.lehrkraftId(), zeile.anzahl());
    }

    // Die Query liefert bereits chronologisch; die Gruppierung erhält die Reihenfolge, sodass jede
    // Zeilenliste sortiert bleibt. Geltende und stornierte Buchungen landen in getrennten Listen —
    // gezählt wird nur, was gilt (Issue #129).
    Map<UUID, List<BuchungsZeile>> zeilenJeLehrkraft = new LinkedHashMap<>();
    Map<UUID, List<BuchungsZeile>> stornierteJeLehrkraft = new LinkedHashMap<>();
    Map<UUID, AuswertungsZeile> ersteZeileJeLehrkraft = new LinkedHashMap<>();
    for (AuswertungsZeile roh : buchungsAnsichten.buchungenFuerAuswertung(id)) {
      ersteZeileJeLehrkraft.putIfAbsent(roh.lehrkraftId(), roh);
      (roh.storniert() ? stornierteJeLehrkraft : zeilenJeLehrkraft)
          .computeIfAbsent(roh.lehrkraftId(), k -> new ArrayList<>())
          .add(
              new BuchungsZeile(
                  roh.buchungId(),
                  roh.startzeit(),
                  roh.schuelerName(),
                  roh.klasse(),
                  roh.fach(),
                  roh.elternName(),
                  roh.notiz(),
                  roh.storniert(),
                  roh.entfallen(),
                  roh.anonymisiertAm() == null ? null : roh.anonymisiertAm().toLocalDate()));
    }

    List<LehrkraftPlan> plaene = new ArrayList<>();
    // Beteiligt ist, wer einen Lehrauftrag in einer teilnehmenden Klasse hat — deshalb erscheint
    // auch, wer keine Buchung hat.
    for (LehrauftragDaten lehrkraft :
        Lehrkraftauswahl.jeLehrkraft(lehrauftraege, kopf.klasseIds())) {
      UUID lehrerId = lehrkraft.lehrkraft().wert();
      List<BuchungsZeile> zeilen = zeilenJeLehrkraft.getOrDefault(lehrerId, List.of());
      plaene.add(
          new LehrkraftPlan(
              lehrerId,
              lehrkraft.kuerzel(),
              lehrkraft.anzeigeName(),
              zeilen.size(),
              entfalleneJeLehrkraft.getOrDefault(lehrerId, 0),
              zeilen,
              stornierteJeLehrkraft.getOrDefault(lehrerId, List.of())));
      ersteZeileJeLehrkraft.remove(lehrerId);
    }
    // Was jetzt noch übrig ist, gehört Lehrkräften ohne aktuellen Lehrauftrag — ein Import hat ihn
    // entfernt, nachdem gebucht wurde. Ihre Buchungen bleiben trotzdem sichtbar: Der Terminplan
    // eines Sprechtags soll nicht davon abhängen, was die Stammdaten heute sagen. Name und Kürzel
    // stammen aus der Buchung selbst, und diese Pläne stehen am Ende — einen Nachnamen, nach dem
    // sich einsortieren ließe, gibt der eingefrorene Anzeigename nicht her.
    for (Map.Entry<UUID, AuswertungsZeile> uebrig : ersteZeileJeLehrkraft.entrySet()) {
      UUID lehrerId = uebrig.getKey();
      AuswertungsZeile eingefroren = uebrig.getValue();
      List<BuchungsZeile> zeilen = zeilenJeLehrkraft.getOrDefault(lehrerId, List.of());
      plaene.add(
          new LehrkraftPlan(
              lehrerId,
              eingefroren.lehrkraftKuerzel(),
              eingefroren.lehrkraftName(),
              zeilen.size(),
              entfalleneJeLehrkraft.getOrDefault(lehrerId, 0),
              zeilen,
              stornierteJeLehrkraft.getOrDefault(lehrerId, List.of())));
    }
    LocalDate anonymisiertAm =
        kopf.anonymisiertAm() == null ? null : kopf.anonymisiertAm().toLocalDate();
    return Optional.of(
        new SprechtagAuswertung(
            kopf.titel(), kopf.datum(), kopf.status(), anonymisiertAm, plaene));
  }

}
