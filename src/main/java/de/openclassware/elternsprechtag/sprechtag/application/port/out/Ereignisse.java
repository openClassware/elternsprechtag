package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.Ereignis;

/**
 * Ausgang für Domänen-Ereignisse. Der Adapter dahinter ist die einzige Stelle im Projekt, die einen
 * {@code ApplicationEventPublisher} kennt (ADR 0003).
 */
public interface Ereignisse {

  void veroeffentliche(Ereignis ereignis);
}
