package de.openclassware.elternsprechtag.schulorganisation.domain;

/** Ein Stammdatum, auf das sich ein Aufruf beruft, gibt es nicht (mehr). */
public class StammdatumNichtGefundenException extends RuntimeException {

  public StammdatumNichtGefundenException(String meldung) {
    super(meldung);
  }
}
