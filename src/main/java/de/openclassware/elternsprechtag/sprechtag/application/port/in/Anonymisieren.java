package de.openclassware.elternsprechtag.sprechtag.application.port.in;

/**
 * Use Case: die Buchungsdaten eines Sprechtags nach Ablauf der Aufbewahrungsfrist anonymisieren
 * (Issue #126). Kein Knopf beim Organizer — eine Löschfrist, die an einem Handgriff hängt, ist keine
 * Frist. Ausgelöst wird der Lauf ausschließlich vom täglichen {@code @Scheduled}-Trigger.
 */
public interface Anonymisieren {

  /**
   * Ein Lauf des täglichen Anonymisierungs-Schedulers: Für jeden Sprechtag, dessen
   * Aufbewahrungsfrist ab der Endzeit verstrichen ist, werden Elternname, Schülername und E-Mail
   * jeder Buchung durch Pseudonyme ersetzt und die Notiz geleert; danach bekommt der Sprechtag
   * seinen {@code anonymisiertAm}-Vermerk. Buchung und Termin bleiben stehen — die Auslastung ist
   * das Wissen, aus dem der nächste Sprechtag geplant wird.
   *
   * @return wie viele Sprechtage in diesem Lauf anonymisiert wurden
   */
  int anonymisiere();
}
