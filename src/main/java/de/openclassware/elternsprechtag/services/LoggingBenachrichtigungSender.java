package de.openclassware.elternsprechtag.services;

import lombok.extern.slf4j.Slf4j;

/**
 * Fallback-{@link BenachrichtigungSender}: protokolliert Empfänger und Betreff der Benachrichtigung,
 * ohne echtes SMTP. Greift, solange kein SMTP konfiguriert ist (kein {@code spring.mail.host}); mit
 * Konfiguration übernimmt der {@link JavaMailBenachrichtigungSender} — siehe
 * {@link BenachrichtigungConfig}.
 */
@Slf4j
class LoggingBenachrichtigungSender implements BenachrichtigungSender {

  @Override
  public void sende(Nachricht nachricht) {
    log.info(
        "Benachrichtigung (kein SMTP konfiguriert) an {} — \"{}\"",
        nachricht.empfaenger(),
        nachricht.betreff());
  }
}
