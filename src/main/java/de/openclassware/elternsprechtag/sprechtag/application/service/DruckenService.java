package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Drucken;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege.LehrauftragDaten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Blatt;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Kopf;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Zeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten.TagesplanZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baut die Tagespläne aller beteiligten Lehrkräfte und reicht sie an den Druck weiter.
 *
 * <p>Wie die Auswertung aus drei Quellen, in Java zusammengefügt — kein SQL joint über die
 * Kontextgrenze (ADR 0003): die Kopfdaten, die beteiligten Lehrkräfte aus der Schulorganisation
 * und die Slots samt geltender Buchung aus dem Query-Port dieses Kontexts.
 *
 * <p>Beteiligt ist, wer einen Lehrauftrag in einer teilnehmenden Klasse hat — dieselbe Regel wie
 * in der Auswertung, deshalb bekommt auch eine Lehrkraft ohne Buchung ihr Blatt. Dazu kommt, wer
 * keinen Lehrauftrag mehr hat, aber eine geltende Buchung: Name und Kürzel stammen dann aus der
 * Buchung, das Blatt steht am Ende. Eine Lehrkraft ohne Lehrauftrag und ohne geltende Buchung hat
 * keinen Namen, den das Blatt tragen könnte, und fehlt.
 */
@RequiredArgsConstructor
@Service
class DruckenService implements Drucken {

  private final SprechtagAnsichten sprechtagAnsichten;
  private final Lehrauftraege lehrauftraege;
  private final TerminAnsichten terminAnsichten;
  private final Tagesplandruck tagesplandruck;

  @Override
  @Transactional(readOnly = true)
  public Optional<Datei> druckePlaene(UUID sprechtagId) {
    SprechtagId id = SprechtagId.von(sprechtagId);
    Optional<SprechtagAnsichten.Kopf> gefunden = sprechtagAnsichten.kopf(id);
    if (gefunden.isEmpty()) {
      return Optional.empty();
    }
    SprechtagAnsichten.Kopf sprechtag = gefunden.get();
    LocalDate anonymisiertAm =
        sprechtag.anonymisiertAm() == null ? null : sprechtag.anonymisiertAm().toLocalDate();

    // Die Query liefert chronologisch; die Gruppierung erhält die Reihenfolge je Lehrkraft.
    Map<UUID, List<TagesplanZeile>> slotsJeLehrkraft = new LinkedHashMap<>();
    for (TagesplanZeile slot : terminAnsichten.tagesplan(id)) {
      slotsJeLehrkraft.computeIfAbsent(slot.lehrkraftId(), k -> new ArrayList<>()).add(slot);
    }

    List<Blatt> blaetter = new ArrayList<>();
    for (LehrauftragDaten lehrkraft :
        Lehrkraftauswahl.jeLehrkraft(lehrauftraege, sprechtag.klasseIds())) {
      List<TagesplanZeile> slots = slotsJeLehrkraft.remove(lehrkraft.lehrkraft().wert());
      blaetter.add(
          new Blatt(
              lehrkraft.kuerzel(),
              lehrkraft.anzeigeName(),
              zeilen(slots == null ? List.of() : slots, anonymisiertAm)));
    }
    for (List<TagesplanZeile> slots : slotsJeLehrkraft.values()) {
      slots.stream()
          .filter(TagesplanZeile::gebucht)
          .findFirst()
          .ifPresent(
              eingefroren ->
                  blaetter.add(
                      new Blatt(
                          eingefroren.lehrkraftKuerzel(),
                          eingefroren.lehrkraftName(),
                          zeilen(slots, anonymisiertAm))));
    }

    Kopf kopf = new Kopf(sprechtag.titel(), sprechtag.datum(), sprechtag.ort(), anonymisiertAm);
    return Optional.of(tagesplandruck.gebuendelt(kopf, blaetter, jetzt()));
  }

  /**
   * Der Vermerk „Angaben entfernt am …" steht nur an der Zeile, solange der Sprechtag nicht ohnehin
   * anonymisiert ist — dann erklärt der Kopf alles. Dieselbe Regel wie in der Auswertung.
   */
  private static List<Zeile> zeilen(
      List<TagesplanZeile> slots, LocalDate sprechtagAnonymisiertAm) {
    return slots.stream()
        .map(
            slot ->
                new Zeile(
                    slot.startzeit(),
                    !slot.gebucht(),
                    slot.schuelerName(),
                    slot.klasse(),
                    slot.fach(),
                    slot.elternName(),
                    slot.notiz(),
                    sprechtagAnonymisiertAm != null || slot.anonymisiertAm() == null
                        ? null
                        : slot.anonymisiertAm().toLocalDate()))
        .toList();
  }

  /** Minutengenau — Sekunden auf einem Papierblatt sagen niemandem etwas. */
  private static LocalDateTime jetzt() {
    return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
  }
}
