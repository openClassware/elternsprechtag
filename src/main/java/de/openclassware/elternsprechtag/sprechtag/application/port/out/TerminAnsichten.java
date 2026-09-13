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
   * Ein Slot, wie die Datenbank ihn kennt. {@code buchbar} ist abgeleitet — angeboten und ohne
   * aktive Buchung; es gibt keine Spalte dafür. Dieselbe Frage, die
   * {@code Termin.istBuchbar()} am Aggregat beantwortet, und deshalb dasselbe Wort.
   *
   * <p>Warum ein Flag und nicht der Zustand: Diese Scheibe unterscheidet auf der Leseseite noch
   * nicht zwischen „vergeben" und „entfällt" — die Eltern-Ansicht kennt den dritten Zustand noch
   * nicht (`ABDECKUNG.md` Z. 230). Gespeichert ist er, sichtbar wird er mit der Ausfall-Strecke.
   */
  record SlotZeile(UUID terminId, UUID lehrkraftId, LocalTime zeit, boolean buchbar) {}
}
