package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Der Slot trägt eine aktive Buchung und lässt deshalb (noch) nicht entfallen.
 *
 * <p>Keine fachliche Sackgasse, sondern eine Grenze des heutigen Stands: Das Storno samt
 * Benachrichtigung der Familie gehört zur Ausfall-Strecke. Bis die gebaut ist, hält das Aggregat
 * einen Zustand fern, dessen Folgen niemand ausführt.
 */
public class TerminHatBuchungException extends RuntimeException {

  public TerminHatBuchungException(String message) {
    super(message);
  }
}
