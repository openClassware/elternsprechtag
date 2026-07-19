package de.openclassware.elternsprechtag.services;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Wählt die aktive {@link BenachrichtigungSender}-Implementierung: Ist SMTP konfiguriert
 * ({@code spring.mail.host} gesetzt), versendet der {@link JavaMailBenachrichtigungSender} echte
 * Mails; sonst greift die {@link LoggingBenachrichtigungSender Log-Attrappe} (Entwicklung/Test).
 */
@Configuration
class BenachrichtigungConfig {

  @Bean
  @ConditionalOnProperty(prefix = "spring.mail", name = "host")
  BenachrichtigungSender javaMailBenachrichtigungSender(
      JavaMailSender mailSender,
      I18NProvider i18n,
      @Value("${elternsprechtag.mail.absender}") String absender,
      ElternsprechtagProperties properties) {
    return new JavaMailBenachrichtigungSender(mailSender, i18n, absender, properties.getSchoolname());
  }

  @Bean
  @ConditionalOnMissingBean(BenachrichtigungSender.class)
  BenachrichtigungSender loggingBenachrichtigungSender() {
    return new LoggingBenachrichtigungSender();
  }

  /**
   * Stellt einen injizierbaren {@link I18NProvider} für den Mailtext bereit. Der Versand läuft in
   * einem {@code @Async}-Hintergrund-Thread ohne Vaadin-{@code UI}-Kontext, sodass das übliche
   * {@code Component#getTranslation(...)} dort nicht greift — der Sender nutzt deshalb direkt den
   * Provider, der aus {@code vaadin-i18n/translations.properties} liest. {@link ConditionalOnMissingBean}
   * lässt einen bereits vorhandenen (z. B. von Vaadin registrierten) Provider unangetastet.
   */
  @Bean
  @ConditionalOnMissingBean(I18NProvider.class)
  I18NProvider i18NProvider() {
    return new DefaultI18NProvider(List.of(Locale.GERMANY));
  }
}
