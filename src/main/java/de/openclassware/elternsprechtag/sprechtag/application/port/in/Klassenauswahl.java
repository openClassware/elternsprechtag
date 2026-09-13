package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Use Case: die Klassen, aus denen ein Sprechtag wählt — für das Bearbeiten-Formular.
 *
 * <p>Die Klassen gehören der Schulorganisation; dieser Kontext liest sie nur. Dass die Oberfläche
 * sie trotzdem über einen Use Case dieses Kontexts bekommt, hält die Regel „Presenter rufen
 * ausschließlich Use-Case-Ports" ein und macht die Grenze an genau einer Stelle sichtbar.
 */
public interface Klassenauswahl {

  /**
   * Die Klassen, die zur Auswahl stehen — nach Namen aufsteigend: die aktiven, <b>ergänzt um die
   * bereits gewählten</b>, auch wenn eine davon inzwischen stillgelegt ist.
   *
   * <p>Die Ergänzung ist der Punkt. Ein Formular zeigt keine Auswahl an, es <em>ist</em> die
   * Auswahl: Was es nicht anbietet, kann es beim Speichern auch nicht zurückschreiben. Böte es nur
   * die aktiven Klassen an, verlöre ein Entwurf eine inzwischen stillgelegte Klasse still beim
   * nächsten Speichern — auch wenn der Organizer nur den Titel geändert hat. Wer eine Klasse aus
   * einem Sprechtag nehmen will, soll das Häkchen entfernen, nicht warten, bis das Formular es für
   * ihn tut.
   *
   * @param bereitsGewaehlt die Klassen des bearbeiteten Sprechtags; leer beim Anlegen
   */
  List<KlasseOption> waehlbareKlassen(Collection<UUID> bereitsGewaehlt);

  /** Eine wählbare Klasse — Id und Anzeigename, mehr braucht keine Ansicht. */
  record KlasseOption(UUID id, String name) {}
}
