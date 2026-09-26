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
 *
 * <p>Die E-Mail ist für alle Buchungen dieselbe und Betriebseinstellung — eine Adresse, die keine
 * Post annimmt. Die Domäne kennt sie nicht, sie bekommt sie mit jedem Lauf übergeben.
 */
public final class Pseudonymisierung {

  private final String seed;
  private final String email;
  private int nummer;

  private Pseudonymisierung(String seed, String email) {
    this.seed = pflicht(seed, "Ein Anonymisierungslauf braucht einen Seed");
    this.email = pflicht(email, "Ein Anonymisierungslauf braucht eine Ersatz-E-Mail");
  }

  /** Ein neuer Lauf mit zufälligem Seed. */
  public static Pseudonymisierung neu(String email) {
    return new Pseudonymisierung(UUID.randomUUID().toString().substring(0, 8), email);
  }

  /** Ein Lauf mit festem Seed — für Tests. */
  public static Pseudonymisierung mitSeed(String seed, String email) {
    return new Pseudonymisierung(seed, email);
  }

  /** Die Ersatz-Familie für die nächste Buchung dieses Laufs. */
  public Familie naechsteFamilie() {
    nummer++;
    String kennung = seed + "-" + String.format("%03d", nummer);
    return new Familie("Eltern-" + kennung, "Schueler-" + kennung, email);
  }

  private static String pflicht(String wert, String meldung) {
    Objects.requireNonNull(wert, meldung);
    if (wert.isBlank()) {
      throw new IllegalArgumentException(meldung);
    }
    return wert;
  }

  public String seed() {
    return seed;
  }
}
