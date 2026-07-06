package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.ui.EditSprechtagView;

@CssImport("./styles/components/no-next-elternsprechtage.css")
public class NoNextElternsprechtage extends Div {

  public NoNextElternsprechtage() {
    addClassName("no-next-elternsprechtage");
    add(createIcon(), createTitel(), createDescription(), createButton());
  }

  private Button createButton() {
    Button button = new Button();
    button.setIcon(VaadinIcon.PLUS.create());
    button.setText(getTranslation("no-next.button"));
    button.setThemeVariants(ButtonVariant.PRIMARY);
    button.addClickListener(_ -> button.getUI().ifPresent(this::navigateToEditSprechtag));
    return button;
  }

  private void navigateToEditSprechtag(UI ui) {
    ui.navigate(EditSprechtagView.ROUTE);
  }

  private Div createDescription() {
    Div description = new Div();
    description.setText(getTranslation("no-next.description"));
    description.addClassName("no-next-elternsprechtage__description");
    return description;
  }

  private Div createTitel() {
    Div titel = new Div();
    titel.setText(getTranslation("no-next.title"));
    titel.addClassName("no-next-elternsprechtage__title");
    return titel;
  }

  private Icon createIcon() {
    Icon icon = new Icon();
    icon.setIcon(VaadinIcon.CALENDAR);
    icon.addClassName("no-next-elternsprechtage__icon");
    return icon;
  }
}
