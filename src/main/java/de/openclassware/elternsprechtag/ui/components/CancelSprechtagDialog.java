package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;

/**
 * Bestätigungsdialog vor der Absage eines Sprechtags. Nennt den Titel des Sprechtags und die Anzahl
 * der betroffenen Eltern (aktive Buchungen, je E-Mail-Adresse einmal) und ruft bei Bestätigung den
 * übergebenen Callback auf. Bleibt eine reine Anzeige-Komponente: Zähl-/Entscheidungslogik liegt im
 * Presenter/Service, hier kommen nur die fertigen Werte an.
 */
@CssImport("./styles/components/cancel-sprechtag-dialog.css")
public class CancelSprechtagDialog extends Dialog {

  public CancelSprechtagDialog(String titel, long betroffene, Runnable onConfirm) {
    setHeaderTitle(getTranslation("manage-sprechtag.cancel.title"));
    addClassName("cancel-sprechtag-dialog");

    Div message = new Div();
    message.addClassName("cancel-sprechtag-dialog__message");
    message.setText(getTranslation("manage-sprechtag.cancel.message", titel));

    Div affected = new Div();
    affected.addClassName("cancel-sprechtag-dialog__affected");
    affected.setText(getTranslation("manage-sprechtag.cancel.affected", betroffene));

    add(message, affected);

    Button abort = new Button(getTranslation("manage-sprechtag.cancel.abort"), _ -> close());
    abort.addThemeVariants(ButtonVariant.TERTIARY);

    Button confirm =
        new Button(
            getTranslation("manage-sprechtag.cancel.confirm"),
            _ -> {
              onConfirm.run();
              close();
            });
    confirm.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);

    getFooter().add(abort, confirm);
  }
}
