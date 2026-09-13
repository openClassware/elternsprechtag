package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Der Schlüssel zum anonymen Elternlink. Es gibt bewusst keine Eltern-Accounts; dieser Wert ist die
 * einzige Zugangsbedingung und darf deshalb nicht leer sein.
 */
public record AccessToken(String wert) {

  public AccessToken {
    Objects.requireNonNull(wert, "wert");
    if (wert.isBlank()) {
      throw new IllegalArgumentException("Ein leeres Zugangs-Token gibt keinen Zugang");
    }
  }

  public static AccessToken neu() {
    return new AccessToken(UUID.randomUUID().toString());
  }

  public static AccessToken von(String wert) {
    return new AccessToken(wert);
  }
}
