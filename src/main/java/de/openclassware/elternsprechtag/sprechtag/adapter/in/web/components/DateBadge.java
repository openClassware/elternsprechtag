package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import java.time.LocalDate;

@CssImport("./styles/components/date-badge.css")
public class DateBadge extends Div {

  public DateBadge(LocalDate date) {
    addClassName("date-badge");
    add(createDayOfMonth(date), createMonth(date));
  }

  private Component createDayOfMonth(LocalDate date) {
    Div dayOfMonth = new Div();
    dayOfMonth.addClassName("date-badge__day-of-month");
    dayOfMonth.setText(date.getDayOfMonth() + "");
    return dayOfMonth;
  }

  private Component createMonth(LocalDate date) {
    Div month = new Div();
    month.addClassName("date-badge__month");
    month.setText(Formats.monthShort(date));
    return month;
  }
}
