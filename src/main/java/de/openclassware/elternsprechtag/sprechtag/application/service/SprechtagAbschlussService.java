package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schließt genau einen Sprechtag ab, wenn er vorbei ist — in seiner <b>eigenen</b> Transaktion
 * ({@link Propagation#REQUIRES_NEW}), aufgerufen aus dem Abschluss-Lauf in
 * {@code SprechtagLebenszyklusService}. Derselbe Grund wie bei {@code ErinnerungsMarkierungService}:
 * Ein Konflikt bei einem Sprechtag — etwa weil der Organizer ihn im selben Moment von Hand
 * abschließt — soll die übrigen des Laufs nicht mit zurückrollen.
 */
@RequiredArgsConstructor
@Service
class SprechtagAbschlussService {

  private final Sprechtage sprechtage;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  boolean schliesseAbWennVorbei(SprechtagId id, LocalDateTime jetzt) {
    Sprechtag sprechtag =
        sprechtage
            .lade(id)
            .orElseThrow(() -> new IllegalStateException("Sprechtag nicht gefunden: " + id.wert()));
    if (!sprechtag.schliesseAbWennVorbei(jetzt)) {
      // Zwischen Lesen des Read-Modells und Laden des Aggregats abgesagt oder von Hand
      // abgeschlossen, oder die Endzeit steht heute noch aus — kein Fehler.
      return false;
    }
    sprechtage.speichere(sprechtag);
    return true;
  }
}
