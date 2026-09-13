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
   * <p><b>Ein Aggregat ist nach dem Speichern verbraucht.</b> Der Adapter hebt die Version in der
   * Datenbank, das Aggregat trägt sie unveränderlich; sein Stand ist damit veraltet. Ein zweites
   * {@code speichere} derselben Instanz meldet deshalb einen Versionskonflikt, auch wenn niemand
   * dazwischengekommen ist. Wer nach dem Speichern weiterarbeiten will, lädt neu.
   *
   * <p>Das ist Absicht und nicht bloß Sparsamkeit: Ein Aggregat, das seine Version selbst
   * fortschreibt, verwischt, wer die Wahrheit über den gespeicherten Stand hält. Der Aufruf ist
   * dadurch unbequemer, aber eindeutig.
   *
   * @throws org.springframework.dao.OptimisticLockingFailureException wenn der Termin seit dem Laden
   *     verändert wurde
   */
  void speichere(Termin termin);

  /** Speichert mehrere frische Aggregate — die Materialisierung beim Veröffentlichen. */
  void speichereAlle(List<Termin> termine);

  /** Ob für diesen Sprechtag schon materialisiert wurde. Macht die Materialisierung idempotent. */
  boolean existierenFuer(SprechtagId sprechtag);

  /**
   * Ob an diesem Sprechtag jemals gebucht wurde — stornierte Buchungen zählen mit: Benachrichtigt
   * wurde trotzdem. Die Antwort entscheidet, ob sich die Veröffentlichung noch zurücknehmen lässt.
   */
  boolean wurdeGebucht(SprechtagId sprechtag);

  /**
   * Verwirft die materialisierten Termine eines Sprechtags — der Gegenzug zur Materialisierung, wenn
   * eine Veröffentlichung zurückgenommen wird.
   *
   * <p>Ausdrücklich nur für Termine ohne Buchung: Ob das zutrifft, entscheidet der Use Case über
   * {@link #wurdeGebucht(SprechtagId)}, bevor er hierher kommt.
   */
  void entferneFuer(SprechtagId sprechtag);
}
