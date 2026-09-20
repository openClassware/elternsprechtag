package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.BuchungBereitsStorniertException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Use Case: Der Organizer verschiebt eine bestehende Buchung auf einen anderen freien Slot
 * <b>derselben Lehrkraft</b> — Storno und Neubuchung in einem Zug (ADR 0005), damit die Familie nie
 * ohne Termin dasteht. Nur die Uhrzeit ändert sich: Familie, Notiz und das eingefrorene
 * Buchungsziel wandern unverändert von der alten in die neue Buchung, ohne erneuten Blick in die
 * Schulorganisation — so lässt sich auch eine Buchung auf einen inzwischen stillgelegten
 * Lehrauftrag noch umbuchen. Ein Wechsel der Lehrkraft ist ausdrücklich <b>kein</b> Umbuchen.
 */
public interface Umbuchen {

  /**
   * Die freien Slots derselben Lehrkraft wie die übergebene Buchung, chronologisch — das Angebot
   * des Umbuchen-Dialogs. Ein Read-Modell wie {@code Buchungsoptionen}: Es darf veraltet sein, die
   * Entscheidung fällt erneut am Aggregat.
   *
   * @throws BuchungNichtGefundenException wenn es die Buchung nicht gibt
   */
  List<SlotOption> freieSlots(UUID buchungId);

  /**
   * Storniert die alte Buchung und legt am Wunschslot eine neue mit demselben Buchungsziel, derselben
   * Familie und derselben Notiz an. Der alte Slot wird erst frei, wenn der neue sicher ist: Scheitert
   * die Neubuchung, bleibt die alte Buchung unverändert bestehen. Veröffentlicht am Ende genau ein
   * {@link de.openclassware.elternsprechtag.sprechtag.domain.BuchungenBestaetigt} mit dem Anlass
   * {@link de.openclassware.elternsprechtag.sprechtag.domain.Anlass#UMBUCHUNG} — die Familie bekommt
   * eine Mail, die die Änderung benennt, nicht die bisherige Eingangsbestätigung.
   *
   * @return die Id der neuen Buchung
   * @throws BuchungNichtGefundenException wenn es die alte Buchung nicht gibt
   * @throws BuchungBereitsStorniertException wenn die alte Buchung nicht mehr die aktive Buchung
   *     ihres Termins ist — etwa ein zweiter Klick aus einem alten Browser-Tab
   * @throws SprechtagNichtVeroeffentlichtException wenn der Sprechtag der alten Buchung nicht
   *     veröffentlicht ist
   * @throws TerminBelegtException wenn der Wunschslot inzwischen vergeben ist
   */
  UUID umbuche(UmbuchAnfrage anfrage);

  /** Ein freier Slot derselben Lehrkraft im Umbuchen-Dialog. */
  record SlotOption(UUID terminId, LocalTime zeit) {}

  /** Welche Buchung auf welchen Slot derselben Lehrkraft verschoben werden soll. */
  record UmbuchAnfrage(UUID buchungId, UUID neuerTerminId) {}
}
