package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Signalisiert den Versuch, einen Sprechtag zu löschen, der kein Entwurf (mehr) ist — oder der sich
 * seit dem Laden verändert hat.
 *
 * <p>Löschen räumt Fehlgriffe auf, es lässt keine Belege verschwinden (`ABDECKUNG.md`, Phase 6,
 * #132). Ein veröffentlichter Sprechtag wird abgesagt; ein abgesagter oder abgeschlossener
 * anonymisiert sich und bleibt als Auslastung stehen.
 */
public class SprechtagNichtLoeschbarException extends RuntimeException {

  public SprechtagNichtLoeschbarException(String message) {
    super(message);
  }
}
