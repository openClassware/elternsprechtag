package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.domain.BuchungenBestaetigt;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Verbindet die Buchungs-Naht mit dem Versand: reagiert auf das Vorgangs-Ereignis
 * {@link BuchungenBestaetigt} erst <em>nach Commit</em> ({@link TransactionPhase#AFTER_COMMIT}) und
 * ruft — {@link Async} in einem eigenen Thread — {@link BuchungBestaetigungService#bestaetige} auf.
 * So ist die Buchung festgeschrieben, bevor der Versand beginnt (er kann sie nie zurückrollen), und
 * die Eltern-UI kehrt sofort zurück, ohne auf den Mailserver zu warten.
 *
 * <p>Das Ereignis ist bewusst das <em>gebündelte</em>: Eine Familie mit vier Terminen bekommt eine
 * Mail, nicht vier. Die feinkörnigen {@code BuchungAngelegt} der Aggregate erreichen diese Stelle
 * nie — sie werden im Use Case abgeholt und zusammengefasst.
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
  void onBuchungenBestaetigt(BuchungenBestaetigt ereignis) {
    service.bestaetige(ereignis.buchungen());
  }
}
