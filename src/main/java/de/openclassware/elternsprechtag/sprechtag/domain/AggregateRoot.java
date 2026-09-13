package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Gemeinsame Basis der Aggregat-Wurzeln: sie sammeln, was in ihnen geschehen ist, und geben es
 * genau einmal heraus.
 *
 * <p>Das Aggregat veröffentlicht <em>nicht</em> selbst — es kennt keinen Publisher. Der Use Case
 * holt nach dem Speichern ab ({@link #ereignisseAbholen()}), bündelt und übergibt dem
 * Ereignis-Port. Rollt die Transaktion zurück, wird nie gebündelt und nie veröffentlicht.
 */
public abstract class AggregateRoot {

  private final List<Ereignis> ereignisse = new ArrayList<>();

  protected void melde(Ereignis ereignis) {
    ereignisse.add(Objects.requireNonNull(ereignis, "ereignis"));
  }

  /**
   * Gibt die gemeldeten Ereignisse heraus und leert den Puffer — ein zweiter Aufruf liefert nichts.
   * Dass genau einmal abgeholt wird, ist die Zusage, auf der die Bündelung im Use Case beruht:
   * sonst bekäme eine Familie ihre Bestätigung zweimal.
   */
  public List<Ereignis> ereignisseAbholen() {
    List<Ereignis> abgeholt = List.copyOf(ereignisse);
    ereignisse.clear();
    return abgeholt;
  }
}
