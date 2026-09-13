package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typisierte Identität. Aggregate referenzieren einander ausschließlich über solche Ids, nie über
 * Objektnavigation (ADR 0003). Die Domäne vergibt sie selbst — nicht die Datenbank (ADR 0004).
 */
public record BuchungId(UUID wert) {

  public BuchungId {
    Objects.requireNonNull(wert, "wert");
  }

  public static BuchungId neu() {
    return new BuchungId(UUID.randomUUID());
  }

  public static BuchungId von(UUID wert) {
    return new BuchungId(wert);
  }
}
