package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.UUID;

/**
 * Use Case: einen Sprechtag veröffentlichen — er wird für die Eltern sichtbar, seine Zeitstruktur
 * friert ein, und die Termine entstehen.
 */
public interface Veroeffentlichen {

  Ergebnis veroeffentliche(UUID id);

  /**
   * Wie viele Termine dabei entstanden sind. {@code 0} ist kein Fehler, aber eine Meldung wert: Dann
   * hat keine der gewählten Klassen einen Lehrauftrag, der Sprechtag ist veröffentlicht und
   * trotzdem leer (`ABDECKUNG.md` Z. 94).
   */
  record Ergebnis(int erzeugteTermine) {

    public boolean ohneTermine() {
      return erzeugteTermine == 0;
    }
  }
}
