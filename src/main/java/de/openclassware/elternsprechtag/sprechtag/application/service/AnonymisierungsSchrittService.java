package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.Aufbewahrungsfrist;
import de.openclassware.elternsprechtag.sprechtag.domain.Pseudonymisierung;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Die Einzelschritte des Anonymisierungs-Laufs, jeder in seiner <b>eigenen</b> Transaktion
 * ({@link Propagation#REQUIRES_NEW}) und jeder an genau einem Aggregat — aufgerufen aus
 * {@code AnonymisierenService}. So bleibt es bei „eine Transaktion, ein Aggregat" (ADR 0005): Ein
 * Sprechtag mit hundert belegten Terminen sind hundert Transaktionen und eine für den Vermerk.
 */
@RequiredArgsConstructor
@Service
class AnonymisierungsSchrittService {

  private final Termine termine;
  private final Sprechtage sprechtage;

  /** Ersetzt die Familien aller Buchungen eines Termins. Ein zweites Mal überschreibt nur erneut. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void anonymisiereTermin(TerminId id, Pseudonymisierung pseudonyme, LocalDateTime jetzt) {
    Termin termin =
        termine
            .lade(id)
            .orElseThrow(() -> new IllegalStateException("Termin nicht gefunden: " + id.wert()));
    if (termin.anonymisiere(pseudonyme, jetzt)) {
      termine.speichere(termin);
    }
  }

  /**
   * Setzt den Vermerk am Sprechtag — erst, wenn alle seine Termine durch sind.
   *
   * @return ob der Vermerk gesetzt wurde; {@code false}, wenn ein anderer Lauf schneller war
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  boolean vermerkeAnonymisierung(SprechtagId id, Aufbewahrungsfrist frist, LocalDateTime jetzt) {
    Sprechtag sprechtag =
        sprechtage
            .lade(id)
            .orElseThrow(() -> new IllegalStateException("Sprechtag nicht gefunden: " + id.wert()));
    if (!sprechtag.vermerkeAnonymisierung(frist, jetzt)) {
      return false;
    }
    sprechtage.speichere(sprechtag);
    return true;
  }
}
