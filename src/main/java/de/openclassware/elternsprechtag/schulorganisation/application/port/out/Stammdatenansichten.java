package de.openclassware.elternsprechtag.schulorganisation.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Der Leseweg in die Datenbank: handgeschriebenes SQL über die Stammdaten-Tabellen, vorbei an den
 * Aggregaten (ADR 0003/0004).
 *
 * <p>Vier Aggregate einzeln zu laden, um einen Lehrauftrag mit Namen zu zeigen, wären vier Abfragen
 * für eine Zeile Text. Der Preis dieses zweiten Wegs ist, dass die Read-Modelle veralten dürfen —
 * hier ohne Folgen: Wer auf ihrer Grundlage bucht, prüft beim Schreiben ohnehin erneut am Termin-
 * Aggregat, und das Buchungsziel wird im selben Moment eingefroren.
 */
public interface Stammdatenansichten {

  /**
   * Die aktiven Lehraufträge einer Klasse, nach Fachname aufsteigend. Unbekannte oder stillgelegte
   * Klasse: leere Liste.
   */
  List<LehrauftragDaten> lehrauftraegeEinerKlasse(UUID klasseId);

  /**
   * Ein einzelner Lehrauftrag, <b>auch wenn er stillgelegt ist</b>. Auf ihn beruft sich eine
   * Buchung, und eine Buchung muss lesbar bleiben, nachdem der Lehrauftrag aus dem Angebot genommen
   * wurde.
   */
  Optional<LehrauftragDaten> lehrauftrag(UUID lehrauftragId);

  /** Die aktiven Klassen, nach Namen aufsteigend. */
  List<KlasseDaten> aktiveKlassen();

  /**
   * Alle Klassen, <b>auch stillgelegte</b>, nach Namen aufsteigend — um den Namen einer Klasse
   * aufzulösen, die an einem bestehenden Sprechtag teilnimmt. Eine stillgelegte Klasse steht nicht
   * mehr zur Wahl, aber ein Sprechtag, der sie schon eingeladen hat, muss sie weiter benennen
   * können.
   */
  List<KlasseDaten> alleKlassen();

  /**
   * Ein Lehrauftrag, flach und ohne Objektgraph — so, wie ihn ein fremder Kontext braucht. Vorname
   * und Nachname stehen getrennt, weil die Auswertung nach Nachname sortiert.
   */
  record LehrauftragDaten(
      UUID id,
      UUID lehrkraftId,
      String kuerzel,
      String vorname,
      String nachname,
      String klasse,
      String fach) {}

  record KlasseDaten(UUID id, String name) {}
}
