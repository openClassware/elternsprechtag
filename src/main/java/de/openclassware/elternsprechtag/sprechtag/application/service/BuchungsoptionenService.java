package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege.LehrauftragDaten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminAnsichten.SlotZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baut die Lehrkraft-Auswahl der Eltern-Ansicht: je Lehrkraft der gewählten Klasse ihre Slots, der
 * für (Klasse, Lehrkraft) aufgelöste Lehrauftrag und die Fächer der Lehrkraft an diesem Sprechtag.
 *
 * <p>Wie die Auswertung fügt dieser Use Case zwei Ports in Java zusammen — Stammdaten aus der
 * Schulorganisation, Slots aus dem Query-Port dieses Kontexts.
 */
@RequiredArgsConstructor
@Service
class BuchungsoptionenService implements Buchungsoptionen {

  private final SprechtagAnsichten sprechtagAnsichten;
  private final Lehrauftraege lehrauftraege;
  private final TerminAnsichten terminAnsichten;

  @Override
  @Transactional(readOnly = true)
  public List<LehrkraftOption> ladeLehrkraftOptionen(UUID sprechtagId, UUID klasseId) {
    SprechtagId id = SprechtagId.von(sprechtagId);
    SprechtagAnsichten.Kopf kopf =
        sprechtagAnsichten
            .kopf(id)
            .orElseThrow(
                () -> new IllegalArgumentException("Sprechtag nicht gefunden: " + sprechtagId));

    Map<UUID, List<SlotOption>> slotsJeLehrkraft = new LinkedHashMap<>();
    for (SlotZeile slot : terminAnsichten.slots(id)) {
      slotsJeLehrkraft
          .computeIfAbsent(slot.lehrkraftId(), k -> new ArrayList<>())
          .add(new SlotOption(slot.terminId(), slot.zeit(), slot.buchbar()));
    }

    // Fächer je Lehrkraft über alle teilnehmenden Klassen des Sprechtags (Scope: dieser Sprechtag) —
    // nur informativ, deshalb bewusst weiter gefasst als die gewählte Klasse.
    Map<UUID, TreeSet<String>> faecherJeLehrkraft = new LinkedHashMap<>();
    for (UUID teilnehmende : kopf.klasseIds()) {
      for (LehrauftragDaten auftrag : lehrauftraege.fuerKlasse(teilnehmende)) {
        faecherJeLehrkraft
            .computeIfAbsent(auftrag.lehrkraft().wert(), k -> new TreeSet<>())
            .add(auftrag.fach());
      }
    }

    // Zur Wahl stehen nur die Lehrkräfte der gewählten Klasse — dieselbe Regel wie in der
    // Auswertung, nur auf eine Klasse angewandt.
    List<LehrkraftOption> optionen = new ArrayList<>();
    for (LehrauftragDaten auftrag :
        Lehrkraftauswahl.jeLehrkraft(lehrauftraege, List.of(klasseId))) {
      UUID lehrerId = auftrag.lehrkraft().wert();
      optionen.add(
          new LehrkraftOption(
              auftrag.id().wert(),
              lehrerId,
              auftrag.kuerzel(),
              auftrag.anzeigeName(),
              new ArrayList<>(faecherJeLehrkraft.getOrDefault(lehrerId, new TreeSet<>())),
              slotsJeLehrkraft.getOrDefault(lehrerId, List.of())));
    }
    return optionen;
  }
}
