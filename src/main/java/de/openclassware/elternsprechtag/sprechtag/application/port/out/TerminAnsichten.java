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
   * Alle Slots <em>einer</em> Lehrkraft an diesem Sprechtag, chronologisch — die Vorlage des
   * Ausfall-Dialogs (#156). Anders als {@link #slots(SprechtagId)} führt diese Scheibe den dritten
   * Zustand mit: Der Organizer soll sehen, was schon entfallen ist, statt es ein zweites Mal
   * anzukreuzen.
   *
   * <p>Ein Slot ergibt <b>eine</b> Zeile, auch wenn er in seiner Historie mehrere stornierte
   * Buchungen trägt: Angehängt wird nur die aktive.
   */
  List<LehrkraftSlotZeile> slotsDerLehrkraft(SprechtagId sprechtag, LehrkraftId lehrkraft);

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
   * Ein Slot einer Lehrkraft samt seiner aktiven Buchung. {@code entfaellt} ist die gespeicherte
   * Absicht des Organizers; die drei Buchungsfelder sind {@code null}, solange niemand gebucht hat.
   * Aus beidem zusammen leitet der Use Case den Zustand ab — frei, gebucht oder entfallen.
   *
   * <p>Die {@code elternAdresse} ist der Familien-Schlüssel und wird <b>nicht angezeigt</b>: Sie
   * trägt die Zeile nur, damit der Dialog „etwa M Familien" aus der Auswahl rechnen kann, ohne je
   * Häkchen eine Query zu stellen. Die Adresse ist der Bündelungsschlüssel überall im Kontext — eine
   * Eltern-Entity gibt es bewusst nicht.
   */
  record LehrkraftSlotZeile(
      UUID terminId,
      LocalTime zeit,
      boolean entfaellt,
      String schuelerName,
      String elternName,
      String elternAdresse) {}
}
