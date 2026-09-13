package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Query-Port der Sprechtagseite: handgeschriebenes SQL an den Aggregaten vorbei — der Leseweg. Die
 * Übersicht des Organizers zeigt Dutzende Sprechtage; sie über Aggregate zu bauen hieße, jedes
 * einzeln zu laden.
 *
 * <p>Die Zeilen tragen die Klassen nur als Id-Liste. Die <em>Namen</em> der Klassen gehören der
 * Schulorganisation, und kein SQL joint über die Kontextgrenze (ADR 0003) — zusammengefügt wird im
 * Use Case.
 */
public interface SprechtagAnsichten {

  /** Alle Sprechtage, nach Datum aufsteigend. */
  List<SprechtagZeile> alle();

  /** Die Kopfdaten eines Sprechtags — was Auswertung, Buchungsoptionen und Mailversand brauchen. */
  Optional<Kopf> kopf(SprechtagId id);

  /**
   * Eine Zeile der Organizer-Übersicht. {@code ort} darf {@code null} sein.
   */
  record SprechtagZeile(
      UUID id,
      String titel,
      LocalDate datum,
      LocalTime beginn,
      LocalTime ende,
      String ort,
      SprechtagStatus status,
      String accessToken,
      List<UUID> klasseIds) {}

  /** {@code ort} darf {@code null} sein; {@code schulkontakt} ist am Sprechtag Pflicht. */
  record Kopf(
      SprechtagId id,
      String titel,
      LocalDate datum,
      String ort,
      String schulkontakt,
      List<UUID> klasseIds) {}
}
