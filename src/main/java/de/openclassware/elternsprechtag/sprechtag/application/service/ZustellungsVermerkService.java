package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen.Versand;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Zustellungen;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.Mailart;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellung;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hält den Ausgang eines Versands fest — in einer <b>eigenen</b> Transaktion
 * ({@link Propagation#REQUIRES_NEW}), aufgerufen aus {@link BenachrichtigenService}.
 *
 * <p>Der Grund für die eigene Transaktion: Der Versand hängt an
 * {@code @TransactionalEventListener(AFTER_COMMIT)}. Läuft er ausnahmsweise im Thread des Vorgangs
 * (ohne {@code @Async}, wie in den Tests), ist dessen Transaktion zwar committet, aber noch gebunden;
 * ein bloßes Mitmachen schriebe in sie hinein und ginge still verloren.
 *
 * <p>Ein Zeitpunkt für den ganzen Versand: Alle Buchungen einer Nachricht teilen ihn, und daran
 * erkennt die Leseseite, was eine Nachricht war.
 */
@RequiredArgsConstructor
@Service
class ZustellungsVermerkService {

  private final Zustellungen zustellungen;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void vermerke(Mailart art, List<Versand> versand) {
    LocalDateTime jetzt = LocalDateTime.now();
    List<Zustellung> vermerke = new ArrayList<>();
    for (Versand nachricht : versand) {
      for (BuchungId buchung : nachricht.buchungen()) {
        vermerke.add(Zustellung.vermerke(buchung, art, nachricht.ergebnis(), jetzt));
      }
    }
    zustellungen.speichere(vermerke);
  }
}
