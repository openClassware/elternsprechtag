package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
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

  /**
   * Alle Slots dieser Lehrkraft an diesem Sprechtag, chronologisch — das Angebot des
   * Ausfall-Dialogs (Sammelaktion „Lehrkraft fällt aus", Issue #156). Anders als {@link #slots}
   * unterscheidet diese Scheibe die drei Zustände eines Slots und trägt bei einer aktiven Buchung
   * deren Beleg-Daten mit.
   */
  List<AusfallZeile> ausfallSlots(SprechtagId sprechtag, LehrkraftId lehrkraft);

  /** Zustand eines Slots aus Sicht der Ausfall-Sammelaktion. */
  enum AusfallSlotZustand {
    FREI,
    GEBUCHT,
    ENTFALLEN
  }

  /**
   * Ein Slot im Ausfall-Dialog. {@code schuelerName}, {@code elternName} und
   * {@code familienSchluessel} sind nur bei {@link AusfallSlotZustand#GEBUCHT} gesetzt, sonst
   * {@code null}. Der Familien-Schlüssel ist die Eltern-Adresse — sie wird im Dialog nicht
   * angezeigt, sondern dient nur der lokalen Zählung betroffener Familien.
   */
  record AusfallZeile(
      UUID terminId,
      LocalTime zeit,
      AusfallSlotZustand zustand,
      String schuelerName,
      String elternName,
      String familienSchluessel) {}

  /**
   * Anzahl entfallener Termine je Lehrkraft an diesem Sprechtag — die dritte Quelle der
   * Auswertung (Issue #160), neben den Lehrkräften aus der Schulorganisation und den
   * Buchungszeilen aus {@link BuchungsAnsichten}. Nur Lehrkräfte mit mindestens einem entfallenen
   * Termin sind enthalten.
   */
  List<EntfalleneZeile> entfalleneJeLehrkraft(SprechtagId sprechtag);

  /** {@code anzahl} ist stets größer null — wer keinen entfallenen Termin hat, fehlt in der Liste. */
  record EntfalleneZeile(UUID lehrkraftId, int anzahl) {}
}
