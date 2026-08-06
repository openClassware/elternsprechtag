package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;

/**
 * Zeigt den Zugangs-Link eines Sprechtags zum Weitergeben an die Eltern und legt ihn auf Wunsch in
 * die Zwischenablage. Der Link steht als Text in einem {@link Div} und nicht in einem
 * schreibgeschützten Textfeld: ein {@code input} bricht nie um und schnitt den Link ab — die
 * Messung dazu steht im Stylesheet. Bleibt eine reine Anzeige-Komponente.
 */
@CssImport("./styles/components/share-link-dialog.css")
public class ShareLinkDialog extends Dialog {

  public ShareLinkDialog(String link) {
    setHeaderTitle(getTranslation("manage-sprechtag.share.title"));

    Div linkText = new Div();
    linkText.addClassName("share-link-dialog__link");
    linkText.setText(link);
    add(linkText);

    Button close = new Button(getTranslation("manage-sprechtag.share.close"), _ -> close());
    close.addThemeVariants(ButtonVariant.TERTIARY);

    Button copy = new Button(getTranslation("manage-sprechtag.share.copy"), VaadinIcon.COPY.create());
    copy.addThemeVariants(ButtonVariant.PRIMARY);
    copy.addClickListener(
        _ -> {
          getUI().ifPresent(ui -> ui.getPage().executeJs("navigator.clipboard.writeText($0)", link));
          Notification.show(getTranslation("manage-sprechtag.share.copied"));
        });

    getFooter().add(close, copy);
  }
}
