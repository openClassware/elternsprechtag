package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.function.Consumer;

/**
 * Notizfeld der aufgeklappten Lehrkraft. Es steht von Anfang an da, bleibt aber ausgegraut, bis
 * ein Termin gewählt ist — ohne Termin gibt es keine Buchung, an der die Notiz hängen könnte, und
 * ausgegraut zeigt das an, statt das Feld erst später aus dem Nichts auftauchen zu lassen.
 *
 * <p>Es schreibt bei jedem Tastendruck über {@code onChange} zurück ins Warenkorb-Modell, damit
 * beim Panel-Wechsel nichts verloren geht.
 */
public class NotizFeld extends TextArea {

  /** Zeichengrenze einer Notiz; deckt sich mit der Spaltenlänge in der Buchung. */
  private static final int MAX_LENGTH = 500;

  public NotizFeld(String value, boolean enabled, Consumer<String> onChange) {
    setLabel(getTranslation("elternsprechtag.notiz.label"));
    addClassName("elternsprechtag-view__notiz-field");
    setPlaceholder(getTranslation("elternsprechtag.notiz.placeholder"));
    setWidthFull();
    setMaxLength(MAX_LENGTH);
    setValue(value);
    setEnabled(enabled);
    setValueChangeMode(ValueChangeMode.EAGER);
    addValueChangeListener(event -> onChange.accept(event.getValue()));
  }
}
