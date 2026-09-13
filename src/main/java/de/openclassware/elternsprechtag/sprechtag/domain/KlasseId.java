package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typisierte Identität einer Klasse. Die Klasse selbst gehört der Schulorganisation — dieser Kontext
 * kennt von ihr nur die Id und liest ihren Namen über einen Port (ADR 0003).
 */
public record KlasseId(UUID wert) {

  public KlasseId {
    Objects.requireNonNull(wert, "wert");
  }

  public static KlasseId von(UUID wert) {
    return new KlasseId(wert);
  }
}
