package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Die Ersatzwerte eines Anonymisierungslaufs (Issue #126): <em>Feldkennung + Seed des Laufs +
 * laufende Nummer</em>, etwa {@code Eltern-3f9a1c2e-001}. Die {@link Familie} behält so ihre
 * Pflichtfelder — ein Aggregat mit leeren Pflichtangaben wäre ein zweites Modell —, trägt aber
 * niemanden mehr.
 *
 * <p>Der Seed entsteht je Lauf einmal, die Nummer zählt innerhalb des Laufs hoch. Die Werte sind
 * damit innerhalb eines Laufs unterscheidbar, lassen aber keinen Rückschluss auf die Familie zu. Ein
 * Objekt dieser Klasse gehört genau einem Lauf; es ist zustandsbehaftet und nicht threadsicher.
 */
public final class Pseudonymisierung {

  /** Die E-Mail, auf die jede anonymisierte Buchung zeigt — sie nimmt keine Post an. */
  public static final String EMAIL = "noreply@openclassware.de";

  private final String seed;
  private int nummer;

  private Pseudonymisierung(String seed) {
    Objects.requireNonNull(seed, "seed");
    if (seed.isBlank()) {
      throw new IllegalArgumentException("Ein Anonymisierungslauf braucht einen Seed");
    }
    this.seed = seed;
  }

  /** Ein neuer Lauf mit zufälligem Seed. */
  public static Pseudonymisierung neu() {
    return new Pseudonymisierung(UUID.randomUUID().toString().substring(0, 8));
  }

  /** Ein Lauf mit festem Seed — für Tests. */
  public static Pseudonymisierung mitSeed(String seed) {
    return new Pseudonymisierung(seed);
  }

  /** Die Ersatz-Familie für die nächste Buchung dieses Laufs. */
  public Familie naechsteFamilie() {
    nummer++;
    String kennung = seed + "-" + String.format("%03d", nummer);
    return new Familie("Eltern-" + kennung, "Schueler-" + kennung, EMAIL);
  }

  public String seed() {
    return seed;
  }
}
