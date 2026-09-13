package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import java.util.UUID;

/**
 * Use Case: die Termine eines veröffentlichten Sprechtags herstellen — je teilnehmender Lehrkraft
 * ein Satz Slots.
 *
 * <p>Ein eigener Use Case und kein Seitenzweig des Veröffentlichens: Hier treffen zwei Aggregate und
 * die Schulorganisation aufeinander. Und er ist <b>idempotent</b> — ein zweiter Aufruf erzeugt
 * nichts Neues; nur deshalb schadet es nicht, dass mehrere Schreibpfade dort landen.
 */
public interface Materialisieren {

  /**
   * @return die Zahl der neu erzeugten Termine; {@code 0}, wenn schon materialisiert war oder keine
   *     der teilnehmenden Klassen einen Lehrauftrag hat
   */
  int materialisiere(UUID sprechtagId);
}
