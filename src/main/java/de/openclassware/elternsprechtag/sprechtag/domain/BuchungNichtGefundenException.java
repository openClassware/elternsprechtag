package de.openclassware.elternsprechtag.sprechtag.domain;

/** Die genannte Buchung gehört nicht zu diesem Termin. */
public class BuchungNichtGefundenException extends RuntimeException {

  public BuchungNichtGefundenException(String message) {
    super(message);
  }
}
