package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungFaellig;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Verbindet den Erinnerungs-Scheduler mit dem Versand — Spiegelbild zu
 * {@link AbsageBenachrichtigungListener}: reagiert auf {@link ErinnerungFaellig} erst
 * <em>nach Commit</em>, damit die markierten Zeitstempel festgeschrieben sind, bevor der Versand
 * beginnt (ein fehlschlagender Versand rollt sie nicht zurück und verdoppelt die Erinnerung nicht),
 * und {@link Async} in einem eigenen Thread, damit der Scheduler-Lauf sofort zurückkehrt.
 */
@Component
@RequiredArgsConstructor
class ErinnerungBenachrichtigungListener {

  private final ErinnerungBenachrichtigungService service;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onErinnerungFaellig(ErinnerungFaellig ereignis) {
    service.erinnere(ereignis.buchungen());
  }
}
