package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.ui.components.SprechtagTable;
import de.openclassware.elternsprechtag.ui.layouts.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.Locale;

@Route(value = ManageSprechtagView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/manage-sprechtag-view.css")
public class ManageSprechtagView extends Div {

  public static final String ROUTE = "sprechtage";

  private final List<Sprechtag> sprechtage;
  private final SprechtagTable table;

  public ManageSprechtagView(ManageSprechtagPresenter presenter) {
    this.sprechtage = presenter.findAllSprechtage();
    addClassName("manage-sprechtag-view");
    this.table = new SprechtagTable(sprechtage);
    add(createHeader(), table);
  }

  private Div createHeader() {
    Div header = new Div();
    header.addClassName("manage-sprechtag-view__header");
    header.add(new H2(getTranslation("manage-sprechtag.header.title")), createSearch());
    return header;
  }

  private Component createSearch() {
    TextField search = new TextField();
    search.addClassName("manage-sprechtag-view__search");
    search.setPlaceholder(getTranslation("manage-sprechtag.search.placeholder"));
    search.setPrefixComponent(VaadinIcon.SEARCH.create());
    search.setClearButtonVisible(true);
    search.setValueChangeMode(ValueChangeMode.EAGER);
    search.addValueChangeListener(event -> filter(event.getValue()));
    return search;
  }

  private void filter(String query) {
    String needle = query == null ? "" : query.trim().toLowerCase(Locale.GERMANY);
    List<Sprechtag> filtered =
        sprechtage.stream()
            .filter(sprechtag -> sprechtag.getTitel().toLowerCase(Locale.GERMANY).contains(needle))
            .toList();
    table.setSprechtage(filtered);
  }
}
