package de.openclassware.elternsprechtag.sprechtag.application.port.out;

import de.openclassware.elternsprechtag.sprechtag.domain.Zustellung;
import java.util.List;

/**
 * Aggregat-Repository der {@link Zustellung}: der Schreibweg für den Ausgang einer Nachricht. Gelesen
 * wird über {@link BuchungsAnsichten}, zusammen mit den Angaben der Buchung.
 */
public interface Zustellungen {

  /**
   * Schreibt die Zustellungen. Eine vorhandene Zeile derselben Buchung und Art wird ersetzt — eine
   * neue Nachricht überschreibt den Ausgang der vorigen.
   */
  void speichere(List<Zustellung> zustellungen);
}
