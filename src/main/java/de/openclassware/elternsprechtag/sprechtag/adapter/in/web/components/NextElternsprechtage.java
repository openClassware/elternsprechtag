package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht.SprechtagZeile;
import java.util.Comparator;
import java.util.List;

@CssImport("./styles/components/next-elternsprechtag.css")
public class NextElternsprechtage extends Div {

  public NextElternsprechtage(List<SprechtagZeile> sprechtage) {
    addClassName("next-elternsprechtag");
    sprechtage.stream()
        .sorted(Comparator.comparing(SprechtagZeile::datum).reversed())
        .map(SprechtagCard::new)
        .forEach(this::add);
  }
}
