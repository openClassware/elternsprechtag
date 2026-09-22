package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungErinnert;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.Ereignis;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Markiert genau eine Buchung als erinnert — in ihrer <b>eigenen</b> Transaktion
 * ({@link Propagation#REQUIRES_NEW}), aufgerufen aus dem Scheduler-Lauf in {@code ErinnernService}.
 *
 * <p>Der Grund für die eigene Transaktion: Ein Lauf erinnert unter Umständen hunderte Buchungen in
 * einer Schleife. Ohne diese Trennung teilten sie sich eine Datenbank-Transaktion, und ein
 * einzelner echter SQL-Fehler (Deadlock, abgebrochene Verbindung) markierte diese als
 * rollback-only — jede bereits markierte Buchung desselben Laufs ginge beim Commit verloren, still
 * und ohne Fehler außerhalb der Logdatei. Mit {@code REQUIRES_NEW} committet jede Buchung für sich;
 * ein Fehlschlag bei einer Familie lässt die übrigen unberührt.
 */
@RequiredArgsConstructor
@Service
class ErinnerungsMarkierungService {

  private final Termine termine;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  Optional<BuchungId> markiere(BuchungId buchungId, LocalDateTime jetzt) {
    Termin termin =
        termine
            .ladeZuBuchung(buchungId)
            .orElseThrow(() -> new IllegalStateException("Buchung ohne Termin: " + buchungId.wert()));
    if (!termin.erinnereBuchung(buchungId, jetzt)) {
      // Zwischen Lesen des Read-Modells und Laden des Aggregats storniert, oder ein anderer Lauf
      // war schneller — kein Fehler.
      return Optional.empty();
    }
    termine.speichere(termin);
    for (Ereignis ereignis : termin.ereignisseAbholen()) {
      if (ereignis instanceof BuchungErinnert erinnert) {
        return Optional.of(erinnert.buchung());
      }
    }
    return Optional.empty();
  }
}
