package de.openclassware.elternsprechtag.sprechtag.application.port.in;

/**
 * Use Case: ein Lauf des Erinnerungs-Schedulers (Issue #107) — der erste zeitgesteuerte Vorgang im
 * Produkt. Kein Knopf beim Organizer: Ausgelöst wird der Lauf ausschließlich vom täglichen
 * {@code @Scheduled}-Trigger, nie von einer Oberfläche. Der Port existiert trotzdem, damit der
 * Trigger — wie jeder andere Aufrufer — ausschließlich gegen einen Use-Case-Port geht.
 */
public interface Erinnern {

  /**
   * Erinnert alle Familien, deren Buchung heute fällig ist — veröffentlichter Sprechtag,
   * gesetzter Erinnerungsvorlauf, aktive Buchung, noch nicht erinnert. „Verfallen statt
   * nachholen": Ein Vorlauf, dessen Tag bereits verstrichen ist, wird nicht nachgeholt.
   *
   * @return wie viele Buchungen in diesem Lauf erinnert wurden
   */
  int erinnere();
}
