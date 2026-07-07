package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@CssImport("./styles/components/sprechtag-filter.css")
public class SprechtagFilterBar extends Div {

  private record Option(String labelKey, SprechtagStatusEnum status) {}

  private static final List<Option> OPTIONS =
      List.of(
          new Option("manage-sprechtag.filter.alle", null),
          new Option("manage-sprechtag.filter.aktiv", SprechtagStatusEnum.VEROEFFENTLICHT),
          new Option("manage-sprechtag.filter.entwuerfe", SprechtagStatusEnum.ENTWURF),
          new Option("manage-sprechtag.filter.abgeschlossen", SprechtagStatusEnum.ABGESCHLOSSEN));

  /**
   * @param onSelect receives the selected status to filter by, or {@code null} for "Alle".
   */
  public SprechtagFilterBar(List<Sprechtag> sprechtage, Consumer<SprechtagStatusEnum> onSelect) {
    addClassName("sprechtag-filter");

    List<Div> tabs = new ArrayList<>();
    for (Option option : OPTIONS) {
      Div tab = createTab(option, sprechtage);
      tab.addClickListener(
          _ -> {
            tabs.forEach(other -> other.removeClassName("sprechtag-filter__tab--active"));
            tab.addClassName("sprechtag-filter__tab--active");
            onSelect.accept(option.status());
          });
      tabs.add(tab);
      add(tab);
    }
    tabs.getFirst().addClassName("sprechtag-filter__tab--active");
  }

  private Div createTab(Option option, List<Sprechtag> sprechtage) {
    Div tab = new Div();
    tab.addClassName("sprechtag-filter__tab");

    Span label = new Span(getTranslation(option.labelKey()));
    label.addClassName("sprechtag-filter__label");

    Span count = new Span(String.valueOf(count(option, sprechtage)));
    count.addClassName("sprechtag-filter__count");

    tab.add(label, count);
    return tab;
  }

  private long count(Option option, List<Sprechtag> sprechtage) {
    if (option.status() == null) {
      return sprechtage.size();
    }
    return sprechtage.stream().filter(sprechtag -> sprechtag.getStatus() == option.status()).count();
  }
}
