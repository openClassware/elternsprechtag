package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: die Tagespläne der Lehrkräfte als Datei — der Ersatz für einen eigenen Lehrkraft-Zugang
 * (Issue #120, {@code ABDECKUNG.md} Phase 5). Den Versand übernimmt der Organizer außerhalb der App.
 *
 * <p>Gedruckt wird <b>der vollständige Tag jeder Lehrkraft</b>, frisch aus der Datenbank und
 * unabhängig davon, wie die Auswertung gerade gefiltert ist: alle angebotenen Slots chronologisch,
 * belegte mit der Familie, freie als Platz für den Nachtrag am Tag selbst. Stornierte Buchungen und
 * entfallene Slots stehen nicht darauf — das Blatt zeigt, wer kommt.
 *
 * <p>Den Stand im Kopf setzt der Use Case: Es ist der Zeitpunkt des Exports. Er macht das Alter
 * des Blattes sichtbar und trägt damit „Änderungen nach dem Druck erreichen die Lehrkraft" als
 * {@code darf fehlen}.
 */
public interface Drucken {

  /**
   * Ein ZIP mit einem PDF je beteiligter Lehrkraft, auch für eine ohne Buchung: Jede Lehrkraft soll
   * ihr eigenes Dokument bekommen. Leer, wenn der Sprechtag nicht existiert.
   */
  Optional<Datei> druckePlaene(UUID sprechtagId);

  /** Die fertige Datei samt Namen und Medientyp, bereit zum Herunterladen. */
  record Datei(String name, String medientyp, byte[] inhalt) {}
}
