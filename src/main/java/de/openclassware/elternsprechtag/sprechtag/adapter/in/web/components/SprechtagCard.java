package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht.SprechtagZeile;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.EditSprechtagView;

@CssImport("./styles/components/sprechtag-card.css")
public class SprechtagCard extends Div {

  public SprechtagCard(SprechtagZeile sprechtag) {
    addClassName("sprechtag-card");

    Div top = new Div();
    top.addClassName("sprechtag-card__top");
    top.add(new DateBadge(sprechtag.datum()), createTitle(sprechtag));

    Div bottom = new Div();
    bottom.addClassName("sprechtag-card__bottom");
    bottom.add(createTimespan(sprechtag), new StatusBadge(sprechtag.status()));

    add(top, bottom);

    addClickListener(_ -> navigateToSprechtagEditView(sprechtag));
  }

  private void navigateToSprechtagEditView(SprechtagZeile sprechtag) {
    getUI().ifPresent(ui -> ui.navigate(EditSprechtagView.ROUTE + "/" + sprechtag.id()));
  }

  private Component createTimespan(SprechtagZeile sprechtag) {
    Div timespan = new Div();
    timespan.addClassName("sprechtag-card__timespan");
    timespan.add(
        VaadinIcon.CLOCK.create(),
        new Span(Formats.time(sprechtag.beginn()) + " - " + Formats.time(sprechtag.ende())));
    return timespan;
  }

  private Component createTitle(SprechtagZeile sprechtag) {
    Div title = new Div();
    title.addClassName("sprechtag-card__title");
    title.setText(sprechtag.titel());
    Div year = new Div();
    year.addClassName("sprechtag-card__year");
    year.setText(sprechtag.datum().getYear() + "");
    Div div = new Div();
    div.add(title, year);
    return div;
  }
}
