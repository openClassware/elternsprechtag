package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;

public class ShareLinkDialog extends Dialog {

  public ShareLinkDialog(String link) {
    setHeaderTitle(getTranslation("manage-sprechtag.share.title"));

    TextField field = new TextField();
    field.setValue(link);
    field.setReadOnly(true);
    field.setWidthFull();
    add(field);

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
