package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Die genannte Buchung ist nicht (mehr) die aktive Buchung ihres Termins — storniert wird nur eine
 * Zusage, die noch gilt.
 *
 * <p>Der Fall ist nicht bloß Formalismus: Nach einem Storno kann eine andere Familie den frei
 * gewordenen Slot buchen. Ein zweiter Klick auf dieselbe Zeile in einem alten, nicht neu geladenen
 * Browser-Tab soll dann scheitern und nicht still nichts tun.
 */
public class BuchungBereitsStorniertException extends RuntimeException {

  public BuchungBereitsStorniertException(String message) {
    super(message);
  }
}
