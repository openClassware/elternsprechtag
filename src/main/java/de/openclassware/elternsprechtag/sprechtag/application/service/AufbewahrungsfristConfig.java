package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.domain.Aufbewahrungsfrist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Die eine {@link Aufbewahrungsfrist} des Betriebs ({@code elternsprechtag.aufbewahrungsfrist-tage}).
 *
 * <p>Zwei Use Cases rechnen mit ihr: der Anonymisierungs-Lauf (#126) und die Vorwarnung der
 * Sprechtag-Liste (#128). Läsen beide die Property selbst, könnten sie auseinanderlaufen, und die
 * Liste nennte ein anderes Datum als das, an dem der Lauf die Daten tatsächlich anonymisiert.
 */
@Configuration
class AufbewahrungsfristConfig {

  @Bean
  Aufbewahrungsfrist aufbewahrungsfrist(
      @Value("${elternsprechtag.aufbewahrungsfrist-tage}") int tage) {
    return Aufbewahrungsfrist.vonTagen(tage);
  }
}
