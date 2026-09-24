package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.domain.AusfallErfasst;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Verbindet die Ausfall-Sammelaktion (Issue #156) mit dem Versand — Spiegelbild zu
 * {@link ErinnerungBenachrichtigungListener}: reagiert auf {@link AusfallErfasst} erst
 * <em>nach Commit</em>, damit die stornierten Buchungen festgeschrieben sind, bevor der Versand
 * beginnt, und {@link Async} in einem eigenen Thread, damit der Organizer nach dem Bestätigen
 * sofort weiterarbeiten kann.
 */
@Component
@RequiredArgsConstructor
class AusfallBenachrichtigungListener {

  private final AusfallBenachrichtigungService service;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onAusfallErfasst(AusfallErfasst ereignis) {
    service.benachrichtige(ereignis.buchungen());
  }
}
