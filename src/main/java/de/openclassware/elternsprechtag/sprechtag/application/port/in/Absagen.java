package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.UUID;

/**
 * Use Case: einen veröffentlichten Sprechtag absagen — der Weg, der die betroffenen Eltern erreicht.
 * Der Versand hängt am Ereignis, das dabei entsteht, und läuft erst nach dem Commit.
 */
public interface Absagen {

  void sageAb(UUID id);

  /**
   * Wie viele Absagen hinausgehen würden — aktive Buchungen, je Adresse und Kind einmal gezählt
   * (ADR 0007), denn genau so versendet der Versand auch. Trägt den Bestätigungsdialog vor dem
   * Absagen: Der Organizer soll sehen, für wie viele Kinder er gleich Eltern behelligt.
   *
   * <p>Die Zahl kommt aus einem Read-Modell und darf veraltet sein. Sie ist eine Anzeige, keine
   * Zusage — wen die Absage tatsächlich erreicht, entscheidet der Versand nach dem Commit.
   */
  long zaehleBetroffeneKinder(UUID id);
}
