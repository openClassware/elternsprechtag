package de.openclassware.elternsprechtag.ui.component;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

@CssImport("./styles/card.css")
public class Card extends Div {

  public Card(VaadinIcon icon, String heading, String description) {
    addClassName("card");
    add(createIcon(icon), createHeading(heading), createDescription(description));
  }

  private Icon createIcon(VaadinIcon vaadinIcon) {
    Icon icon = new Icon(vaadinIcon);
    icon.addClassName("card__icon");
    return icon;
  }

  private Div createHeading(String text) {
    Div heading = new Div();
    heading.addClassName("card__header");
    heading.setText(text);
    return heading;
  }

  private Div createDescription(String text) {
    Div description = new Div();
    description.addClassName("card__description");
    description.setText(text);
    return description;
  }
}
