package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Die Lage eines Termins auf dem Zeitstrahl — Beginn und Ende, beides Pflicht. Das Ende muss nach
 * dem Beginn liegen; ein Termin ohne Dauer ist keiner.
 */
public record Zeitraum(LocalDateTime beginn, LocalDateTime ende) {

  public Zeitraum {
    Objects.requireNonNull(beginn, "beginn");
    Objects.requireNonNull(ende, "ende");
    if (!ende.isAfter(beginn)) {
      throw new IllegalArgumentException("Das Ende muss nach dem Beginn liegen: " + beginn + " – " + ende);
    }
  }

  /** Die Uhrzeit des Beginns — das, was die Oberfläche als Slot-Zeit zeigt. */
  public LocalTime uhrzeit() {
    return beginn.toLocalTime();
  }
}
