package de.openclassware.elternsprechtag.services;

import lombok.extern.slf4j.Slf4j;

/**
 * Fallback-{@link BenachrichtigungSender}: protokolliert die Benachrichtigung nur, ohne echtes SMTP.
 * Greift, solange kein SMTP konfiguriert ist (kein {@code spring.mail.host}); mit Konfiguration
 * übernimmt der {@link JavaMailBenachrichtigungSender} — siehe {@link BenachrichtigungConfig}.
 */
@Slf4j
class LoggingBenachrichtigungSender implements BenachrichtigungSender {

  @Override
  public void sende(AbsageBenachrichtigungService.AbsageEmpfaenger empfaenger) {
    log.info(
        "Absage-Benachrichtigung (kein SMTP konfiguriert) an {} — Sprechtag \"{}\" am {}",
        empfaenger.email(),
        empfaenger.titel(),
        empfaenger.datum());
  }
}
