package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Der Lebenszyklus eines Sprechtags. Die erlaubten Übergänge stehen hier <b>einmal</b> und werden
 * vom Aggregat durchgesetzt — vorher kannte sie nur eine Enum-Methode, an der ein zweiter
 * Schreibpfad („Speichern" mit Zielstatus) vorbeilief (`ABDECKUNG.md` Z. 93).
 *
 * <p>Der Rückweg {@code VEROEFFENTLICHT -> ENTWURF} ist erlaubt, aber nur solange niemand gebucht
 * hat; darüber entscheidet das Aggregat, nicht diese Tabelle (`ABDECKUNG.md` Z. 91 f.).
 */
public enum SprechtagStatus {
  ENTWURF,
  VEROEFFENTLICHT,
  ABGESAGT,
  ABGESCHLOSSEN;

  /** Erlaubte Folgestatus für diesen Status. Endzustände liefern eine leere Menge. */
  public Set<SprechtagStatus> erlaubteUebergaenge() {
    return switch (this) {
      case ENTWURF -> EnumSet.of(VEROEFFENTLICHT);
      case VEROEFFENTLICHT -> EnumSet.of(ABGESCHLOSSEN, ABGESAGT, ENTWURF);
      case ABGESAGT, ABGESCHLOSSEN -> EnumSet.noneOf(SprechtagStatus.class);
    };
  }

  /** Abgesagt und abgeschlossen sind endgültig: Von hier aus ändert sich nichts mehr. */
  public boolean istEndzustand() {
    return erlaubteUebergaenge().isEmpty();
  }
}
