package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.Mailart;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
   *
   * <p>{@code anonymisiertAm} ist der Tag, an dem der Anonymisierungs-Lauf die personenbezogenen
   * Angaben ersetzt hat, oder {@code null}, solange das nicht geschehen ist (Issue #127). Nur der
   * Tag: Die nächtliche Uhrzeit des Laufs sagt dem Organizer nichts.
   *
   * <p>{@code nichtErreicht} ist die Arbeitsliste fürs Telefon (Issue #110): je Nachricht, die eine
   * Familie nicht bekommen hat, ein Eintrag, nach Zeitpunkt sortiert. Leer, wenn alles rausging.
   */
  record SprechtagAuswertung(
      String titel,
      LocalDate datum,
      SprechtagStatus status,
      LocalDate anonymisiertAm,
      List<LehrkraftPlan> plaene,
      List<NichtErreicht> nichtErreicht) {}

  /**
   * Eine Nachricht, die ihre Familie nicht erreicht hat. Gegliedert, wie das Sekretariat arbeitet: je
   * Nachricht, nicht je Lehrkraft. Eine Nachricht gilt einem Kind an einer Adresse (ADR 0007); Kind
   * und Klasse genügen, um die Familie in der Schulverwaltung nachzuschlagen. {@code elternNamen}
   * bleibt eine Liste, weil ein Nachtrag für dasselbe Kind den Namen anders schreiben kann.
   *
   * <p>Die Adresse steht nur hier und nur für Gescheiterte: Ein Tippfehler darin ist der häufigste
   * Grund, und das Sekretariat soll ihn sehen können.
   */
  record NichtErreicht(
      Mailart art,
      LocalDateTime zeitpunkt,
      String email,
      String schuelerName,
      String klasse,
      List<String> elternNamen) {}

  /**
   * Terminplan einer Lehrkraft: Anzeigename/Kürzel, Anzahl aktiver Buchungen und die Zeilen in
   * chronologischer Reihenfolge. Eine Lehrkraft ohne Buchung hat {@code anzahl == 0} und eine leere
   * Zeilenliste.
   *
   * <p>{@code entfalleneAnzahl} ist die Anzahl ihrer entfallenen Termine an diesem Sprechtag
   * (Issue #160). Solange die Lehrkraft einen Lehrauftrag in einer teilnehmenden Klasse hat oder
   * aus mindestens einer (auch stornierten) Buchung bekannt ist, bleibt sie nach einem
   * Ganztags-Ausfall hier sichtbar und trägt den Hinweis. Eine Lehrkraft ohne Lehrauftrag und ohne
   * je eine Buchung gehabt zu haben, hat schlicht keinen Anzeigenamen — den liefert nur die
   * Schulorganisation oder eine eingefrorene Buchung, nie der Termin allein.
   *
   * <p>{@code zeilen} sind die geltenden Zusagen, {@code anzahl} zählt genau sie.
   * {@code stornierte} stehen gesondert und ebenfalls chronologisch daneben (Issue #129): Sie zählen
   * nicht zum Plan, bleiben aber erreichbar, damit ihre Angaben auf Verlangen der Familie entfernt
   * werden können.
   */
  record LehrkraftPlan(
      UUID lehrerId,
      String kuerzel,
      String anzeigeName,
      int anzahl,
      int entfalleneAnzahl,
      List<BuchungsZeile> zeilen,
      List<BuchungsZeile> stornierte) {}

  /**
   * Eine Buchungszeile im Terminplan einer Lehrkraft. Klasse und Fach sind der eingefrorene Stand
   * der Buchung, nicht der heutige Lehrauftrag — ein Import darf die Auswertung eines vergangenen
   * Sprechtags nicht verändern.
   *
   * <p>Die {@code buchungId} ist die Identität der Zeile: Nur mit ihr kann die Oberfläche eine
   * Aktion — Storno, Umbuchen, Angaben entfernen — auf genau diese Buchung beziehen.
   *
   * <p>{@code anonymisiertAm} ist der Tag, an dem die Angaben der Familie gefallen sind, oder
   * {@code null}, solange sie noch dastehen (Issue #129). Nur der Tag, wie am Sprechtag.
   *
   * <p>{@code entfallen} heißt: Der Termin der Zeile entfällt — eine stornierte Buchung darauf hat
   * der Ausfall der Lehrkraft zurückgenommen, nicht die Familie.
   */
  record BuchungsZeile(
      UUID buchungId,
      LocalTime startzeit,
      String schuelerName,
      String klasse,
      String fach,
      String elternName,
      String notiz,
      boolean storniert,
      boolean entfallen,
      LocalDate anonymisiertAm) {}
}
