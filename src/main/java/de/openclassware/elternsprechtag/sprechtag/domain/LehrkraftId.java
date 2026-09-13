package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typisierte Identität. Aggregate referenzieren einander ausschließlich über solche Ids, nie über
 * Objektnavigation (ADR 0003). Die Domäne vergibt sie selbst — nicht die Datenbank (ADR 0004).
 */
public record LehrkraftId(UUID wert) {

  public LehrkraftId {
    Objects.requireNonNull(wert, "wert");
  }

  public static LehrkraftId neu() {
    return new LehrkraftId(UUID.randomUUID());
  }

  public static LehrkraftId von(UUID wert) {
    return new LehrkraftId(wert);
  }
}
