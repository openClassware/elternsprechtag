package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/** Legende der Slot-Farben (frei/belegt) neben der Schrittüberschrift des Terminschritts. */
public class SlotLegende extends Div {

  public SlotLegende() {
    addClassName("elternsprechtag-view__legend");
    add(
        item("frei", "elternsprechtag.termin.legend.frei"),
        item("belegt", "elternsprechtag.termin.legend.belegt"));
  }

  private Div item(String modifier, String translationKey) {
    Div item = new Div();
    item.addClassName("elternsprechtag-view__legend-item");
    Span swatch = new Span();
    swatch.addClassName("elternsprechtag-view__legend-swatch");
    swatch.addClassName("elternsprechtag-view__legend-swatch--" + modifier);
    item.add(swatch, new Span(getTranslation(translationKey)));
    return item;
  }
}
