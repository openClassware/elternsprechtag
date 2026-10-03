package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;

/**
 * Bestätigungsdialog vor dem Löschen eines Entwurfs (#132). Löschen ist der einzige Menüeintrag,
 * der sich nicht umkehren lässt — ein Fehlklick direkt neben „Duplizieren" soll ihn nicht auslösen.
 * Reine Anzeige: Ob der Sprechtag noch ein Entwurf ist, entscheidet das Aggregat nach dem Klick.
 */
@CssImport("./styles/components/delete-sprechtag-dialog.css")
public class DeleteSprechtagDialog extends Dialog {

  public DeleteSprechtagDialog(String titel, Runnable onConfirm) {
    setHeaderTitle(getTranslation("manage-sprechtag.delete.title"));
    addClassName("delete-sprechtag-dialog");

    Div message = new Div();
    message.addClassName("delete-sprechtag-dialog__message");
    message.setText(getTranslation("manage-sprechtag.delete.message", titel));
    add(message);

    Button abort = new Button(getTranslation("manage-sprechtag.delete.abort"), _ -> close());
    abort.addThemeVariants(ButtonVariant.TERTIARY);

    Button confirm =
        new Button(
            getTranslation("manage-sprechtag.delete.confirm"),
            _ -> {
              onConfirm.run();
              close();
            });
    confirm.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);

    getFooter().add(abort, confirm);
  }
}
