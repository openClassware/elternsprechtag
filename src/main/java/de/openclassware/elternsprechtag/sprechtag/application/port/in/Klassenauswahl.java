package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Use Case: die Klassen, aus denen ein Sprechtag wählt — für das Bearbeiten-Formular und für die
 * Klassenauswahl der Eltern.
 *
 * <p>Die Klassen gehören der Schulorganisation; dieser Kontext liest sie nur. Dass die Oberfläche
 * sie trotzdem über einen Use Case dieses Kontexts bekommt, hält die Regel „Presenter rufen
 * ausschließlich Use-Case-Ports" ein und macht die Grenze an genau einer Stelle sichtbar.
 */
public interface Klassenauswahl {

  /** Alle Klassen, nach Namen aufsteigend. */
  List<KlasseOption> alleKlassen();

  /** Eine wählbare Klasse — Id und Anzeigename, mehr braucht keine Ansicht. */
  record KlasseOption(UUID id, String name) {}
}
