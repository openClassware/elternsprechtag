package de.openclassware.elternsprechtag.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Der Organizer-Zugang wird im Container ausschliesslich ueber Umgebungsvariablen gesetzt — lokal
 * greifen dagegen immer die Defaults. Dieser Test faehrt den Kontext deshalb mit einer simulierten
 * Umgebung hoch und prueft, was in der Demo tatsaechlich passiert: dass der gesetzte bcrypt-Hash
 * beim Login akzeptiert wird.
 *
 * <p>Hintergrund: hiessen die Variablen wie die Properties (ELTERNSPRECHTAG_SECURITY_ORGANIZER_*),
 * bindet Springs Relaxed Binding sie direkt an die Property und verdraengt das {bcrypt}-Praefix aus
 * application.properties — der Login scheitert dann an "no default password encoder configured".
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = OrganizerCredentialsEnvTest.SimulierteUmgebung.class)
class OrganizerCredentialsEnvTest {

  private static final String KLARTEXT = "GeheimesDemoPasswort";
  private static final String HASH = new BCryptPasswordEncoder().encode(KLARTEXT);

  static class SimulierteUmgebung
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new SystemEnvironmentPropertySource(
                  "simulierteUmgebung",
                  Map.of("ORGANIZER_USERNAME", "demo", "ORGANIZER_PASSWORD_HASH", HASH)));
    }
  }

  @Autowired private UserDetailsService userDetailsService;

  @Test
  void nimmtDenHashAusDerUmgebungAn() {
    PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    var organizer = userDetailsService.loadUserByUsername("demo");

    assertThat(encoder.matches(KLARTEXT, organizer.getPassword())).isTrue();
  }
}
