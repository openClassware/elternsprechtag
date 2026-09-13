package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Use Case: die Sprechtage des Organizers als Liste — Übersicht und Verwaltung.
 *
 * <p>Ein Read-Modell: Es entsteht aus dem Query-Port dieses Kontexts und den Klassennamen aus der
 * Schulorganisation, in Java zusammengefügt. Es darf veraltet sein; jede Entscheidung darauf wird
 * beim Schreiben am Aggregat erneut geprüft (ADR 0003).
 */
public interface Sprechtagsuebersicht {

  /** Alle Sprechtage, nach Datum aufsteigend. */
  List<SprechtagZeile> alle();

  /**
   * Eine Zeile der Übersicht — genau das, was Tabelle und Karten rendern. {@code ort} darf
   * {@code null} sein; {@code klassen} sind die Namen, alphabetisch.
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
      List<String> klassen) {}
}
