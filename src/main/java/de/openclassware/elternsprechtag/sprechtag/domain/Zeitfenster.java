package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Von wann bis wann ein Sprechtag läuft — beides Pflicht, das Ende nach dem Beginn. Ein Sprechtag
 * ohne Dauer ist keiner, und ohne diese Zusicherung ließe sich die Slot-Berechnung nicht sinnvoll
 * beenden.
 *
 * <p>Anders als {@link Zeitraum} trägt das Zeitfenster kein Datum: Es beschreibt den Tagesabschnitt,
 * das Datum steht am Sprechtag.
 */
public record Zeitfenster(LocalTime beginn, LocalTime ende) {

  public Zeitfenster {
    Objects.requireNonNull(beginn, "beginn");
    Objects.requireNonNull(ende, "ende");
    if (!ende.isAfter(beginn)) {
      throw new IllegalArgumentException(
          "Das Ende muss nach dem Beginn liegen: " + beginn + " – " + ende);
    }
  }
}
