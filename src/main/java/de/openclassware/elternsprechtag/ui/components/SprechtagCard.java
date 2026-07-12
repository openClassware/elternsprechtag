package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagRow;
import de.openclassware.elternsprechtag.ui.EditSprechtagView;
import de.openclassware.elternsprechtag.ui.Formats;

@CssImport("./styles/components/sprechtag-card.css")
public class SprechtagCard extends Div {

  public SprechtagCard(SprechtagRow sprechtag) {
    addClassName("sprechtag-card");

    Div top = new Div();
    top.addClassName("sprechtag-card__top");
    top.add(new DateBadge(sprechtag.startDate()), createTitle(sprechtag));

    Div bottom = new Div();
    bottom.addClassName("sprechtag-card__bottom");
    bottom.add(createTimespan(sprechtag), new StatusBadge(sprechtag.status()));

    add(top, bottom);

    addClickListener(_ -> navigateToSprechtagEditView(sprechtag));
  }

  private void navigateToSprechtagEditView(SprechtagRow sprechtag) {
    getUI().ifPresent(ui -> ui.navigate(EditSprechtagView.ROUTE + "/" + sprechtag.id()));
  }

  private Component createTimespan(SprechtagRow sprechtag) {
    Div timespan = new Div();
    timespan.addClassName("sprechtag-card__timespan");
    timespan.add(
        VaadinIcon.CLOCK.create(),
        new Span(Formats.time(sprechtag.startTime()) + " - " + Formats.time(sprechtag.endTime())));
    return timespan;
  }

  private Component createTitle(SprechtagRow sprechtag) {
    Div title = new Div();
    title.addClassName("sprechtag-card__title");
    title.setText(sprechtag.titel());
    Div year = new Div();
    year.addClassName("sprechtag-card__year");
    year.setText(sprechtag.startDate().getYear() + "");
    Div div = new Div();
    div.add(title, year);
    return div;
  }
}
