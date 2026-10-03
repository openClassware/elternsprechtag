package de.openclassware.elternsprechtag.sprechtag.adapter.in.event;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Benachrichtigen;
import de.openclassware.elternsprechtag.sprechtag.domain.AusfallErfasst;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungenBestaetigt;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungFaellig;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagAbgesagt;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Der Eingang für die gebündelten Vorgangs-Ereignisse: Er reicht jedes an den
 * {@link Benachrichtigen}-Use-Case weiter — erst <em>nach Commit</em>
 * ({@link TransactionPhase#AFTER_COMMIT}), damit der Vorgang festgeschrieben ist, bevor der Versand
 * beginnt (er kann ihn nie zurückrollen), und {@link Async} in einem eigenen Thread, damit
 * Organizer, Eltern und Scheduler sofort weiterkommen, ohne auf den Mailserver zu warten.
 *
 * <p>Ein Eingangs-Adapter wie der Scheduler: Er treibt einen Use Case an, statt einen Port zu
 * erfüllen. Bewusst eine eigene Bean, damit der Aufruf über den Spring-Proxy läuft — nur so greift
 * {@code @Async} überhaupt.
 *
 * <p>Die feinkörnigen Ereignisse der Aggregate erreichen diese Stelle nie; der Use Case des Vorgangs
 * hat sie abgeholt und zu genau einem Ereignis gebündelt. Eine Familie mit vier Terminen bekommt
 * deshalb eine Mail, nicht vier.
 */
@RequiredArgsConstructor
@Component
class BenachrichtigungListener {

  private final Benachrichtigen benachrichtigen;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onBuchungenBestaetigt(BuchungenBestaetigt ereignis) {
    benachrichtigen.bestaetige(ereignis);
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onErinnerungFaellig(ErinnerungFaellig ereignis) {
    benachrichtigen.erinnere(ereignis);
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onSprechtagAbgesagt(SprechtagAbgesagt ereignis) {
    benachrichtigen.sageAb(ereignis);
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onAusfallErfasst(AusfallErfasst ereignis) {
    benachrichtigen.meldeAusfall(ereignis);
  }
}
