package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Signalisiert den Versuch, die Zeitstruktur eines veröffentlichten Sprechtags zu ändern — Datum,
 * Zeitfenster, Slot-Dauer oder die Liste der teilnehmenden Klassen.
 *
 * <p>Aus genau diesen vier Angaben entstehen beim Veröffentlichen die Termine. Wer sie danach
 * ändert, ändert nur noch die Anzeige: Die Termine bleiben, wo sie sind, die Eltern haben
 * Uhrzeiten in der Hand, die nicht mehr gelten, und eine nachträglich hinzugefügte Klasse bekäme nie
 * Termine (`ABDECKUNG.md` Z. 87–89). Das Einfrieren ist zugleich die Umsetzung der
 * Produktentscheidung, einen Sprechtag nicht zu verschieben (`ABDECKUNG.md` Z. 324 ff.): Wer die
 * Zeiten wirklich ändern muss, sagt ab und legt neu an.
 */
public class ZeitstrukturEingefrorenException extends RuntimeException {

  public ZeitstrukturEingefrorenException(String message) {
    super(message);
  }
}
