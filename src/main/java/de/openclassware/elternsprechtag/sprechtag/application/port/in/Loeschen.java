package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtLoeschbarException;
import java.util.UUID;

/**
 * Use Case: einen versehentlich angelegten Entwurf löschen (`ABDECKUNG.md`, Phase 6, #132) — eine
 * Aufräumfunktion für Fehlgriffe, wie sie das Duplizieren mit einem Klick erzeugt.
 */
public interface Loeschen {

  /**
   * Ein Sprechtag, den es nicht (mehr) gibt, ist kein Fehler: Wer löschen wollte, hat sein Ziel.
   *
   * @throws SprechtagNichtLoeschbarException wenn der Sprechtag kein Entwurf ist oder sich seit dem
   *     Laden verändert hat
   */
  void loesche(UUID id);
}
