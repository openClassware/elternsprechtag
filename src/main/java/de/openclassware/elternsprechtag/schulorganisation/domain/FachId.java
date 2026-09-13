package de.openclassware.elternsprechtag.schulorganisation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typisierte Identität. Aggregate referenzieren einander ausschließlich über solche Ids, nie über
 * Objektnavigation (ADR 0003). Die Domäne vergibt sie selbst — nicht die Datenbank (ADR 0004).
 *
 * <p>Namensgleich zur {@code FachId} des Sprechtag-Kontexts und trotzdem ein eigener Typ: Der
 * andere Kontext hat seine eigene Sicht auf denselben Begriff, und ein geteilter Typ wäre eine
 * geteilte Abhängigkeit.
 */
public record FachId(UUID wert) {

  public FachId {
    Objects.requireNonNull(wert, "wert");
  }

  public static FachId neu() {
    return new FachId(UUID.randomUUID());
  }

  public static FachId von(UUID wert) {
    return new FachId(wert);
  }
}
