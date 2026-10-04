package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.Mailart;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Query-Port der Buchungsseite: handgeschriebenes SQL an den Aggregaten vorbei. Die Zeilen lesen
 * ausschließlich die <em>eingefrorenen</em> Spalten der Buchung — kein Join in die Stammdaten, damit
 * ein Import die Auswertung eines vergangenen Sprechtags nicht verändert (ADR 0003).
 */
public interface BuchungsAnsichten {

  /**
   * Alle Buchungen eines Sprechtags, geltende wie stornierte, chronologisch nach Startzeit — die
   * Grundlage der Auswertung. Stornierte führt sie, damit ihre Angaben auf Verlangen der Familie
   * entfernt werden können (Issue #129); was davon zählt, entscheidet der Use Case.
   */
  List<AuswertungsZeile> buchungenFuerAuswertung(SprechtagId sprechtag);

  /**
   * Die Buchungen genau dieser Ids, chronologisch — die Menge eines Vorgangs für die
   * Bestätigungsmail. Unbekannte Ids liefern schlicht keine Zeile.
   */
  List<BelegZeile> belege(List<BuchungId> buchungen);

  /**
   * Die Belege der aktiven Buchungen dieses Sprechtags — die Grundlage einer Absage. Je Buchung
   * eine Zeile, nach Empfänger und Uhrzeit sortiert: Der Versand bündelt selbst je Adresse und Kind
   * (ADR 0007) und muss wissen, welche Buchungen eine Nachricht trägt (Issue #110).
   */
  List<BelegZeile> aktiveBelege(SprechtagId sprechtag);

  /**
   * Wie viele Empfänger — je Adresse und Kind (ADR 0007) — {@link #aktiveBelege(SprechtagId)}
   * umfasst, also wie viele Absagen hinausgehen würden.
   */
  long zaehleAktiveEmpfaenger(SprechtagId sprechtag);

  /**
   * Die Ids der aktiven, noch nicht erinnerten Buchungen dieses Sprechtags — die Kandidaten eines
   * Scheduler-Laufs (Issue #107). Der Zeitstempel steht an der Buchung, nicht im Read-Modell hier:
   * Es liefert nur, wen der Scheduler ansehen muss; verbindlich geprüft und gesetzt wird am
   * Aggregat über {@code Termin.erinnereBuchung}.
   */
  List<BuchungId> aktiveUnerinnerteBuchungen(SprechtagId sprechtag);

  /**
   * Die Nachrichten dieses Sprechtags, die ihre Familie nicht erreicht haben (Issue #110) — je
   * Nachricht eine Zeile, nach Zeitpunkt sortiert. Eine Nachricht ist eine Art an einen Empfänger
   * (Adresse und Kind, ADR 0007) zu einem Zeitpunkt. Zur Liste gehört eine fehlgeschlagene
   * Zustellung nur, solange sie noch jemanden betrifft: an einer aktiven Buchung oder als
   * Ausfall-Nachricht (deren Buchung eben dadurch entfallen ist), und nie an einer anonymisierten.
   */
  List<NichtErreichtZeile> nichtErreicht(SprechtagId sprechtag);

  /**
   * Je Sprechtag, wie viele Nachrichten {@link #nichtErreicht(SprechtagId)} umfasst — gezählt je
   * Nachricht, nicht je Buchung. Sprechtage ohne solche Nachricht fehlen in der Map.
   */
  Map<UUID, Integer> nichtErreichtJeSprechtag();

  /**
   * Eine Zeile des Terminplans einer Lehrkraft. {@code notiz} und {@code anonymisiertAm} dürfen
   * {@code null} sein.
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
      String notiz,
      boolean storniert,
      boolean entfallen,
      LocalDateTime anonymisiertAm) {}

  /**
   * Eine Zeile für den Buchungsbeleg. Trägt die Sprechtag-Id mit, damit der Versand die Kopfdaten
   * über den Sprechtag-Port holt statt über einen Join, und die Buchungs-Id, damit er melden kann,
   * welche Buchungen eine Nachricht trug. {@code notiz} darf {@code null} sein.
   */
  record BelegZeile(
      BuchungId buchung,
      UUID sprechtagId,
      LocalTime zeit,
      String lehrkraftName,
      String fach,
      String notiz,
      String elternName,
      String schuelerName,
      String elternEmail,
      String klasse) {}

  /**
   * Eine Nachricht, die ihre Familie nicht erreicht hat. {@code elternNamen} sind die
   * verschiedenen Schreibweisen aller Buchungen, die sie trug — ein Nachtrag für dasselbe Kind kann
   * den Namen anders schreiben.
   */
  record NichtErreichtZeile(
      Mailart art,
      LocalDateTime zeitpunkt,
      String elternEmail,
      String schuelerName,
      String klasse,
      List<String> elternNamen) {}
}
