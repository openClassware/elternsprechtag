package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typisierte Identität. Aggregate referenzieren einander ausschließlich über solche Ids, nie über
 * Objektnavigation (ADR 0003). Die Domäne vergibt sie selbst — nicht die Datenbank (ADR 0004).
 */
public record TerminId(UUID wert) {

  public TerminId {
    Objects.requireNonNull(wert, "wert");
  }

  public static TerminId neu() {
    return new TerminId(UUID.randomUUID());
  }

  public static TerminId von(UUID wert) {
    return new TerminId(wert);
  }
}
