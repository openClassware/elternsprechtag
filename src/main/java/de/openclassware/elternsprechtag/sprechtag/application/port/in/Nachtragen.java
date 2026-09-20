package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import de.openclassware.elternsprechtag.sprechtag.domain.ZeitkonfliktException;
import java.util.List;
import java.util.UUID;

/**
 * Use Case: Der Organizer bucht im Namen einer Familie nach, die noch keine Buchung hat — fachlich
 * derselbe Vorgang wie der Eltern-Submit ({@link Buchen}), nur von jemand anderem angestoßen.
 *
 * <p>Ein eigener Port statt eines Schalters am bestehenden {@link Buchen}: Beide Wege tragen ihre
 * eigenen Vorbedingungen. Konkret hängt am Nachtragen später der Anmeldeschluss — er schließt den
 * Elternlink, lässt die Organizer-Strecke aber bis zum Abschluss des Sprechtags offen. Mit einem
 * gemeinsamen Port bräuchte diese Frist eine Ausnahme; mit getrennten Ports landet sie an genau
 * einer Stelle.
 */
public interface Nachtragen {

  /**
   * Schreibt den Nachtrag als N Buchungen fest — <b>alles oder nichts</b>, wie {@link
   * Buchen#buchen}: Ist auch nur ein Slot nicht mehr zu haben, rollt die gesamte Transaktion
   * zurück, es wird kein Ereignis veröffentlicht und {@link TerminBelegtException} geworfen.
   *
   * <p>Zusätzlich abgewiesen wird ein Nachtrag an einem nicht veröffentlichten Sprechtag — geprüft
   * hier und nicht bloß in der Oberfläche, weil die Route per URL für jeden Sprechtag-Status
   * erreichbar ist. Gibt die Anzahl gebuchter Termine zurück.
   *
   * @throws TerminBelegtException wenn ein gewählter Slot nicht mehr zu haben ist.
   * @throws ZeitkonfliktException wenn zwei Wünsche dieses Vorgangs auf dieselbe Uhrzeit fallen.
   * @throws SprechtagNichtVeroeffentlichtException wenn der Sprechtag der Wünsche nicht
   *     veröffentlicht ist.
   */
  int trageNach(NachtragsAnfrage anfrage);

  /**
   * Ein einzelner nachgetragener Termin: welcher Lehrauftrag (Klasse+Fach+Lehrkraft) zu welchem
   * Slot — samt der Notiz, die genau dieser Lehrkraft gilt. {@code notiz} darf {@code null} sein.
   */
  record NachtragsWunsch(UUID lehrauftragId, UUID terminId, String notiz) {}

  /** Ein kompletter Organizer-Nachtrag: Angaben zur Familie plus alle gewählten Fach-Slots. */
  record NachtragsAnfrage(
      String elternName,
      String schuelerName,
      String elternEmail,
      List<NachtragsWunsch> wuensche) {}
}
