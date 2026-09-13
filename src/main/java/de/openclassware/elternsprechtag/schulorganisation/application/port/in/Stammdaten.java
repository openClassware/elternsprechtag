package de.openclassware.elternsprechtag.schulorganisation.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Die Leseseite dieses Kontexts nach außen — <b>der einzige Weg</b>, auf dem der Sprechtag-Kontext
 * Stammdaten erreicht (ADR 0003, per ArchUnit geprüft).
 *
 * <p>Dieser Kontext hat keinen Web-Adapter: Alles mit Oberfläche gehört dem Sprechtag. Was hier
 * steht, ist deshalb genau das, was drüben gebraucht wird, und nichts darüber hinaus — eine Abfrage
 * auf Vorrat wäre eine Zusage, die niemand einlöst.
 *
 * <p>Die Grenze geht durch Java, nicht durch SQL: <b>Kein Statement joint über sie</b>. Wer Slots
 * und Lehrkräfte zusammen zeigen will, holt beides einzeln und fügt es im Use Case zusammen.
 *
 * <p>Ids sind hier bewusst nackte {@link UUID}s statt typisierter Ids: Die typisierten Ids beider
 * Kontexte sind verschiedene Typen für denselben Begriff, und einer von beiden über die Grenze
 * gereicht wäre genau die geteilte Abhängigkeit, die es nicht geben soll.
 */
public interface Stammdaten {

  /**
   * Die aktiven Lehraufträge einer Klasse, nach Fachname aufsteigend — je Eintrag die Lehrkraft und
   * ihr Fach.
   *
   * <p>Daraus beantwortet der Sprechtag-Kontext beides, wonach er fragt: <em>welche Lehrkräfte</em>
   * an einer Klasse hängen (je Lehrkraft der erste Eintrag) und <em>welche Fächer</em> eine
   * Lehrkraft dort unterrichtet (alle ihre Einträge). Zwei getrennte Abfragen dafür wären zweimal
   * dieselbe Zeilenmenge, einmal je Blickrichtung gefaltet — und die Faltung gehört dorthin, wo
   * entschieden wird, was angezeigt wird.
   */
  List<LehrauftragSchnappschuss> lehrauftraegeEinerKlasse(UUID klasseId);

  /**
   * Der Schnappschuss eines Lehrauftrags — das, was eine Buchung als Buchungsziel einfriert.
   *
   * <p>Liefert ihn <b>auch stillgelegt</b>: Eine Auswertung des letzten Schuljahrs muss lesbar
   * bleiben, auch wenn es den Lehrauftrag im Angebot nicht mehr gibt.
   */
  Optional<LehrauftragSchnappschuss> lehrauftrag(UUID lehrauftragId);

  /** Die Klassen, aus denen ein Sprechtag wählen kann — aktiv, nach Namen aufsteigend. */
  List<KlasseOption> waehlbareKlassen();

  /**
   * Alle Klassen, <b>auch stillgelegte</b>, nach Namen aufsteigend.
   *
   * <p>Die zweite Klassenliste ist kein Duplikat, sondern der Unterschied zwischen „was darf ein
   * neuer Sprechtag einladen" und „wie heißt die Klasse, die dieser Sprechtag eingeladen hat". Ohne
   * sie risse das Stilllegen einer Klasse einem bereits veröffentlichten Sprechtag den Namen weg —
   * in der Übersicht des Organizers und in der Klassenauswahl der Eltern.
   */
  List<KlasseOption> alleKlassen();

  /**
   * Ein Lehrauftrag mit den aufgelösten Namen. Flach: Der fremde Kontext bekommt Text, keine
   * Aggregate.
   */
  record LehrauftragSchnappschuss(
      UUID id,
      UUID lehrkraftId,
      String kuerzel,
      String vorname,
      String nachname,
      String klasse,
      String fach) {}

  /** Eine wählbare Klasse — Id und Anzeigename, mehr braucht keine Ansicht. */
  record KlasseOption(UUID id, String name) {}
}
