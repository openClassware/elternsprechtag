package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Query-Port der Terminseite: handgeschriebenes SQL, das die Aggregate umgeht. Die Slot-Anzeige
 * eines Sprechtags über Aggregate zu bauen wären rund 600 Ladevorgänge (ADR 0003).
 */
public interface TerminAnsichten {

  /** Alle Slots eines Sprechtags, chronologisch. */
  List<SlotZeile> slots(SprechtagId sprechtag);

  /**
   * Ein Slot, wie die Datenbank ihn kennt. {@code belegt} ist abgeleitet — aktive Buchung vorhanden
   * oder Verfügbarkeit {@code ENTFAELLT}; es gibt keine Spalte dafür.
   */
  record SlotZeile(UUID terminId, UUID lehrkraftId, LocalTime zeit, boolean belegt) {}
}
