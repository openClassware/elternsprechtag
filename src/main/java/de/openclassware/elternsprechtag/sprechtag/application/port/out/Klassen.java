package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Der zweite Blick in die Schulorganisation, neben {@link Lehrauftraege}: die Klassen, aus denen ein
 * Sprechtag seine Teilnehmer wählt. Gelesen, nie geschrieben — die Abhängigkeit ist einseitig
 * (ADR 0003).
 *
 * <p>Dass die Klassennamen über einen eigenen Port kommen statt über einen Join, ist der Preis
 * dafür, dass kein SQL über die Kontextgrenze geht. Eine Schule hat Dutzende Klassen; die Kosten
 * sind vernachlässigbar.
 */
public interface Klassen {

  /**
   * Die Klassen, die ein Sprechtag einladen kann — nach Namen aufsteigend. Eine stillgelegte Klasse
   * ist nicht dabei.
   */
  List<KlasseDaten> waehlbare();

  /**
   * Alle Klassen, <b>auch stillgelegte</b>, nach Namen aufsteigend — zum Auflösen der Namen, die an
   * einem bestehenden Sprechtag hängen. Ein veröffentlichter Sprechtag hat seine Klassen
   * eingeladen; dass eine davon inzwischen stillgelegt ist, darf ihm den Namen nicht wegnehmen.
   */
  List<KlasseDaten> alle();

  record KlasseDaten(UUID id, String name) {}
}
