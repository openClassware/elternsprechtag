package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: der Terminplan eines Sprechtags je beteiligter Lehrkraft — die Auswertungsansicht des
 * Organizers.
 */
public interface Auswerten {

  /** Leeres Optional, wenn der Sprechtag nicht existiert. */
  Optional<SprechtagAuswertung> werteAus(UUID sprechtagId);

  /**
   * Kopfdaten plus je beteiligter Lehrkraft ein Terminplan. Die Pläne sind alphabetisch nach
   * Lehrkraft-Nachname sortiert.
   */
  record SprechtagAuswertung(String titel, LocalDate datum, List<LehrkraftPlan> plaene) {}

  /**
   * Terminplan einer Lehrkraft: Anzeigename/Kürzel, Anzahl aktiver Buchungen und die Zeilen in
   * chronologischer Reihenfolge. Eine Lehrkraft ohne Buchung hat {@code anzahl == 0} und eine leere
   * Zeilenliste.
   */
  record LehrkraftPlan(
      UUID lehrerId, String kuerzel, String anzeigeName, int anzahl, List<BuchungsZeile> zeilen) {}

  /**
   * Eine Buchungszeile im Terminplan einer Lehrkraft. Klasse und Fach sind der eingefrorene Stand
   * der Buchung, nicht der heutige Lehrauftrag — ein Import darf die Auswertung eines vergangenen
   * Sprechtags nicht verändern.
   */
  record BuchungsZeile(
      LocalTime startzeit,
      String schuelerName,
      String klasse,
      String fach,
      String elternName,
      String notiz) {}
}
