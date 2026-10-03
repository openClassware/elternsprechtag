package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import java.util.Locale;

/**
 * Die zweite Eingabe „E-Mail-Adresse wiederholen" der beiden Buchungsstrecken (Issue #111) — Stufe
 * eins der Adressprüfung: Sie fängt den Buchstabendreher, den das Formatmuster durchlässt.
 *
 * <p>Beide Eingaben gelten als gleich, wenn sie nach Entfernen der Randleerzeichen ohne Unterschied
 * zwischen Groß- und Kleinschreibung übereinstimmen — ein Leerzeichen am Ende oder ein von der
 * Autokorrektur großgeschriebener Anfang ist kein Tippfehler. Gespeichert wird weiterhin die erste
 * Eingabe; der Server erfährt von der Wiederholung nichts.
 *
 * <p>Bewusst <b>Vaadin-frei</b> und ohne Zustand, damit die Regel ohne UI unit-testbar ist und
 * beide Ansichten dieselbe teilen.
 */
final class EmailWiederholung {

  /** Wie die Wiederholung zur ersten Eingabe steht. */
  enum Abgleich {
    /** Die Wiederholung ist noch leer. */
    LEER,
    /** Die Wiederholung ist ausgefüllt, weicht aber ab. */
    ABWEICHEND,
    /** Beide Eingaben stimmen überein. */
    GLEICH
  }

  private EmailWiederholung() {}

  static Abgleich vergleiche(String email, String wiederholung) {
    String wiederholt = normiert(wiederholung);
    if (wiederholt.isEmpty()) {
      return Abgleich.LEER;
    }
    return wiederholt.equals(normiert(email)) ? Abgleich.GLEICH : Abgleich.ABWEICHEND;
  }

  private static String normiert(String eingabe) {
    return eingabe == null ? "" : eingabe.strip().toLowerCase(Locale.ROOT);
  }
}
