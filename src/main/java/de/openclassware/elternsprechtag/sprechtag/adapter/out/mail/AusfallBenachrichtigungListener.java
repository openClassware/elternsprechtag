package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.domain.AusfallGemeldet;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Verbindet den Ausfall-Vorgang (#156) mit dem Versand — Spiegelbild zu
 * {@link ErinnerungBenachrichtigungListener}: reagiert auf {@link AusfallGemeldet} erst <em>nach
 * Commit</em>, damit keine Mail einen Ausfall behaupten kann, der zurückgerollt wurde, und
 * {@link Async} in einem eigenen Thread, damit die Organizer-Oberfläche nicht auf SMTP wartet.
 */
@Component
@RequiredArgsConstructor
class AusfallBenachrichtigungListener {

  private final AusfallBenachrichtigungService service;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onAusfallGemeldet(AusfallGemeldet ereignis) {
    service.benachrichtige(ereignis.stornierteBuchungen());
  }
}
