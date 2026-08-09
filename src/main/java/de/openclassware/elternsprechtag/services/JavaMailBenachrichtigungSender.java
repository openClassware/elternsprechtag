package de.openclassware.elternsprechtag.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * {@link BenachrichtigungSender} mit echtem SMTP-Versand über einen {@link JavaMailSender}. Reiner
 * Transport: baut aus der fertigen {@link Nachricht} eine {@link SimpleMailMessage} (Absender aus
 * {@code elternsprechtag.mail.absender}, reiner Text, kein Anhang) und versendet sie — Betreff und
 * Text formuliert der jeweilige Fachservice. In Produktion die aktive Implementierung, sobald SMTP
 * konfiguriert ist ({@code spring.mail.host}); ohne Konfiguration greift die {@link
 * LoggingBenachrichtigungSender Log-Attrappe} — siehe {@link BenachrichtigungConfig}.
 *
 * <p>Ein Zustellfehler wirft eine {@link org.springframework.mail.MailException}
 * (RuntimeException); die aufrufende Kernlogik fängt sie je Adresse ab und versendet best-effort
 * weiter.
 */
@Slf4j
class JavaMailBenachrichtigungSender implements BenachrichtigungSender {

  private final JavaMailSender mailSender;
  private final String absender;

  JavaMailBenachrichtigungSender(JavaMailSender mailSender, String absender) {
    this.mailSender = mailSender;
    this.absender = absender;
  }

  @Override
  public void sende(Nachricht nachricht) {
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom(absender);
    mail.setTo(nachricht.empfaenger());
    mail.setSubject(nachricht.betreff());
    mail.setText(nachricht.text());
    mailSender.send(mail);
  }
}
