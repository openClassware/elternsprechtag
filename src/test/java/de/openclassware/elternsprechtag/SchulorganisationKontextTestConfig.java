package de.openclassware.elternsprechtag;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Bringt den vollständigen Schulorganisations-Kontext in einen Slice-Test: die beiden
 * Use-Case-Services und die Outbound-Adapter.
 *
 * <p>Wie bei {@link SprechtagKontextTestConfig} bewusst ein {@code @ComponentScan} statt einer
 * Klassenliste — Services und Adapter sind package-private, nach außen gilt der Port.
 */
@Configuration
@ComponentScan("de.openclassware.elternsprechtag.schulorganisation")
public class SchulorganisationKontextTestConfig {}
