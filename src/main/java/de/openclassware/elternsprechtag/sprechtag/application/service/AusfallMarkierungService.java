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
 * Lässt genau einen Termin entfallen — in seiner <b>eigenen</b> Transaktion
 * ({@link Propagation#REQUIRES_NEW}), aufgerufen aus der Sammelaktion in
 * {@code EntfallenLassenService}. Gleicher Grund wie bei {@code ErinnerungsMarkierungService}: Ein
 * Ausfall betrifft unter Umständen ein Dutzend Termine in einer Schleife; ohne eigene Transaktion
 * teilten sie sich eine, und ein einzelner echter Fehlschlag risse bereits festgeschriebene Termine
 * beim Commit wieder zurück.
 */
@RequiredArgsConstructor
@Service
class AusfallMarkierungService {

  private final Termine termine;

  /** Die Buchung, die dabei mit-storniert wurde — leer, wenn der Termin keine aktive Buchung trug. */
  record Ergebnis(Optional<BuchungId> stornierteBuchung) {}

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  Optional<Ergebnis> entfallenLassen(TerminId terminId) {
    Termin termin =
        termine
            .lade(terminId)
            .orElseThrow(() -> new IllegalStateException("Termin nicht gefunden: " + terminId.wert()));
    if (!termin.lassEntfallen()) {
      // Bereits entfallen — kein Fehler, der Aufrufer geht einfach zum nächsten Termin über.
      return Optional.empty();
    }
    termine.speichere(termin);
    // Abgeholt und größtenteils verworfen: Nur die mit-stornierte Buchung wandert weiter, sie
    // entscheidet über den Mailversand. Das begleitende TerminEntfallen benachrichtigt niemanden —
    // es existiert für die Aggregat-Invariante und ihre Tests (TerminTest), nicht für einen
    // Listener. Dasselbe Muster wie in StornierenService: ohne dieses Abholen bliebe die Meldung im
    // Puffer eines Aggregats liegen, das ohnehin verbraucht ist.
    BuchungId stornierteBuchung = null;
    for (Ereignis ereignis : termin.ereignisseAbholen()) {
      if (ereignis instanceof BuchungStorniert storniert) {
        stornierteBuchung = storniert.buchung();
      }
    }
    return Optional.of(new Ergebnis(Optional.ofNullable(stornierteBuchung)));
  }
}
