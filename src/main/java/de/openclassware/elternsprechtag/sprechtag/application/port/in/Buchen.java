package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import java.util.List;
import java.util.UUID;

/**
 * Use Case: einen Buchungsvorgang festschreiben. Der einzige Eingang zum Buchen — Presenter rufen
 * ihn, nie ein Repository.
 *
 * <p>Die Anfrage kommt mit rohen {@link UUID}s herein, weil sie aus der Oberfläche stammt; die
 * typisierten Ids der Domäne entstehen dahinter.
 */
public interface Buchen {

  /**
   * Schreibt den Vorgang als N Buchungen fest — <b>alles oder nichts</b>. Ist auch nur ein Slot
   * nicht mehr zu haben, rollt die gesamte Transaktion zurück, es wird kein Ereignis veröffentlicht
   * und {@link TerminBelegtException} geworfen. Gibt die Anzahl gebuchter Termine zurück.
   */
  int buchen(BuchungsAnfrage anfrage);

  /**
   * Ein einzelner gewünschter Termin: welcher Lehrauftrag (Klasse+Fach+Lehrkraft) zu welchem Slot —
   * samt der Notiz, die genau dieser Lehrkraft gilt. {@code notiz} darf {@code null} sein.
   */
  record BuchungsWunsch(UUID lehrauftragId, UUID terminId, String notiz) {}

  /** Ein kompletter Eltern-Submit: Angaben zur Familie plus alle gewählten Fach-Slots. */
  record BuchungsAnfrage(
      String elternName, String schuelerName, String elternEmail, List<BuchungsWunsch> wuensche) {}
}
