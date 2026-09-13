package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Objects;

/**
 * Wem die Buchung gilt — als <em>Stand zum Buchungszeitpunkt</em>, nicht als Verweis: Lehrkraft,
 * Klasse und Fach sind eingefroren, damit ein periodischer Import der Stammdaten die Auswertung
 * eines vergangenen Sprechtags nicht verändert (ADR 0003).
 *
 * <p>{@code herkunft} ist die {@link LehrauftragId}, aus der diese Angaben stammen — nur eine Spur,
 * keine Quelle. Verschwindet der Lehrauftrag, bleibt die Buchung vollständig.
 */
public record Buchungsziel(
    LehrauftragId herkunft,
    LehrkraftId lehrkraft,
    String lehrkraftName,
    String lehrkraftKuerzel,
    String klasse,
    String fach) {

  public Buchungsziel {
    Objects.requireNonNull(herkunft, "herkunft");
    Objects.requireNonNull(lehrkraft, "lehrkraft");
    lehrkraftName = pflicht(lehrkraftName, "lehrkraftName");
    lehrkraftKuerzel = pflicht(lehrkraftKuerzel, "lehrkraftKuerzel");
    klasse = pflicht(klasse, "klasse");
    fach = pflicht(fach, "fach");
  }

  private static String pflicht(String wert, String feld) {
    if (wert == null || wert.isBlank()) {
      throw new IllegalArgumentException("Pflichtangabe fehlt: " + feld);
    }
    return wert.trim();
  }
}
