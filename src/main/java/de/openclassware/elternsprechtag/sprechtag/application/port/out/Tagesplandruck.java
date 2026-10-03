package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Drucken.Datei;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Ausgangsport: setzt Tagespläne als Dateien (Issue #120). Hinter ihm liegt die PDF-Bibliothek —
 * die Anwendung kennt nur Blätter und Bytes.
 *
 * <p>Auch die Dateinamen entstehen hier, nicht im Use Case: Sie tragen übersetzte Wörter, und die
 * Übersetzungen sind Sache der Adapter.
 */
public interface Tagesplandruck {

  /** Ein ZIP mit einem PDF je Blatt. {@code stand} steht im Kopf jedes Blatts und vorn im Namen. */
  Datei gebuendelt(Kopf kopf, List<Blatt> blaetter, LocalDateTime stand);

  /**
   * Was vom Sprechtag auf jedes Blatt gehört. {@code ort} darf {@code null} sein;
   * {@code anonymisiertAm} ist der Tag des Anonymisierungs-Laufs oder {@code null} — dann erklärt
   * ein Vermerk auf dem Blatt die Platzhalter, wie der Hinweis in der Auswertung.
   */
  record Kopf(String titel, LocalDate datum, String ort, LocalDate anonymisiertAm) {}

  /** Das Blatt einer Lehrkraft: ihre Slots chronologisch. {@code kuerzel} darf fehlen. */
  record Blatt(String kuerzel, String anzeigeName, List<Zeile> zeilen) {}

  /**
   * Ein Slot. Ist er frei, sind alle Angaben zur Familie {@code null}. {@code entferntAm} ist der
   * Tag, an dem die Angaben dieser Familie gefallen sind — nur gesetzt, solange nicht ohnehin der
   * ganze Sprechtag anonymisiert ist. Eine E-Mail-Adresse gibt es hier nicht: Das Blatt bleibt im
   * Klassenzimmer liegen.
   */
  record Zeile(
      LocalTime startzeit,
      boolean frei,
      String schuelerName,
      String klasse,
      String fach,
      String elternName,
      String notiz,
      LocalDate entferntAm) {}
}
