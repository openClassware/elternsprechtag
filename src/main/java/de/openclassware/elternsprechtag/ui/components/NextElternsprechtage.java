package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagRow;
import java.util.Comparator;
import java.util.List;

@CssImport("./styles/components/next-elternsprechtag.css")
public class NextElternsprechtage extends Div {

  public NextElternsprechtage(List<SprechtagRow> sprechtage) {
    addClassName("next-elternsprechtag");
    sprechtage.stream()
        .sorted(Comparator.comparing(SprechtagRow::startDate).reversed())
        .map(SprechtagCard::new)
        .forEach(this::add);
  }
}
