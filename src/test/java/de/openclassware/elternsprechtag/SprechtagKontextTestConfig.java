package de.openclassware.elternsprechtag;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Bringt den vollständigen Sprechtag-Kontext in einen Slice-Test: Use-Case-Services und alle
 * Outbound-Adapter.
 *
 * <p>Bewusst ein {@code @ComponentScan} statt einer Liste einzelner Klassen — die Services und
 * Adapter sind package-private (nach außen gilt der Port), und eine Klassenliste ließe sich von
 * hier aus gar nicht schreiben. Der Scan hält die Sichtbarkeitsgrenze und die Testbarkeit zugleich.
 *
 * <p>Zwei Adapter bleiben ausgeschlossen, und zwar aus je eigenem Grund. Der <b>Web-Adapter</b>
 * ({@code adapter.in.web}) treibt den Kontext an, statt ihn zu bedienen — seine Presenter gehören
 * in keinen Slice-Test, der eine Regel prüft. Der <b>Mail-Adapter</b> ({@code adapter.out.mail})
 * brächte einen echten Sender mit; die Versand-Tests wollen dort ihre Attrappe und importieren
 * deshalb genau die Klassen, die sie prüfen. Jeder weitere Outbound-Adapter kommt automatisch mit.
 *
 * <p>Die Schulorganisation kommt mit, weil zwei Outbound-Adapter dieses Kontexts ihren
 * {@code port/in} rufen — ohne sie fehlte dem Slice das Gegenüber. Die Richtung ist einseitig und
 * bleibt es: Der Schulorganisations-Kontext lässt sich allein starten, dieser nicht.
 */
@Configuration
@ComponentScan(
    value = "de.openclassware.elternsprechtag.sprechtag",
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern =
                "de\\.openclassware\\.elternsprechtag\\.sprechtag\\.adapter"
                    + "\\.(in\\.web|out\\.mail)\\..*"))
@Import(SchulorganisationKontextTestConfig.class)
public class SprechtagKontextTestConfig {}
