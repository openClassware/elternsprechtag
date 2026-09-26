package de.openclassware.elternsprechtag.sprechtag.adapter;

import java.time.LocalDate;
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

  /** Kurzes Datum mit Wochentag, z. B. „Mo., 20.07.". */
  public static String weekdayDateShort(LocalDate date) {
    return date.format(WEEKDAY_DATE_SHORT);
  }

  /** Kurzer Monatsname, z. B. „Jul.". */
  public static String monthShort(LocalDate date) {
    return date.getMonth().getDisplayName(TextStyle.SHORT, LOCALE);
  }
}
