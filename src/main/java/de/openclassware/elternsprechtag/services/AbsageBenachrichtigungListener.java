package de.openclassware.elternsprechtag.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Verbindet die Absage-Naht mit dem Versand: reagiert auf {@link SprechtagAbgesagtEvent} erst
 * <em>nach Commit</em> ({@link TransactionPhase#AFTER_COMMIT}) und ruft — {@link Async} in einem
 * eigenen Thread — {@link AbsageBenachrichtigungService#benachrichtige(java.util.UUID)} auf. So ist
 * die Absage festgeschrieben, bevor der Versand beginnt (er kann sie nie zurückrollen), und die
 * Organizer-UI kehrt sofort zurück, ohne auf den Versand zu warten.
 *
 * <p>Bewusst als eigene Bean (nicht als Methode im Service): Der Aufruf läuft dadurch über den
 * Spring-Proxy, sodass {@code benachrichtige(...)}s eigenes {@code @Transactional} greift.
 */
@Component
@RequiredArgsConstructor
class AbsageBenachrichtigungListener {

  private final AbsageBenachrichtigungService service;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onSprechtagAbgesagt(SprechtagAbgesagtEvent event) {
    service.benachrichtige(event.sprechtagId());
  }
}
