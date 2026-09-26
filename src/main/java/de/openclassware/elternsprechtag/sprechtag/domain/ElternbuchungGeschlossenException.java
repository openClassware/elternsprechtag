package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Der Elternlink nimmt keine Buchung an — der Anmeldeschluss ist vorbei, oder der Sprechtag ist
 * nicht (mehr) veröffentlicht (Issue #122). Eine Exception für beide Fälle, weil die Eltern-Ansicht
 * in beiden dasselbe tut: abweisen und den Zugang neu prüfen.
 *
 * <p>Trifft nur den Eltern-Submit. Der Organizer-Nachtrag bleibt bis zum Abschluss offen.
 */
public class ElternbuchungGeschlossenException extends RuntimeException {

  public ElternbuchungGeschlossenException(String message) {
    super(message);
  }
}
