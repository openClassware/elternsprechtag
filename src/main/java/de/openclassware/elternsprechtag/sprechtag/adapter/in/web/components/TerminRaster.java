package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.SlotOption;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Terminraster der aufgeklappten Lehrkraft samt Notizfeld; sitzt in der Liste direkt hinter ihrer
 * Karte. Der Zustand je Slot (frei/belegt/gewählt/Konflikt) kommt vom Aufrufer — der Warenkorb,
 * der ihn kennt, bleibt Vaadin-frei und darf hier nicht importiert werden.
 */
public class TerminRaster extends Div {

  /** Zustand eines Slots aus Sicht der aktuellen Auswahl. */
  public enum SlotZustand {
    FREI,
    BELEGT,
    GEWAEHLT,
    KONFLIKT
  }

  public TerminRaster(
      List<SlotOption> slots,
      Function<SlotOption, SlotZustand> zustandOf,
      Consumer<SlotOption> onSelect,
      Runnable onDeselect,
      String notiz,
      boolean notizEnabled,
      Consumer<String> onNotizChange) {
    addClassName("elternsprechtag-view__slot-panel");

    if (slots.isEmpty()) {
      Div placeholder = new Div();
      placeholder.addClassName("elternsprechtag-view__placeholder");
      placeholder.setText(getTranslation("elternsprechtag.termin.empty"));
      add(placeholder);
      return;
    }

    Div grid = new Div();
    grid.addClassName("elternsprechtag-view__slot-grid");
    slots.forEach(slot -> grid.add(createSlot(slot, zustandOf.apply(slot), onSelect, onDeselect)));
    add(grid, new NotizFeld(notiz, notizEnabled, onNotizChange));
  }

  private Component createSlot(
      SlotOption slot, SlotZustand zustand, Consumer<SlotOption> onSelect, Runnable onDeselect) {
    Div slotEl = new Div();
    slotEl.addClassName("elternsprechtag-view__slot");
    slotEl.add(new Span(Formats.time(slot.zeit())));

    switch (zustand) {
      case BELEGT -> slotEl.addClassName("elternsprechtag-view__slot--belegt");
      case KONFLIKT -> slotEl.addClassName("elternsprechtag-view__slot--konflikt");
      case GEWAEHLT -> {
        slotEl.addClassName("elternsprechtag-view__slot--selected");
        Span check = new Span();
        check.addClassName("elternsprechtag-view__slot-check");
        check.add(VaadinIcon.CHECK.create());
        slotEl.add(check);
        slotEl.addClickListener(event -> onDeselect.run());
      }
      case FREI -> slotEl.addClickListener(event -> onSelect.accept(slot));
    }
    return slotEl;
  }
}
