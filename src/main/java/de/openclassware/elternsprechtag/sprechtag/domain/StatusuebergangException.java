package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Signalisiert einen Statuswechsel, den {@link SprechtagStatus#erlaubteUebergaenge()} nicht
 * hergibt — insbesondere jeden Weg aus einem Endzustand heraus. Ein abgesagter Sprechtag lässt sich
 * nicht über „Speichern" wiederbeleben (`ABDECKUNG.md` Z. 93); die Eltern haben die Absage bereits
 * bekommen.
 */
public class StatusuebergangException extends RuntimeException {

  public StatusuebergangException(String message) {
    super(message);
  }
}
