package de.openclassware.elternsprechtag;

import de.openclassware.elternsprechtag.sprechtag.FakeBenachrichtigungen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Bringt den vollständigen Sprechtag-Kontext in einen Slice-Test: Use-Case-Services, alle
 * Outbound-Adapter außer dem Mailversand und den Ereignis-Eingang — und an Stelle des Mailversands
 * die Attrappe {@link FakeBenachrichtigungen}.
 *
 * <p>Die Versand-Tests unter {@code adapter.out.mail} wollen den echten Mail-Adapter mit einer
 * Sender-Attrappe darunter; sie setzen sich deshalb aus den Bausteinen {@link Kern} und
 * {@link Ereigniseingang} selbst zusammen. Zwei {@code Benachrichtigungen} in einem Kontext gäbe es
 * sonst.
 *
 * <p>{@code @TestConfiguration} statt {@code @Configuration}, damit die Tests mit vollem
 * Anwendungskontext sie nicht beim Scan aufsammeln — dort erfüllt der echte Mail-Adapter den Port.
 */
@TestConfiguration
@Import({SprechtagKontextTestConfig.Kern.class, SprechtagKontextTestConfig.Ereigniseingang.class})
public class SprechtagKontextTestConfig {

  @Bean
  FakeBenachrichtigungen fakeBenachrichtigungen(BuchungsAnsichten buchungsAnsichten) {
    return new FakeBenachrichtigungen(buchungsAnsichten);
  }

  /**
   * Der Kontext ohne einen Erfüller des {@code Benachrichtigungen}-Ports und ohne den
   * Ereignis-Eingang.
   *
   * <p>Bewusst ein {@code @ComponentScan} statt einer Liste einzelner Klassen — die Services und
   * Adapter sind package-private (nach außen gilt der Port), und eine Klassenliste ließe sich von
   * hier aus gar nicht schreiben. Der Scan hält die Sichtbarkeitsgrenze und die Testbarkeit
   * zugleich.
   *
   * <p>Drei Adapter bleiben ausgeschlossen, und zwar aus je eigenem Grund. Der <b>Web-Adapter</b>
   * ({@code adapter.in.web}) treibt den Kontext an, statt ihn zu bedienen — seine Presenter gehören
   * in keinen Slice-Test, der eine Regel prüft. Der <b>Mail-Adapter</b> ({@code adapter.out.mail})
   * brächte einen echten Sender mit; die Versand-Tests wollen dort ihre Attrappe und importieren
   * deshalb genau die Klassen, die sie prüfen. Der <b>Ereignis-Eingang</b> ({@code adapter.in.event})
   * stieße nach jedem Vorgang den Versand an — wer einen Mail-Service für sich prüft, will das
   * nicht; wer es will, nimmt {@link Ereigniseingang} dazu. Jeder weitere Adapter kommt automatisch
   * mit.
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
                      + "\\.(in\\.web|in\\.event|out\\.mail)\\..*"))
  @Import(SchulorganisationKontextTestConfig.class)
  public static class Kern {}

  /**
   * Der Ereignis-Eingang: Nach dem Commit eines Vorgangs ruft er den {@code Benachrichtigen}-Use-Case.
   * Ohne {@code @EnableAsync} läuft er im Thread des Tests — der Versand ist damit deterministisch
   * beobachtbar.
   */
  @Configuration
  @ComponentScan("de.openclassware.elternsprechtag.sprechtag.adapter.in.event")
  public static class Ereigniseingang {}
}
