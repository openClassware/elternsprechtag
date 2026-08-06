package de.openclassware.elternsprechtag.services;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Stellt {@link I18NProvider} und {@link ElternsprechtagProperties} bereit, die {@link
 * AbsageBenachrichtigungService} für Betreff und Text der Mail braucht — die
 * {@code @DataJpaTest}-Slices laden {@link BenachrichtigungConfig} und die
 * {@code @ConfigurationProperties} der Anwendung nicht mit. Bewusst der echte Provider auf
 * {@code vaadin-i18n/translations.properties} und die echten Properties, damit die Tests die
 * tatsächlich ausgelieferten Texte prüfen.
 */
@TestConfiguration
@EnableConfigurationProperties(ElternsprechtagProperties.class)
class BenachrichtigungTextTestConfig {

  @Bean
  I18NProvider i18NProvider() {
    return new DefaultI18NProvider(List.of(Locale.GERMANY));
  }
}
