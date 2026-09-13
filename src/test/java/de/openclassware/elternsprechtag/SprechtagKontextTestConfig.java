package de.openclassware.elternsprechtag;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Bringt den vollständigen Sprechtag-Kontext in einen Slice-Test: Use-Case-Services und alle
 * Outbound-Adapter.
 *
 * <p>Bewusst ein {@code @ComponentScan} statt einer Liste einzelner Klassen — die Services und
 * Adapter sind package-private (nach außen gilt der Port), und eine Klassenliste ließe sich von
 * hier aus gar nicht schreiben. Der Scan hält die Sichtbarkeitsgrenze und die Testbarkeit zugleich.
 *
 * <p>Die Schulorganisation kommt mit, weil zwei Outbound-Adapter dieses Kontexts ihren
 * {@code port/in} rufen — ohne sie fehlte dem Slice das Gegenüber. Die Richtung ist einseitig und
 * bleibt es: Der Schulorganisations-Kontext lässt sich allein starten, dieser nicht.
 */
@Configuration
@ComponentScan("de.openclassware.elternsprechtag.sprechtag")
@Import(SchulorganisationKontextTestConfig.class)
public class SprechtagKontextTestConfig {}
