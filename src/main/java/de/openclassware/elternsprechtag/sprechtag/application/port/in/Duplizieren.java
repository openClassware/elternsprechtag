package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.UUID;

/**
 * Use Case: einen Sprechtag als neuen Entwurf kopieren — der übliche Weg zum Sprechtag des nächsten
 * Halbjahres.
 */
public interface Duplizieren {

  /**
   * @return die Id der Kopie
   */
  UUID dupliziere(UUID id);
}
