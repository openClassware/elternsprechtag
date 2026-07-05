package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import java.util.List;

@CssImport("./styles/components/next-elternsprechtag.css")
public class NextElternsprechtage extends Div {

  public NextElternsprechtage(List<Sprechtag> sprechtage) {
    addClassName("next-elternsprechtag");
    sprechtage.stream().map(SprechtagCard::new).forEach(this::add);
  }
}
