package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Der echte Mail-Adapter als Erfüller des {@code Benachrichtigungen}-Ports — mit allen vier
 * Fachservices, weil {@link MailBenachrichtigungen} sie zusammen braucht — und darunter die
 * {@link FakeBenachrichtigungSender Sender-Attrappe} statt SMTP. Gehört zum {@code Kern} von
 * {@code SprechtagKontextTestConfig}, nicht zu dessen Voll-Variante: Die brächte eine zweite
 * Erfüllung des Ports mit.
 */
@TestConfiguration
@Import({
  MailBenachrichtigungen.class,
  BuchungBestaetigungService.class,
  ErinnerungBenachrichtigungService.class,
  AbsageBenachrichtigungService.class,
  AusfallBenachrichtigungService.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextConfig.class
})
class MailVersandTestConfig {}
