package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
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
   *
   * <p>Der {@code status} steht mit dabei, damit sich daraus ableiten lässt, ob die Ansicht eine
   * Storno-Aktion anbietet. Die <em>Entscheidung</em> trifft der Presenter, nicht der View — und
   * verbindlich ist sie ohnehin erst im {@link Stornieren}-Use-Case.
   */
  record SprechtagAuswertung(
      String titel, LocalDate datum, SprechtagStatus status, List<LehrkraftPlan> plaene) {}

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
   *
   * <p>Die {@code buchungId} ist die Identität der Zeile: Nur mit ihr kann die Oberfläche eine
   * Aktion — heute das Storno — auf genau diese Buchung beziehen.
   */
  record BuchungsZeile(
      UUID buchungId,
      LocalTime startzeit,
      String schuelerName,
      String klasse,
      String fach,
      String elternName,
      String notiz) {}
}
