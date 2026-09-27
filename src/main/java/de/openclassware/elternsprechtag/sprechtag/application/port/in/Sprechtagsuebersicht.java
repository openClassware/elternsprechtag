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
   * {@code null} sein; {@code klassen} sind die Namen, alphabetisch. {@code datenfrist} ist
   * {@code null}, wo der Anonymisierungs-Lauf nie hinkommt: bei Entwürfen und Veröffentlichten.
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
      List<String> klassen,
      Datenfrist datenfrist) {}

  /**
   * Die Vorwarnung vor der Anonymisierung (Issue #128): bis wann die personenbezogenen Angaben
   * eines abgeschlossenen oder abgesagten Sprechtags noch vorhanden sind — oder, nach dem Lauf, seit
   * wann nicht mehr. Ein Feld mit {@link Art} statt zweier Daten: „noch da" und „entfernt" zugleich
   * gibt es nicht.
   */
  record Datenfrist(Art art, LocalDate datum) {

    public enum Art {
      /** {@code datum} ist der letzte Tag, an dem die Angaben sicher noch vorhanden sind. */
      VERFUEGBAR_BIS,
      /** {@code datum} ist der Tag, an dem der Lauf den Sprechtag anonymisiert hat. */
      ENTFERNT_AM
    }
  }
}
