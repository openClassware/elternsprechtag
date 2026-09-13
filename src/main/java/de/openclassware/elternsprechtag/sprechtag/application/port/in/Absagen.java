package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.UUID;

/**
 * Use Case: einen veröffentlichten Sprechtag absagen — der Weg, der die betroffenen Eltern erreicht.
 * Der Versand hängt am Ereignis, das dabei entsteht, und läuft erst nach dem Commit.
 */
public interface Absagen {

  void sageAb(UUID id);
}
