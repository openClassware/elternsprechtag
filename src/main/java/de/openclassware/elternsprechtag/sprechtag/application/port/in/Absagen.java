package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.UUID;

/**
 * Use Case: einen veröffentlichten Sprechtag absagen — der Weg, der die betroffenen Eltern erreicht.
 * Der Versand hängt am Ereignis, das dabei entsteht, und läuft erst nach dem Commit.
 */
public interface Absagen {

  void sageAb(UUID id);

  /**
   * Wie viele Eltern eine Absage erreichen würde — aktive Buchungen, je E-Mail-Adresse einmal
   * gezählt, denn genau so versendet der Versand auch. Trägt den Bestätigungsdialog vor dem
   * Absagen: Der Organizer soll sehen, wie viele Familien er gleich behelligt.
   *
   * <p>Die Zahl kommt aus einem Read-Modell und darf veraltet sein. Sie ist eine Anzeige, keine
   * Zusage — wen die Absage tatsächlich erreicht, entscheidet der Versand nach dem Commit.
   */
  long zaehleBetroffeneEltern(UUID id);
}
