package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Query-Port der Buchungsseite: handgeschriebenes SQL an den Aggregaten vorbei. Die Zeilen lesen
 * ausschließlich die <em>eingefrorenen</em> Spalten der Buchung — kein Join in die Stammdaten, damit
 * ein Import die Auswertung eines vergangenen Sprechtags nicht verändert (ADR 0003).
 */
public interface BuchungsAnsichten {

  /** Aktive Buchungen eines Sprechtags, chronologisch nach Startzeit. */
  List<AuswertungsZeile> aktiveBuchungen(SprechtagId sprechtag);

  /**
   * Die Buchungen genau dieser Ids, chronologisch — die Menge eines Vorgangs für die
   * Bestätigungsmail. Unbekannte Ids liefern schlicht keine Zeile.
   */
  List<BelegZeile> belege(List<BuchungId> buchungen);

  /**
   * Die (deduplizierten) Eltern-Adressen mit aktiver Buchung an diesem Sprechtag — die Empfänger
   * einer Absage.
   */
  List<String> aktiveElternAdressen(SprechtagId sprechtag);

  /** Wie viele Adressen {@link #aktiveElternAdressen(SprechtagId)} liefern würde. */
  long zaehleAktiveElternAdressen(SprechtagId sprechtag);

  /**
   * Die Ids der aktiven, noch nicht erinnerten Buchungen dieses Sprechtags — die Kandidaten eines
   * Scheduler-Laufs (Issue #107). Der Zeitstempel steht an der Buchung, nicht im Read-Modell hier:
   * Es liefert nur, wen der Scheduler ansehen muss; verbindlich geprüft und gesetzt wird am
   * Aggregat über {@code Termin.erinnereBuchung}.
   */
  List<BuchungId> aktiveUnerinnerteBuchungen(SprechtagId sprechtag);

  /**
   * Eine Zeile des Terminplans einer Lehrkraft. {@code notiz} darf {@code null} sein.
   *
   * <p>Die {@code buchungId} trägt die Zeile, damit die Oberfläche eine Aktion auf genau diese
   * Buchung beziehen kann — ohne sie wäre eine Zeile im Terminplan nicht adressierbar.
   *
   * <p>Name und Kürzel der Lehrkraft stehen mit dabei, obwohl sie in aller Regel auch aus den
   * Stammdaten kämen: Sie sind der eingefrorene Stand und die einzige Quelle, wenn der Lehrauftrag
   * inzwischen verschwunden ist.
   */
  record AuswertungsZeile(
      UUID buchungId,
      UUID lehrkraftId,
      String lehrkraftName,
      String lehrkraftKuerzel,
      LocalTime startzeit,
      String schuelerName,
      String klasse,
      String fach,
      String elternName,
      String notiz) {}

  /**
   * Eine Zeile für den Buchungsbeleg. Trägt die Sprechtag-Id mit, damit der Versand die Kopfdaten
   * über den Sprechtag-Port holt statt über einen Join. {@code notiz} darf {@code null} sein.
   */
  record BelegZeile(
      UUID sprechtagId,
      LocalTime zeit,
      String lehrkraftName,
      String fach,
      String notiz,
      String elternName,
      String schuelerName,
      String elternEmail,
      String klasse) {}
}
