package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Wie lange die personenbezogenen Angaben einer Buchung nach dem Sprechtag aufbewahrt werden
 * (Issue #126) — gezählt in Tagen ab der {@link Sprechtag#endzeit() Endzeit}, nicht ab einem
 * Statuswechsel. So ist der Ablauf für jeden Sprechtag definiert, auch für einen abgesagten, und
 * hängt nicht daran, ob ein Lauf rechtzeitig kam.
 *
 * <p>Eine Frist von null Tagen gibt es nicht: Sie nähme dem Organizer die Auswertung, bevor er sie
 * gelesen hat.
 */
public record Aufbewahrungsfrist(int tage) {

  public Aufbewahrungsfrist {
    if (tage < 1) {
      throw new IllegalArgumentException(
          "Die Aufbewahrungsfrist beträgt mindestens einen Tag: " + tage);
    }
  }

  public static Aufbewahrungsfrist vonTagen(int tage) {
    return new Aufbewahrungsfrist(tage);
  }

  /** Ob die Frist für einen Sprechtag mit dieser Endzeit zum Zeitpunkt {@code jetzt} verstrichen ist. */
  public boolean istAbgelaufen(LocalDateTime endzeit, LocalDateTime jetzt) {
    return endzeit.plusDays(tage).isBefore(jetzt);
  }

  /**
   * Der letzte Tag, an dem die Angaben eines Sprechtags mit dieser Endzeit noch vorhanden sind — die
   * Vorwarnung der Sprechtag-Liste (Issue #128).
   *
   * <p>Zu Beginn dieses Tages ist die Frist noch nicht abgelaufen, zu Beginn des Folgetags schon.
   * Den ganzen Tag über stimmt das Datum, weil der Anonymisierungs-Lauf <b>nachts</b> läuft, vor der
   * Uhrzeit, zu der ein Sprechtag endet: {@link #istAbgelaufen} wird an diesem Tag erst zur
   * Endzeit-Uhrzeit wahr. Ein Lauf am Abend fände die Daten schon am genannten Tag fällig.
   */
  public LocalDate verfuegbarBis(LocalDateTime endzeit) {
    return endzeit.plusDays(tage).toLocalDate();
  }

  /**
   * Das späteste Sprechtagsdatum, dessen Frist bis {@code heute} abgelaufen sein kann — der grobe
   * Vorfilter für die Kandidatensuche. Entschieden wird mit {@link #istAbgelaufen}.
   */
  public LocalDate spaetestesDatumAbgelaufenBis(LocalDate heute) {
    return heute.minusDays(tage);
  }
}
