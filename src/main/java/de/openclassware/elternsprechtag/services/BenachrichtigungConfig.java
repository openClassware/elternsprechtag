package de.openclassware.elternsprechtag.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registriert den {@link LoggingBenachrichtigungSender} als Default-{@link BenachrichtigungSender},
 * solange keine andere Implementierung (z. B. {@code JavaMailSender}-basiert, späteres Ticket) im
 * Kontext vorhanden ist.
 */
@Configuration
class BenachrichtigungConfig {

  @Bean
  @ConditionalOnMissingBean(BenachrichtigungSender.class)
  BenachrichtigungSender loggingBenachrichtigungSender() {
    return new LoggingBenachrichtigungSender();
  }
}
