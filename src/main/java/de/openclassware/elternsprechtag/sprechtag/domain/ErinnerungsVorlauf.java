package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalDate;

/**
 * Der Vorlauf, mit dem Eltern vor ihrem gebuchten Termin erinnert werden — feste Optionen statt
 * einer freien Datumsangabe, damit ein Erinnerungszeitpunkt nach dem Sprechtag gar nicht erst
 * entstehen kann (Issue #106). Die Uhrzeit des Versands steckt in der Option, nicht in einem
 * zweiten Feld.
 *
 * <p>Ist nicht Teil der Zeitstruktur ({@link Sprechtag#legeZeitstrukturFest}): Der Vorlauf
 * verschiebt keinen Termin und macht keine Buchung ungültig, deshalb bleibt er auch nach dem
 * Veröffentlichen änderbar (ADR 0006). Eine Änderung wirkt erst ab dem nächsten Lauf des
 * Erinnerungs-Schedulers (#107).
 */
public enum ErinnerungsVorlauf {
  KEINE(0),
  EIN_TAG(1),
  ZWEI_TAGE(2),
  DREI_TAGE(3);

  private final int tageVorher;

  ErinnerungsVorlauf(int tageVorher) {
    this.tageVorher = tageVorher;
  }

  /** Wie viele Tage vor dem Termin die Erinnerung läuft; {@code 0} heißt: keine Erinnerung. */
  public int tageVorher() {
    return tageVorher;
  }

  /**
   * Ob der Erinnerungs-Scheduler an genau diesem Tag laufen soll — „verfallen statt nachholen"
   * (Issue #107): Ein verpasster Lauf holt den Versand am Folgetag nicht nach, weil eine Erinnerung
   * am Sprechtagmorgen niemandem mehr hilft. {@code KEINE} ist nie fällig.
   */
  public boolean istFaelligAm(LocalDate sprechtagDatum, LocalDate heute) {
    return tageVorher > 0 && sprechtagDatum.minusDays(tageVorher).isEqual(heute);
  }
}
