package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen.SlotOption;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Dialog vor dem Umbuchen einer Buchung: benennt Schüler/in und Lehrkraft und bietet die freien
 * Slots derselben Lehrkraft zur Wahl — nach dem Muster von {@link StornoBuchungDialog}. Reine
 * Anzeige-Komponente: fertige Werte herein, die gewählte Termin-Id bei Bestätigung heraus, keine
 * Entscheidung liegt hier.
 */
@CssImport("./styles/components/umbuchen-dialog.css")
public class UmbuchenDialog extends Dialog {

  public UmbuchenDialog(
      String schuelerName, String lehrkraft, List<SlotOption> optionen, Consumer<UUID> onConfirm) {
    setHeaderTitle(getTranslation("auswertung.umbuchen.title"));
    addClassName("umbuchen-dialog");

    Div message = new Div();
    message.addClassName("umbuchen-dialog__message");
    message.setText(getTranslation("auswertung.umbuchen.message", schuelerName, lehrkraft));
    add(message);

    Button confirm = new Button(getTranslation("auswertung.umbuchen.confirm"));
    confirm.addThemeVariants(ButtonVariant.PRIMARY);

    if (optionen.isEmpty()) {
      Div empty = new Div();
      empty.addClassName("umbuchen-dialog__empty");
      empty.setText(getTranslation("auswertung.umbuchen.keine-slots"));
      add(empty);
      confirm.setEnabled(false);
    } else {
      ComboBox<SlotOption> auswahl = new ComboBox<>();
      auswahl.addClassName("umbuchen-dialog__auswahl");
      auswahl.setLabel(getTranslation("auswertung.umbuchen.slot-label"));
      auswahl.setPlaceholder(getTranslation("auswertung.umbuchen.slot-placeholder"));
      auswahl.setItems(optionen);
      auswahl.setItemLabelGenerator(option -> Formats.time(option.zeit()));
      auswahl.addValueChangeListener(
          event -> confirm.setEnabled(event.getValue() != null));
      confirm.setEnabled(false);
      add(auswahl);

      confirm.addClickListener(
          _ -> {
            SlotOption gewaehlt = auswahl.getValue();
            if (gewaehlt == null) {
              return;
            }
            onConfirm.accept(gewaehlt.terminId());
            close();
          });
    }

    Button abort = new Button(getTranslation("auswertung.umbuchen.abort"), _ -> close());
    abort.addThemeVariants(ButtonVariant.TERTIARY);

    getFooter().add(abort, confirm);
  }
}
