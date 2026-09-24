package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Use Case: was die Eltern-Ansicht zur Auswahl stellt — je Lehrkraft der gewählten Klasse ihre
 * Slots.
 *
 * <p>Das ist ein Read-Modell und darf veraltet sein: Ein hier als frei gezeigter Slot kann beim
 * Submit vergeben sein. Die Entscheidung fällt erneut am Aggregat (ADR 0003).
 */
public interface Buchungsoptionen {

  List<LehrkraftOption> ladeLehrkraftOptionen(UUID sprechtagId, UUID klasseId);

  /**
   * Ein Slot in der Eltern-Ansicht. {@code buchbar} heißt „steht zur Wahl": angeboten und ohne
   * aktive Buchung. Dieselbe Frage und dasselbe Wort wie {@code Termin.istBuchbar()} — und dieselbe
   * Antwort, nur womöglich eine ältere.
   *
   * <p>Warum die beiden Gründe, nicht zu buchen, hier nicht unterschieden sind: Die Eltern-Ansicht
   * fasst „belegt" und „entfällt" bewusst zusammen — die Buchung ist ohnehin doppelt verhindert
   * (`ABDECKUNG.md`, Phase 4).
   */
  record SlotOption(UUID terminId, LocalTime zeit, boolean buchbar) {}

  /**
   * Eine wählbare Lehrkraft der gewählten Klasse samt ihrer Slots. {@code lehrauftragId} ist der für
   * (Klasse, Lehrkraft) aufgelöste Lehrauftrag (Buchungs-Ziel); {@code faecher} listet die Fächer
   * der Lehrkraft an diesem Sprechtag (nur informativ).
   */
  record LehrkraftOption(
      UUID lehrauftragId,
      UUID lehrerId,
      String kuerzel,
      String lehrerName,
      List<String> faecher,
      List<SlotOption> slots) {}
}
