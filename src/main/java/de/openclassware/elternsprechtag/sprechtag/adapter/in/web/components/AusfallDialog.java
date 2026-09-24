package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.SlotZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.SlotZustand;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Dialog der Sammelaktion „Lehrkraft fällt aus" (Issue #156): zeigt alle Slots der Lehrkraft an
 * diesem Sprechtag — frei, gebucht oder bereits entfallen —, lässt eine Teilmenge oder per
 * „alle auswählen" den Ganztagsfall wählen und zeigt vor dem Bestätigen, wie viele Termine und
 * (geschätzt) wie viele Familien betroffen sind.
 *
 * <p>Die Auswahl-Entscheidung selbst liegt im Vaadin-freien {@link AusfallAuswahl}-Modell, das
 * dieser Dialog hält; er rendert nur, was es liefert. Fertige Slots herein, die gewählten
 * Termin-Ids bei Bestätigung heraus.
 */
@CssImport("./styles/components/ausfall-dialog.css")
public class AusfallDialog extends Dialog {

  private final AusfallAuswahl auswahl;
  private final Div rows = new Div();
  private final Span summary = new Span();
  private final Button confirm = new Button();

  public AusfallDialog(
      String lehrkraftName, List<SlotZeile> slots, Consumer<List<UUID>> onConfirm) {
    this.auswahl = new AusfallAuswahl(slots);
    setHeaderTitle(getTranslation("auswertung.ausfall.title"));
    addClassName("ausfall-dialog");

    Div message = new Div();
    message.addClassName("ausfall-dialog__message");
    message.setText(getTranslation("auswertung.ausfall.message", lehrkraftName));
    add(message);

    Button abort = new Button(getTranslation("auswertung.ausfall.abort"), _ -> close());
    abort.addThemeVariants(ButtonVariant.TERTIARY);
    confirm.setText(getTranslation("auswertung.ausfall.confirm"));

    if (slots.isEmpty()) {
      Div empty = new Div();
      empty.addClassName("ausfall-dialog__empty");
      empty.setText(getTranslation("auswertung.ausfall.keine-slots"));
      add(empty);
      confirm.setEnabled(false);
      getFooter().add(abort, confirm);
      return;
    }

    Checkbox selectAll = new Checkbox(getTranslation("auswertung.ausfall.select-all"));
    selectAll.addClassName("ausfall-dialog__select-all");
    selectAll.addValueChangeListener(
        event -> {
          if (Boolean.TRUE.equals(event.getValue())) {
            auswahl.waehleAlle();
          } else {
            auswahl.waehleKeine();
          }
          render();
        });
    add(selectAll);

    rows.addClassName("ausfall-dialog__rows");
    add(rows);

    summary.addClassName("ausfall-dialog__summary");
    add(summary);

    Div warning = new Div();
    warning.addClassName("ausfall-dialog__warning");
    warning.setText(getTranslation("auswertung.ausfall.warning"));
    add(warning);

    render();

    confirm.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
    confirm.addClickListener(
        _ -> {
          onConfirm.accept(auswahl.gewaehlteTerminIds());
          close();
        });

    getFooter().add(abort, confirm);
  }

  private void render() {
    rows.removeAll();
    for (SlotZeile slot : auswahl.slots()) {
      rows.add(createRow(slot));
    }
    summary.setText(
        getTranslation("auswertung.ausfall.summary.termine", auswahl.anzahlTermine())
            + " · "
            + getTranslation("auswertung.ausfall.summary.familien", auswahl.anzahlFamilien()));
    confirm.setEnabled(auswahl.hatAuswahl());
  }

  private Div createRow(SlotZeile slot) {
    Div row = new Div();
    row.addClassName("ausfall-dialog__row");

    Checkbox checkbox = new Checkbox(auswahl.istGewaehlt(slot));
    checkbox.setEnabled(auswahl.istWaehlbar(slot));
    checkbox.addValueChangeListener(
        event -> {
          auswahl.toggle(slot);
          render();
        });

    Span zeit = new Span(Formats.time(slot.zeit()));
    zeit.addClassName("ausfall-dialog__zeit");

    Span zustand = new Span(zustandsText(slot));
    zustand.addClassName("ausfall-dialog__zustand");

    row.add(checkbox, zeit, zustand);
    return row;
  }

  private String zustandsText(SlotZeile slot) {
    if (slot.zustand() == SlotZustand.ENTFALLEN) {
      return getTranslation("auswertung.ausfall.zustand.entfallen");
    }
    if (slot.zustand() == SlotZustand.GEBUCHT) {
      return getTranslation("auswertung.ausfall.zustand.gebucht") + " · " + slot.schuelerName();
    }
    return getTranslation("auswertung.ausfall.zustand.frei");
  }
}
