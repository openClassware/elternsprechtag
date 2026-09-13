package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.LehrauftragId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Der Blick in die Schulorganisation — Stammdaten, die dieser Kontext liest und nie schreibt. Die
 * Abhängigkeit ist einseitig, und kein SQL-Statement joint über diese Grenze (ADR 0003).
 *
 * <p>Der Adapter dahinter ruft den {@code port/in} der Schulorganisation. Er ist die einzige Stelle
 * dieses Kontexts, die den anderen überhaupt kennt — was hier durchkommt, ist Text und eine Id,
 * kein fremdes Aggregat.
 */
public interface Lehrauftraege {

  Optional<LehrauftragDaten> lade(LehrauftragId id);

  /** Alle Lehraufträge einer Klasse, nach Fachname aufsteigend. Unbekannte Klasse: leere Liste. */
  List<LehrauftragDaten> fuerKlasse(UUID klasseId);

  /**
   * Was dieser Kontext von einem Lehrauftrag braucht — flach, ohne Objektgraph. Vorname und Nachname
   * stehen getrennt, weil die Auswertung nach Nachname sortiert.
   */
  record LehrauftragDaten(
      LehrauftragId id,
      LehrkraftId lehrkraft,
      String kuerzel,
      String vorname,
      String nachname,
      String klasse,
      String fach) {

    /** Wie die Oberfläche die Lehrkraft nennt. */
    public String anzeigeName() {
      return vorname + " " + nachname;
    }
  }
}
