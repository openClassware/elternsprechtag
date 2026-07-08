package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

/** Numbered section header: an accent-coloured number badge next to a title. */
@CssImport("./styles/components/step-header.css")
public class StepHeader extends Div {

  public StepHeader(int number, String title) {
    this(number, title, null);
  }

  public StepHeader(int number, String title, String suffix) {
    addClassName("step-header");

    Span numberBadge = new Span(String.valueOf(number));
    numberBadge.addClassName("step-header__number");

    H2 titleElement = new H2(title);
    titleElement.addClassName("step-header__title");

    add(numberBadge, titleElement);

    if (suffix != null && !suffix.isBlank()) {
      Span suffixElement = new Span(suffix);
      suffixElement.addClassName("step-header__suffix");
      add(suffixElement);
    }
  }
}
