package de.openclassware.elternsprechtag.sprechtag.adapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Zentrale, Deutsch-lokalisierte Datums-/Zeit-Formatierung für die UI. Einzige Stelle für
 * Anzeige-Formatter — nicht inline duplizieren (siehe ARCHITECTURE.md, Abschnitt i18n).
 */
public final class Formats {

  private Formats() {}

  private static final Locale LOCALE = Locale.GERMANY;
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_LONG =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(LOCALE);
  private static final DateTimeFormatter DATE_TIME_SHORT =
      DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", LOCALE);
  private static final DateTimeFormatter WEEKDAY_DATE_SHORT =
      DateTimeFormatter.ofPattern("EEE, dd.MM.", LOCALE);

  /** Uhrzeit als {@code HH:mm}. */
  public static String time(LocalTime time) {
    return time.format(TIME);
  }

  /** Ausführliches Datum, z. B. „20. Juli 2026". */
  public static String dateLong(LocalDate date) {
    return date.format(DATE_LONG);
  }

  /** Datum und Uhrzeit, z. B. „02.10.2026, 14:37" — der Stand auf einem gedruckten Blatt. */
  public static String dateTimeShort(LocalDateTime dateTime) {
    return dateTime.format(DATE_TIME_SHORT);
  }

  /** Kurzes Datum mit Wochentag, z. B. „Mo., 20.07.". */
  public static String weekdayDateShort(LocalDate date) {
    return date.format(WEEKDAY_DATE_SHORT);
  }

  /** Kurzer Monatsname, z. B. „Jul.". */
  public static String monthShort(LocalDate date) {
    return date.getMonth().getDisplayName(TextStyle.SHORT, LOCALE);
  }
}
