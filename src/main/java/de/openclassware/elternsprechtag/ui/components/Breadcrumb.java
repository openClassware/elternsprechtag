package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;

@CssImport("./styles/components/breadcrumb.css")
public class Breadcrumb extends Div {

  private Span current;

  public Breadcrumb() {
    addClassName("breadcrumb");
  }

  public Breadcrumb addLink(String text, Class<? extends Component> navigationTarget) {
    addSeparatorIfNeeded();
    RouterLink link = new RouterLink(text, navigationTarget);
    link.addClassName("breadcrumb__link");
    add(link);
    return this;
  }

  public Breadcrumb addCurrent(String text) {
    addSeparatorIfNeeded();
    current = new Span(text);
    current.addClassName("breadcrumb__current");
    add(current);
    return this;
  }

  public void setCurrentText(String text) {
    if (current != null) {
      current.setText(text);
    }
  }

  private void addSeparatorIfNeeded() {
    if (getElement().getChildCount() == 0) {
      return;
    }
    Span separator = new Span("/");
    separator.addClassName("breadcrumb__separator");
    add(separator);
  }
}
