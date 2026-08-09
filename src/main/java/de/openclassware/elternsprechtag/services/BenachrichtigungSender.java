package de.openclassware.elternsprechtag.services;

/**
 * Port für den Versand genau einer Benachrichtigung an genau einen Empfänger. Der Port ist
 * <b>anlassneutral</b>: Er transportiert nur Empfänger, Betreff und Text und weiß nicht, worum es
 * geht — Betreff und Text entstehen im jeweiligen Fachservice (z. B. {@link
 * AbsageBenachrichtigungService}). So braucht eine weitere Mailart keine zweite Methode am Port.
 *
 * <p>In Produktion versendet der {@link JavaMailBenachrichtigungSender} echte Mails, sobald SMTP
 * konfiguriert ist; ohne Konfiguration protokolliert die Log-Attrappe nur (kein echtes SMTP).
 */
public interface BenachrichtigungSender {

  /**
   * Vollständiger Vertrag zwischen Kernlogik und Sender: fertig formulierte Nachricht an eine
   * Adresse. Eine Implementierung braucht keinerlei Entities, Repository-Zugriff oder Kenntnis des
   * Anlasses.
   */
  record Nachricht(String empfaenger, String betreff, String text) {}

  /**
   * Versendet die Nachricht an den Empfänger. Darf bei Zustellproblemen eine
   * {@link RuntimeException} werfen; die aufrufende Kernlogik fängt Einzelfehler ab und versendet
   * best-effort an die übrigen Empfänger weiter.
   */
  void sende(Nachricht nachricht);
}
