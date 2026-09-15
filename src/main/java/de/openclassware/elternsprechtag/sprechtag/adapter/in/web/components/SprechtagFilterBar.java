package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht.SprechtagZeile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@CssImport("./styles/components/sprechtag-filter.css")
public class SprechtagFilterBar extends Div {

  private record Option(String labelKey, SprechtagStatus status) {}

  private static final List<Option> OPTIONS =
      List.of(
          new Option("manage-sprechtag.filter.alle", null),
          new Option("manage-sprechtag.filter.aktiv", SprechtagStatus.VEROEFFENTLICHT),
          new Option("manage-sprechtag.filter.entwuerfe", SprechtagStatus.ENTWURF),
          new Option("manage-sprechtag.filter.abgeschlossen", SprechtagStatus.ABGESCHLOSSEN),
          new Option("manage-sprechtag.filter.abgesagt", SprechtagStatus.ABGESAGT));

  /**
   * @param activeStatus the status whose tab is initially active, or {@code null} for "Alle".
   * @param onSelect receives the selected status to filter by, or {@code null} for "Alle".
   */
  public SprechtagFilterBar(
      List<SprechtagZeile> sprechtage,
      SprechtagStatus activeStatus,
      Consumer<SprechtagStatus> onSelect) {
    addClassName("sprechtag-filter");

    List<Div> tabs = new ArrayList<>();
    for (Option option : OPTIONS) {
      Div tab = createTab(option, sprechtage);
      if (option.status() == activeStatus) {
        tab.addClassName("sprechtag-filter__tab--active");
      }
      tab.addClickListener(
          _ -> {
            tabs.forEach(other -> other.removeClassName("sprechtag-filter__tab--active"));
            tab.addClassName("sprechtag-filter__tab--active");
            onSelect.accept(option.status());
          });
      tabs.add(tab);
      add(tab);
    }
  }

  private Div createTab(Option option, List<SprechtagZeile> sprechtage) {
    Div tab = new Div();
    tab.addClassName("sprechtag-filter__tab");

    Span label = new Span(getTranslation(option.labelKey()));
    label.addClassName("sprechtag-filter__label");

    Span count = new Span(String.valueOf(count(option, sprechtage)));
    count.addClassName("sprechtag-filter__count");

    tab.add(label, count);
    return tab;
  }

  private long count(Option option, List<SprechtagZeile> sprechtage) {
    if (option.status() == null) {
      return sprechtage.size();
    }
    return sprechtage.stream().filter(sprechtag -> sprechtag.status() == option.status()).count();
  }
}
