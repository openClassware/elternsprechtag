package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Wie viele Tage vor dem Sprechtag die Anmeldung über den Elternlink schließt (Issue #122) — der
 * gespeicherte Abstand, aus dem sich der <b>Anmeldeschluss</b> erst errechnet.
 *
 * <p>Relativ statt als festes Datum, damit eine duplizierte Kopie nie „tot geboren" ist: Sie
 * übernimmt den Abstand, und der passt zu jedem Datum, auf das der Organizer sie danach setzt.
 *
 * <p>0 heißt „bis zum Beginn des Sprechtags" (Issue #118) — nicht bis Mitternacht, sonst stünden am
 * Tag selbst Slots zur Wahl, die schon begonnen haben. Ab 1 zählt der letzte Buchungstag ganz.
 * Mehr als 28 Tage gibt es nicht — die Grenze fängt den Tippfehler ab (40 statt 4), der die
 * Anmeldung sonst unbemerkt wochenlang vorher schlösse. Dass der Anmeldeschluss nie nach dem
 * Beginn liegt, folgt schon aus der Untergrenze.
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

  /**
   * Der Zeitpunkt, ab dem der Elternlink keine Buchung mehr annimmt — <b>ausschließlich</b>: Wer
   * davor abschickt, bucht noch. Als eine Regel: mit dem Beginn des Sprechtags, spätestens aber am
   * Ende des Tages {@code tageVorher} Tage vorher. Bei 0 greift der Beginn, sonst die Mitternacht
   * nach dem {@link #letzterTagFuer letzten Buchungstag}.
   */
  public LocalDateTime anmeldeschlussFuer(LocalDate sprechtagDatum, LocalTime sprechtagBeginn) {
    if (schliesstMitBeginn()) {
      return sprechtagDatum.atTime(sprechtagBeginn);
    }
    return letzterTagFuer(sprechtagDatum).plusDays(1).atStartOfDay();
  }

  /**
   * Ob die Anmeldung mit dem Beginn des Sprechtags schließt statt zum Tageswechsel — dann nennt die
   * Anzeige eine Uhrzeit, sonst nur den letzten Tag.
   */
  public boolean schliesstMitBeginn() {
    return tageVorher == 0;
  }

  /** Der letzte Tag, an dem Eltern buchen können — ganz, bei 0 nur bis zum Beginn. */
  public LocalDate letzterTagFuer(LocalDate sprechtagDatum) {
    return sprechtagDatum.minusDays(tageVorher);
  }
}
