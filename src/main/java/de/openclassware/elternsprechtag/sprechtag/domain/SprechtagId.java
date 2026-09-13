package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typisierte Identität. Aggregate referenzieren einander ausschließlich über solche Ids, nie über
 * Objektnavigation (ADR 0003). Die Domäne vergibt sie selbst — nicht die Datenbank (ADR 0004).
 */
public record SprechtagId(UUID wert) {

  public SprechtagId {
    Objects.requireNonNull(wert, "wert");
  }

  public static SprechtagId neu() {
    return new SprechtagId(UUID.randomUUID());
  }

  public static SprechtagId von(UUID wert) {
    return new SprechtagId(wert);
  }
}
