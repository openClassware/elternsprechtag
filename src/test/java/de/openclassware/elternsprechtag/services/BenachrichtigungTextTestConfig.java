package de.openclassware.elternsprechtag.services;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Stellt den {@link I18NProvider} bereit, den {@link AbsageBenachrichtigungService} für Betreff und
 * Text der Mail braucht — die {@code @DataJpaTest}-Slices laden {@link BenachrichtigungConfig} nicht
 * mit. Bewusst der echte Provider auf {@code vaadin-i18n/translations.properties}, damit die Tests
 * die tatsächlich ausgelieferten Texte prüfen.
 */
@TestConfiguration
class BenachrichtigungTextTestConfig {

  @Bean
  I18NProvider i18NProvider() {
    return new DefaultI18NProvider(List.of(Locale.GERMANY));
  }
}
