package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.UUID;

/** Use Case: einen Sprechtag abschließen — er hat stattgefunden, die Auswertung bleibt lesbar. */
public interface Abschliessen {

  void schliesseAb(UUID id);
}
