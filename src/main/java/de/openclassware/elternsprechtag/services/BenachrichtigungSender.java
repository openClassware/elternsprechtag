package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.services.AbsageBenachrichtigungService.AbsageEmpfaenger;

/**
 * Port für den Versand einer Absage-Benachrichtigung an genau einen Empfänger. Der
 * {@link AbsageEmpfaenger}-Record ist der vollständige Vertrag zwischen Kernlogik und Sender —
 * eine Implementierung braucht keinerlei Entities oder Repository-Zugriff.
 *
 * <p>In Produktion versendet der {@link JavaMailBenachrichtigungSender} echte Mails, sobald SMTP
 * konfiguriert ist; ohne Konfiguration protokolliert die Log-Attrappe nur (kein echtes SMTP).
 */
public interface BenachrichtigungSender {

  /**
   * Versendet die Absage-Benachrichtigung an den Empfänger. Darf bei Zustellproblemen eine
   * {@link RuntimeException} werfen; die aufrufende Kernlogik fängt Einzelfehler ab und versendet
   * best-effort an die übrigen Empfänger weiter.
   */
  void sende(AbsageEmpfaenger empfaenger);
}
