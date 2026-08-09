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
 * Stellt den Service-Tests die Bausteine bereit, aus denen {@link AbsageBenachrichtigungService}
 * Betreff und Text formuliert: den echten {@link DefaultI18NProvider} auf
 * {@code vaadin-i18n/translations.properties} und die {@link ElternsprechtagProperties} aus
 * {@code application.properties}. Bewusst nicht {@link BenachrichtigungConfig} importiert — die
 * brächte zusätzlich einen echten Sender-Bean mit, wo die Tests die Attrappe wollen.
 */
@TestConfiguration
@EnableConfigurationProperties(ElternsprechtagProperties.class)
class BenachrichtigungTextConfig {

  @Bean
  I18NProvider i18NProvider() {
    return new DefaultI18NProvider(List.of(Locale.GERMANY));
  }
}
