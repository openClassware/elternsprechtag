package de.openclassware.elternsprechtag.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Verbindet die Buchungs-Naht mit dem Versand: reagiert auf {@link BuchungenErstelltEvent} erst
 * <em>nach Commit</em> ({@link TransactionPhase#AFTER_COMMIT}) und ruft — {@link Async} in einem
 * eigenen Thread — {@link BuchungBestaetigungService#bestaetige(java.util.List)} auf. So ist die
 * Buchung festgeschrieben, bevor der Versand beginnt (er kann sie nie zurückrollen), und die
 * Eltern-UI kehrt sofort zurück, ohne auf den Mailserver zu warten.
 *
 * <p>Bewusst als eigene Bean (nicht als Methode im Service) wie beim {@link
 * AbsageBenachrichtigungListener}: Der Aufruf läuft dadurch über den Spring-Proxy — die
 * Async-Ausführung greift also überhaupt erst — und der Service bleibt frei von Event-Verdrahtung.
 */
@Component
@RequiredArgsConstructor
class BuchungBestaetigungListener {

  private final BuchungBestaetigungService service;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onBuchungenErstellt(BuchungenErstelltEvent event) {
    service.bestaetige(event.buchungIds());
  }
}
