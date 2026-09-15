package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Der Vorgang setzt einen veröffentlichten Sprechtag voraus, und dieser ist es nicht.
 *
 * <p>Für das Storno heißt das: Nach dem Abschluss eines Sprechtags wäre „wieder frei" eine Lüge —
 * es bucht niemand mehr. Ein Löschverlangen danach ist die Anonymisierung einer Buchung, nicht ihr
 * Storno (`ABDECKUNG.md`, Phase 6).
 */
public class SprechtagNichtVeroeffentlichtException extends RuntimeException {

  public SprechtagNichtVeroeffentlichtException(String message) {
    super(message);
  }
}
