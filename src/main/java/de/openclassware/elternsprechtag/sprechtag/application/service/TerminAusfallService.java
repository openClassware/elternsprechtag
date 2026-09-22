package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungStorniert;
import de.openclassware.elternsprechtag.sprechtag.domain.Ereignis;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lässt genau <b>einen</b> Termin entfallen — in seiner <b>eigenen</b> Transaktion
 * ({@link Propagation#REQUIRES_NEW}), aufgerufen aus dem Vorgang in {@code EntfallenLassenService}.
 *
 * <p>Der Grund für die eigene Transaktion ist hier ein anderer als beim Eltern-Submit und genau
 * dessen Gegenteil: Dort ist Rollback die gute Nachricht — keine halb gebuchte Familie. Beim
 * Ausfall wäre er die schlechte. Scheitert Termin 5 von 8, machte ein Rollback die anderen sieben
 * wieder buchbar, obwohl die Lehrkraft fehlt; eine Familie buchte in einen Termin, an dem niemand
 * erscheint. Mit {@code REQUIRES_NEW} committet jeder Termin für sich (dasselbe Muster wie im
 * Erinnerungs-Scheduler, {@code ErinnerungsMarkierungService}).
 */
@RequiredArgsConstructor
@Service
class TerminAusfallService {

  private final Termine termine;

  /**
   * Die Vorbedingung „veröffentlichter Sprechtag" wird hier <b>nicht</b> geprüft: Sie gilt dem
   * ganzen Vorgang und liegt deshalb vor der Schleife in {@code EntfallenLassenService}, wo sie
   * abweisen kann, bevor der erste Termin geschrieben ist.
   *
   * @return leer, wenn dieser Termin nichts beigetragen hat — es gibt ihn nicht, oder er war schon
   *     entfallen. Beides ist kein Fehler
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  Optional<Ausfall> lassEntfallen(TerminId terminId) {
    Optional<Termin> geladen = termine.lade(terminId);
    if (geladen.isEmpty()) {
      return Optional.empty();
    }
    Termin termin = geladen.get();

    // Vor dem Aufruf abgreifen: Danach hat der Termin keine aktive Buchung mehr, und mit ihr wäre
    // die Adresse weg, an die der Versand später geht.
    Optional<String> betroffeneAdresse =
        termin.aktiveBuchung().map(buchung -> buchung.familie().email());
    if (!termin.lassEntfallen()) {
      return Optional.empty();
    }
    termine.speichere(termin);

    // Was das Aggregat gemeldet hat, ist die Wahrheit darüber, ob und welche Buchung es getroffen
    // hat; gebündelt wird eine Ebene höher. Die Adresse steht nicht in der Meldung — die einzige
    // Angabe, die von außen dazukommt, und sie ist da, wo eine Meldung kam: Das Aggregat storniert
    // nur, was es als aktive Buchung vorfand.
    for (Ereignis ereignis : termin.ereignisseAbholen()) {
      if (ereignis instanceof BuchungStorniert meldung) {
        return Optional.of(new Ausfall(meldung.buchung(), betroffeneAdresse.orElseThrow()));
      }
    }
    return Optional.of(Ausfall.ohneBuchung());
  }

  /**
   * Was ein einzelner entfallener Termin zum Vorgang beiträgt. Die beiden Felder gehören zusammen
   * und sind <b>gemeinsam</b> {@code null}, wenn der Slot frei war — dann entfällt nur der Slot,
   * und niemand muss davon erfahren.
   */
  record Ausfall(BuchungId stornierteBuchung, String elternAdresse) {

    static Ausfall ohneBuchung() {
      return new Ausfall(null, null);
    }

    boolean trafEineFamilie() {
      return stornierteBuchung != null;
    }
  }
}
