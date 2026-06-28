package de.openclassware.elternsprechtag.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

@CssImport("./styles/no-next-elternsprechtage.css")
public class NoNextElternsprechtage extends Div {

  public NoNextElternsprechtage() {
    addClassName("no-next-elternsprechtage");
    add(createIcon(), createTitel(), createDescription(), createButton());
  }

  private Button createButton() {
    Button button = new Button();
    button.setIcon(VaadinIcon.PLUS.create());
    button.setText("Ersten Elternsprechtag anlegen");
    button.setThemeVariants(ButtonVariant.PRIMARY);
    return button;
  }

  private Div createDescription() {
    Div description = new Div();
    description.setText(
        "Legen Sie Ihren ersten Elternsprechtag an - Datum und Zeitfenster sind in wenigen Schritten festgelegt");
    description.addClassName("no-next-elternsprechtage__description");
    return description;
  }

  private Div createTitel() {
    Div titel = new Div();
    titel.setText("Noch keine Elternsprechtage");
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
