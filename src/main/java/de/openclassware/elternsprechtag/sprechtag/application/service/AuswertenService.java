package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.AuswertungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege.LehrauftragDaten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
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
 * <p>Das Read-Modell entsteht aus <b>zwei</b> Ports und wird hier in Java zusammengefügt: die
 * beteiligten Lehrkräfte aus der Schulorganisation, die Buchungszeilen aus dem Query-Port dieses
 * Kontexts. Kein SQL-Statement joint über die Kontextgrenze — ein Sprechtag hat rund 30 Lehrkräfte,
 * die Kosten sind vernachlässigbar (ADR 0003).
 *
 * <p>Beteiligt ist eine Lehrkraft, die einen Lehrauftrag in einer teilnehmenden Klasse hat — deshalb
 * erscheint auch, wer keine Buchung hat.
 */
@RequiredArgsConstructor
@Service
class AuswertenService implements Auswerten {

  private final Sprechtage sprechtage;
  private final Lehrauftraege lehrauftraege;
  private final BuchungsAnsichten buchungsAnsichten;

  @Override
  @Transactional(readOnly = true)
  public Optional<SprechtagAuswertung> werteAus(UUID sprechtagId) {
    SprechtagId id = SprechtagId.von(sprechtagId);
    Optional<Sprechtage.Kopf> gefunden = sprechtage.ladeKopf(id);
    if (gefunden.isEmpty()) {
      return Optional.empty();
    }
    Sprechtage.Kopf kopf = gefunden.get();

    // Die Query liefert bereits chronologisch; die Gruppierung erhält die Reihenfolge, sodass jede
    // Zeilenliste sortiert bleibt.
    Map<UUID, List<BuchungsZeile>> zeilenJeLehrkraft = new LinkedHashMap<>();
    Map<UUID, AuswertungsZeile> ersteZeileJeLehrkraft = new LinkedHashMap<>();
    for (AuswertungsZeile roh : buchungsAnsichten.aktiveBuchungen(id)) {
      ersteZeileJeLehrkraft.putIfAbsent(roh.lehrkraftId(), roh);
      zeilenJeLehrkraft
          .computeIfAbsent(roh.lehrkraftId(), k -> new ArrayList<>())
          .add(
              new BuchungsZeile(
                  roh.startzeit(),
                  roh.schuelerName(),
                  roh.klasse(),
                  roh.fach(),
                  roh.elternName(),
                  roh.notiz()));
    }

    List<LehrkraftPlan> plaene = new ArrayList<>();
    for (LehrauftragDaten lehrkraft : beteiligteLehrkraefte(kopf.klasseIds())) {
      UUID lehrerId = lehrkraft.lehrkraft().wert();
      List<BuchungsZeile> zeilen = zeilenJeLehrkraft.remove(lehrerId);
      if (zeilen == null) {
        zeilen = List.of();
      }
      plaene.add(
          new LehrkraftPlan(
              lehrerId, lehrkraft.kuerzel(), lehrkraft.anzeigeName(), zeilen.size(), zeilen));
    }
    // Was jetzt noch übrig ist, gehört Lehrkräften ohne aktuellen Lehrauftrag — ein Import hat ihn
    // entfernt, nachdem gebucht wurde. Ihre Buchungen bleiben trotzdem sichtbar: Der Terminplan
    // eines Sprechtags soll nicht davon abhängen, was die Stammdaten heute sagen. Name und Kürzel
    // stammen aus der Buchung selbst, und diese Pläne stehen am Ende — einen Nachnamen, nach dem
    // sich einsortieren ließe, gibt der eingefrorene Anzeigename nicht her.
    for (Map.Entry<UUID, List<BuchungsZeile>> uebrig : zeilenJeLehrkraft.entrySet()) {
      AuswertungsZeile eingefroren = ersteZeileJeLehrkraft.get(uebrig.getKey());
      plaene.add(
          new LehrkraftPlan(
              uebrig.getKey(),
              eingefroren.lehrkraftKuerzel(),
              eingefroren.lehrkraftName(),
              uebrig.getValue().size(),
              uebrig.getValue()));
    }
    return Optional.of(new SprechtagAuswertung(kopf.titel(), kopf.datum(), plaene));
  }

  /**
   * Je Lehrkraft der teilnehmenden Klassen ein Eintrag, dedupliziert und nach Nachname sortiert.
   * Welcher ihrer Lehraufträge den Eintrag stellt, ist gleichgültig — Kürzel und Name sind daran
   * dieselben.
   */
  private List<LehrauftragDaten> beteiligteLehrkraefte(List<UUID> klasseIds) {
    Map<UUID, LehrauftragDaten> jeLehrkraft = new LinkedHashMap<>();
    for (UUID klasseId : klasseIds) {
      for (LehrauftragDaten auftrag : lehrauftraege.fuerKlasse(klasseId)) {
        jeLehrkraft.putIfAbsent(auftrag.lehrkraft().wert(), auftrag);
      }
    }
    List<LehrauftragDaten> lehrkraefte = new ArrayList<>(jeLehrkraft.values());
    lehrkraefte.sort(
        Comparator.comparing(LehrauftragDaten::nachname, String.CASE_INSENSITIVE_ORDER));
    return lehrkraefte;
  }
}
