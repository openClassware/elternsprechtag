package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalDate;

/**
 * Wie viele Tage vor dem Sprechtag die Anmeldung über den Elternlink schließt (Issue #122) — der
 * gespeicherte Abstand, aus dem sich der <b>Anmeldeschluss</b> erst errechnet.
 *
 * <p>Relativ statt als festes Datum, damit eine duplizierte Kopie nie „tot geboren" ist: Sie
 * übernimmt den Abstand, und der passt zu jedem Datum, auf das der Organizer sie danach setzt.
 *
 * <p>0 heißt „am Tag selbst"; mehr als 28 Tage gibt es nicht — die Grenze fängt den Tippfehler ab
 * (40 statt 4), der die Anmeldung sonst unbemerkt wochenlang vorher schlösse. Dass der
 * Anmeldeschluss nie nach dem Sprechtag liegt, folgt schon aus der Untergrenze.
 *
 * <p>Ist nicht Teil der Zeitstruktur ({@link Sprechtag#legeZeitstrukturFest}): Die Frist erzeugt
 * keinen Termin und macht keine Buchung ungültig, deshalb bleibt sie wie der {@link
 * ErinnerungsVorlauf} auch nach dem Veröffentlichen änderbar.
 */
public record Anmeldefrist(int tageVorher) {

  public static final int HOECHSTENS_TAGE = 28;

  /** Der Vortag — was jeder neue Sprechtag vorgeschlagen bekommt. */
  public static final Anmeldefrist STANDARD = new Anmeldefrist(1);

  public Anmeldefrist {
    if (!istZulaessig(tageVorher)) {
      throw new IllegalArgumentException(
          "Die Anmeldefrist liegt zwischen 0 und " + HOECHSTENS_TAGE + " Tagen: " + tageVorher);
    }
  }

  /** Ob sich aus diesem Wert eine Anmeldefrist bilden lässt — für die Formularvalidierung. */
  public static boolean istZulaessig(int tageVorher) {
    return tageVorher >= 0 && tageVorher <= HOECHSTENS_TAGE;
  }

  public static Anmeldefrist vonTagen(int tageVorher) {
    return new Anmeldefrist(tageVorher);
  }

  /** Der letzte Tag, an dem Eltern buchen können — einschließlich. */
  public LocalDate anmeldeschlussFuer(LocalDate sprechtagDatum) {
    return sprechtagDatum.minusDays(tageVorher);
  }
}
