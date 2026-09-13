package de.openclassware.elternsprechtag.schulorganisation.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Typisierte Identität. Aggregate referenzieren einander ausschließlich über solche Ids, nie über
 * Objektnavigation (ADR 0003). Die Domäne vergibt sie selbst — nicht die Datenbank (ADR 0004).
 *
 * <p>Namensgleich zur {@code LehrauftragId} des Sprechtag-Kontexts und trotzdem ein eigener Typ: Der
 * andere Kontext hat seine eigene Sicht auf denselben Begriff, und ein geteilter Typ wäre eine
 * geteilte Abhängigkeit.
 */
public record LehrauftragId(UUID wert) {

  public LehrauftragId {
    Objects.requireNonNull(wert, "wert");
  }

  public static LehrauftragId neu() {
    return new LehrauftragId(UUID.randomUUID());
  }

  public static LehrauftragId von(UUID wert) {
    return new LehrauftragId(wert);
  }
}
