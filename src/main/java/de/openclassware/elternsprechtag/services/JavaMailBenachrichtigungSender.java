package de.openclassware.elternsprechtag.services;

import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.services.AbsageBenachrichtigungService.AbsageEmpfaenger;
import de.openclassware.elternsprechtag.ui.Formats;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * {@link BenachrichtigungSender} mit echtem SMTP-Versand über einen {@link JavaMailSender}. Baut
 * aus dem Empfänger-Record eine feste Absage-Mail (Betreff + Text mit Titel, Datum und ggf. Ort —
 * <b>kein Absage-Grund</b>); alle Texte kommen aus {@code vaadin-i18n/translations.properties} über
 * den {@link I18NProvider}, das Datum über {@link Formats}. In Produktion die aktive
 * Implementierung, sobald SMTP konfiguriert ist ({@code spring.mail.host}); ohne Konfiguration
 * greift die {@link LoggingBenachrichtigungSender Log-Attrappe} — siehe {@link
 * BenachrichtigungConfig}.
 *
 * <p>Ein Zustellfehler wirft eine {@link org.springframework.mail.MailException}
 * (RuntimeException); die aufrufende Kernlogik fängt sie je Adresse ab und versendet best-effort
 * weiter.
 */
@Slf4j
class JavaMailBenachrichtigungSender implements BenachrichtigungSender {

  private static final Locale LOCALE = Locale.GERMANY;

  private final JavaMailSender mailSender;
  private final I18NProvider i18n;
  private final String absender;
  private final String schulname;

  JavaMailBenachrichtigungSender(
      JavaMailSender mailSender, I18NProvider i18n, String absender, String schulname) {
    this.mailSender = mailSender;
    this.i18n = i18n;
    this.absender = absender;
    this.schulname = schulname;
  }

  @Override
  public void sende(AbsageEmpfaenger empfaenger) {
    String datum = Formats.dateLong(empfaenger.datum());
    String betreff = i18n.getTranslation("absage.mail.subject", LOCALE, empfaenger.titel(), datum);
    String text =
        i18n.getTranslation("absage.mail.body", LOCALE, empfaenger.titel(), datum, schulname);

    SimpleMailMessage nachricht = new SimpleMailMessage();
    nachricht.setFrom(absender);
    nachricht.setTo(empfaenger.email());
    nachricht.setSubject(betreff);
    nachricht.setText(text);
    mailSender.send(nachricht);
  }
}
