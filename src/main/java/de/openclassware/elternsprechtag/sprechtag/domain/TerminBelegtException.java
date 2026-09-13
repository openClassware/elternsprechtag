package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Ein gewünschter Slot ist nicht (mehr) zu haben — er trägt schon eine aktive Buchung, oder zwei
 * Eltern haben ihn gleichzeitig gebucht und das optimistische Sperren am Root hat entschieden.
 *
 * <p>Beides ist fachlich derselbe Fall und sieht für die Eltern gleich aus. Read-Modelle dürfen
 * veralten; dass der Termin beim Buchen selbst prüft und sonst diese Ausnahme wirft, ist der
 * tragende Mechanismus — nicht die Aktualität der Anzeige (ADR 0003).
 */
public class TerminBelegtException extends RuntimeException {

  public TerminBelegtException(String message) {
    super(message);
  }
}
