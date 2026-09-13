package de.openclassware.elternsprechtag.sprechtag.application.port.in;

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
   * Die Klassen, die ein Sprechtag einladen kann — nach Namen aufsteigend, ohne stillgelegte.
   *
   * <p>Nicht dasselbe wie „alle Klassen": Wer die Namen eines <em>bestehenden</em> Sprechtags
   * auflösen will, fragt nicht hier, sondern bekommt sie vom Use Case, der den Sprechtag liefert
   * ({@link Sprechtagsuebersicht}, {@link Sprechtagszugang}). Der Unterschied trägt, sobald eine
   * Klasse stillgelegt wird.
   */
  List<KlasseOption> waehlbareKlassen();

  /** Eine wählbare Klasse — Id und Anzeigename, mehr braucht keine Ansicht. */
  record KlasseOption(UUID id, String name) {}
}
