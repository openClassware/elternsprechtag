package de.openclassware.elternsprechtag.sprechtag.application.port.in;

/**
 * Use Case: Sprechtage abschließen — sie haben stattgefunden, die Auswertung bleibt lesbar. Einen
 * Handabschluss gibt es nicht (#166).
 */
public interface Abschliessen {

  /**
   * Ein Lauf des täglichen Abschluss-Schedulers (Issue #124): schließt jeden veröffentlichten
   * Sprechtag ab, dessen Endzeit verstrichen ist. „Der Nachmittag ist vorbei" stellt die Maschine
   * fest, nicht der Organizer.
   *
   * @return wie viele Sprechtage in diesem Lauf abgeschlossen wurden
   */
  int schliesseVorbeiAb();
}
