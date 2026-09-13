package de.openclassware.elternsprechtag;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Bringt den vollständigen Sprechtag-Kontext in einen Slice-Test: Use-Case-Services und alle
 * Outbound-Adapter.
 *
 * <p>Bewusst ein {@code @ComponentScan} statt einer Liste einzelner Klassen — die Services und
 * Adapter sind package-private (nach außen gilt der Port), und eine Klassenliste ließe sich von
 * hier aus gar nicht schreiben. Der Scan hält die Sichtbarkeitsgrenze und die Testbarkeit zugleich.
 */
@Configuration
@ComponentScan("de.openclassware.elternsprechtag.sprechtag")
public class SprechtagKontextTestConfig {}
