package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.AccessToken;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.util.Optional;

/**
 * Aggregat-Repository des Sprechtags: lädt und speichert ganze Aggregate — der Schreibweg in die
 * Datenbank. Read-Modelle gehen den anderen Weg, über {@link SprechtagAnsichten}.
 */
public interface Sprechtage {

  Optional<Sprechtag> lade(SprechtagId id);

  /**
   * Der Sprechtag hinter einem Elternlink. Gehört hierher und nicht zu den Ansichten: Der Zugang
   * entscheidet, ob überhaupt gebucht werden darf, und diese Frage wird am Aggregat beantwortet.
   */
  Optional<Sprechtag> ladeNachAccessToken(AccessToken token);

  /**
   * Schreibt das ganze Aggregat. Gibt nichts zurück: Der Aufrufer behält das Aggregat in der Hand,
   * das er verändert hat — samt seiner noch nicht abgeholten Ereignisse.
   *
   * <p><b>Ein Aggregat ist nach dem Speichern verbraucht</b> (wie bei {@link Termine}): Der Adapter
   * hebt die Version in der Datenbank, das Aggregat trägt sie unveränderlich. Wer weiterarbeiten
   * will, lädt neu.
   *
   * @throws org.springframework.dao.OptimisticLockingFailureException wenn der Sprechtag seit dem
   *     Laden verändert wurde
   */
  void speichere(Sprechtag sprechtag);
}
