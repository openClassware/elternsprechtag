package de.openclassware.elternsprechtag.services;

/**
 * Port für den Versand genau einer Nachricht an genau einen Empfänger. Der Port ist
 * <b>anlassneutral</b>: Er kennt weder Absage noch Buchung, sondern nur Empfänger, Betreff und Text
 * — Betreff und Text entstehen in der jeweiligen Kernlogik (z. B. {@link
 * AbsageBenachrichtigungService}). So kommt eine weitere Mailart ohne eine zweite Port-Methode aus.
 *
 * <p>In Produktion versendet der {@link JavaMailBenachrichtigungSender} echte Mails, sobald SMTP
 * konfiguriert ist; ohne Konfiguration protokolliert die Log-Attrappe nur (kein echtes SMTP).
 */
public interface BenachrichtigungSender {

  /**
   * Vollständiger Vertrag zwischen Kernlogik und Sender: Empfänger-Adresse, fertiger Betreff und
   * fertiger Text. Eine Implementierung braucht keinerlei Entities oder Repository-Zugriff.
   */
  record Nachricht(String empfaenger, String betreff, String text) {}

  /**
   * Versendet die Nachricht an ihren Empfänger. Darf bei Zustellproblemen eine {@link
   * RuntimeException} werfen; die aufrufende Kernlogik fängt Einzelfehler ab und versendet
   * best-effort an die übrigen Empfänger weiter.
   */
  void sende(Nachricht nachricht);
}
