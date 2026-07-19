package de.openclassware.elternsprechtag.services;

import lombok.extern.slf4j.Slf4j;

/**
 * Default-{@link BenachrichtigungSender}: protokolliert die Benachrichtigung nur, ohne echtes SMTP.
 * Greift, solange keine versendende Implementierung (z. B. {@code JavaMailSender}-basiert, späteres
 * Ticket) registriert ist — siehe {@link BenachrichtigungConfig}.
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
