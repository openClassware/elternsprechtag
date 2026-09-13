package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Das Buchungsziel nennt eine andere Lehrkraft als der Termin. Kein fachlicher Konflikt, sondern
 * ein widersprüchlicher Auftrag — deshalb ein {@link IllegalArgumentException}, das keine
 * Oberfläche freundlich behandeln muss.
 */
public class FremdeLehrkraftException extends IllegalArgumentException {

  public FremdeLehrkraftException(String message) {
    super(message);
  }
}
