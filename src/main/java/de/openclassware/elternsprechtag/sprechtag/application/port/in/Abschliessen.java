package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.UUID;

/** Use Case: einen Sprechtag abschließen — er hat stattgefunden, die Auswertung bleibt lesbar. */
public interface Abschliessen {

  void schliesseAb(UUID id);

  /**
   * Ein Lauf des täglichen Abschluss-Schedulers (Issue #124): schließt jeden veröffentlichten
   * Sprechtag ab, dessen Endzeit verstrichen ist. „Der Nachmittag ist vorbei" stellt die Maschine
   * fest, nicht der Organizer; der Menüpunkt bleibt zum Vorziehen.
   *
   * @return wie viele Sprechtage in diesem Lauf abgeschlossen wurden
   */
  int schliesseVorbeiAb();
}
