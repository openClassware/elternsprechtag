package de.openclassware.elternsprechtag.sprechtag.adapter.out.event;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Ereignisse;
import de.openclassware.elternsprechtag.sprechtag.domain.Ereignis;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Der Ausgang für Domänen-Ereignisse — und die <b>einzige</b> Stelle im Sprechtag-Kontext, die
 * {@link ApplicationEventPublisher} kennt (ADR 0003).
 *
 * <p>Die erprobte Semantik bleibt bei den Empfängern: {@code @TransactionalEventListener} mit
 * {@code AFTER_COMMIT} plus {@code @Async}. Rollt die Transaktion zurück, wird nichts zugestellt.
 */
@RequiredArgsConstructor
@Component
class SpringEreignisse implements Ereignisse {

  private final ApplicationEventPublisher publisher;

  @Override
  public void veroeffentliche(Ereignis ereignis) {
    publisher.publishEvent(ereignis);
  }
}
