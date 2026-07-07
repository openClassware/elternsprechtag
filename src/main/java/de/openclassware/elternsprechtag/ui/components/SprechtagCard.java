package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.ui.EditSprechtagView;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@CssImport("./styles/components/sprechtag-card.css")
public class SprechtagCard extends Div {

  public SprechtagCard(Sprechtag sprechtag) {
    addClassName("sprechtag-card");

    Div top = new Div();
    top.addClassName("sprechtag-card__top");
    top.add(createDate(sprechtag), createTitle(sprechtag));

    Div bottom = new Div();
    bottom.addClassName("sprechtag-card__bottom");
    bottom.add(createTimespan(sprechtag));

    add(top, bottom);

    addClickListener(_ -> navigateToSprechtagEditView(sprechtag));
  }

  private void navigateToSprechtagEditView(Sprechtag sprechtag) {
    getUI().ifPresent(ui -> ui.navigate(EditSprechtagView.ROUTE + "/" + sprechtag.getId()));
  }

  private Component createTimespan(Sprechtag sprechtag) {
    Div timespan = new Div();
    timespan.addClassName("sprechtag-card__timespan");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    timespan.add(
        VaadinIcon.CLOCK.create(),
        new Span(
            sprechtag.getStartTime().format(formatter)
                + " - "
                + sprechtag.getEndTime().format(formatter)));
    return timespan;
  }

  private Component createTitle(Sprechtag sprechtag) {
    Div title = new Div();
    title.addClassName("sprechtag-card__title");
    title.setText(sprechtag.getTitel());
    Div year = new Div();
    year.addClassName("sprechtag-card__year");
    year.setText(sprechtag.getStartDate().getYear() + "");
    Div div = new Div();
    div.add(title, year);
    return div;
  }

  private Component createDate(Sprechtag sprechtag) {
    Div dayOfMonth = new Div();
    dayOfMonth.setText(sprechtag.getStartDate().getDayOfMonth() + "");
    dayOfMonth.addClassName("sprechtag-card__day-of-month");
    Div month = new Div();
    month.addClassName("sprechtag-card__month");
    month.setText(
        sprechtag.getStartDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.GERMANY));
    Div div = new Div();
    div.addClassName("sprechtag-card__date");
    div.add(dayOfMonth, month);
    return div;
  }
}
