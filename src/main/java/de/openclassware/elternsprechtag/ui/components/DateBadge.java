package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import java.time.format.TextStyle;
import java.util.Locale;

@CssImport("./styles/components/date-badge.css")
public class DateBadge extends Div {

  public DateBadge(Sprechtag sprechtag) {
    addClassName("date-badge");
    add(createDayOfMonth(sprechtag), createMonth(sprechtag));
  }

  private Component createDayOfMonth(Sprechtag sprechtag) {
    Div dayOfMonth = new Div();
    dayOfMonth.addClassName("date-badge__day-of-month");
    dayOfMonth.setText(sprechtag.getStartDate().getDayOfMonth() + "");
    return dayOfMonth;
  }

  private Component createMonth(Sprechtag sprechtag) {
    Div month = new Div();
    month.addClassName("date-badge__month");
    month.setText(
        sprechtag.getStartDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.GERMANY));
    return month;
  }
}
