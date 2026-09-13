package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.util.List;
import java.util.Optional;

/**
 * Aggregat-Repository des Termins: lädt und speichert ganze Aggregate samt ihrer Buchungen — der
 * Schreibweg in die Datenbank. Read-Modelle gehen den anderen Weg, über die Query-Ports.
 */
public interface Termine {

  Optional<Termin> lade(TerminId id);

  /**
   * Schreibt das ganze Aggregat. Gibt nichts zurück: Der Aufrufer behält bewusst das Aggregat in der
   * Hand, das er verändert hat — samt seiner noch nicht abgeholten Ereignisse.
   *
   * @throws org.springframework.dao.OptimisticLockingFailureException wenn der Termin seit dem Laden
   *     verändert wurde
   */
  void speichere(Termin termin);

  /** Speichert mehrere frische Aggregate — die Materialisierung beim Veröffentlichen. */
  void speichereAlle(List<Termin> termine);

  /** Ob für diesen Sprechtag schon materialisiert wurde. Macht die Materialisierung idempotent. */
  boolean existierenFuer(SprechtagId sprechtag);
}
