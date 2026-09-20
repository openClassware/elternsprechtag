package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Ein Buchungsvorgang wünscht sich zwei Termine zur selben Uhrzeit — man kann nicht an zwei
 * Tischen gleichzeitig sitzen.
 *
 * <p>Die Regel ist eine Aussage über mehrere {@link Termin}e und kann deshalb nicht am einzelnen
 * Aggregat geprüft werden; sie gehört in den Buchungsvorgang selbst (ADR 0005). Gilt nur innerhalb
 * eines Vorgangs — zwei Familien dürfen dieselbe Uhrzeit bei verschiedenen Lehrkräften in
 * getrennten Vorgängen buchen.
 */
public class ZeitkonfliktException extends RuntimeException {

  public ZeitkonfliktException(String message) {
    super(message);
  }
}
