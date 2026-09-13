package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Wie lang ein einzelnes Gespräch dauert, in Minuten. Muss positiv sein — eine Dauer von null
 * Minuten ließe die Slot-Berechnung endlos laufen, eine negative erst recht.
 *
 * <p>Dass die Oberfläche das Feld nicht leer lassen darf (`ABDECKUNG.md` Z. 95), ist dieselbe Regel
 * an anderer Stelle: Hier scheitert {@code null} spätestens beim Anlegen des Wertes.
 */
public record Slotdauer(int minuten) {

  public Slotdauer {
    if (minuten <= 0) {
      throw new IllegalArgumentException("Die Slot-Dauer muss positiv sein: " + minuten);
    }
  }

  public static Slotdauer vonMinuten(int minuten) {
    return new Slotdauer(minuten);
  }
}
