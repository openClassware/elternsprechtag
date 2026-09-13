package de.openclassware.elternsprechtag.schulorganisation.domain;

/**
 * Die eine Stelle für die Prüfung, die alle vier Stammdaten-Aggregate teilen: ein Pflichtfeld ist
 * weder {@code null} noch leer noch nur Leerraum, und es wird getrimmt gespeichert.
 *
 * <p>Getrimmt, weil Stammdaten aus einem Import kommen werden und dessen Quelle über Leerzeichen am
 * Rand nichts aussagt — ein Fach „ Deutsch" und ein Fach „Deutsch" sind dasselbe Fach, und die
 * Eindeutigkeit in der Datenbank soll das auch so sehen.
 */
final class Pflichttext {

  private Pflichttext() {}

  static String pflicht(String wert, String feld) {
    if (wert == null || wert.isBlank()) {
      throw new IllegalArgumentException(feld + " ist eine Pflichtangabe");
    }
    return wert.trim();
  }
}
