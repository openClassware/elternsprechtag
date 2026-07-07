package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.ui.components.Breadcrumb;
import de.openclassware.elternsprechtag.ui.components.SprechtagFilterBar;
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

  private SprechtagStatusEnum statusFilter;
  private String searchQuery = "";

  public ManageSprechtagView(ManageSprechtagPresenter presenter) {
    this.sprechtage = presenter.findAllSprechtage();
    addClassName("manage-sprechtag-view");
    this.table = new SprechtagTable(sprechtage);
    add(createBreadcrumb(), createHeader(), createFilterRow(), table);
  }

  private Component createBreadcrumb() {
    Breadcrumb breadcrumb = new Breadcrumb();
    breadcrumb.addClassName("manage-sprechtag-view__breadcrumb");
    breadcrumb.addLink(getTranslation("breadcrumb.uebersicht"), OrganizerView.class);
    breadcrumb.addCurrent(getTranslation("manage-sprechtag.header.title"));
    return breadcrumb;
  }

  private Div createHeader() {
    Div header = new Div();
    header.addClassName("manage-sprechtag-view__header");
    header.add(new H2(getTranslation("manage-sprechtag.header.title")), createNewButton());
    return header;
  }

  private Component createNewButton() {
    Button newButton = new Button(getTranslation("manage-sprechtag.new-button"));
    newButton.addClassName("manage-sprechtag-view__new-button");
    newButton.setIcon(VaadinIcon.PLUS.create());
    newButton.addThemeVariants(ButtonVariant.PRIMARY);
    newButton.addClickListener(
        _ -> getUI().ifPresent(ui -> ui.navigate(EditSprechtagView.ROUTE)));
    return newButton;
  }

  private Div createFilterRow() {
    Div filterRow = new Div();
    filterRow.addClassName("manage-sprechtag-view__filter-row");
    filterRow.add(new SprechtagFilterBar(sprechtage, this::onStatusSelected), createSearch());
    return filterRow;
  }

  private Component createSearch() {
    TextField search = new TextField();
    search.addClassName("manage-sprechtag-view__search");
    search.setPlaceholder(getTranslation("manage-sprechtag.search.placeholder"));
    search.setPrefixComponent(VaadinIcon.SEARCH.create());
    search.setClearButtonVisible(true);
    search.setValueChangeMode(ValueChangeMode.EAGER);
    search.addValueChangeListener(event -> onSearch(event.getValue()));
    return search;
  }

  private void onStatusSelected(SprechtagStatusEnum status) {
    this.statusFilter = status;
    applyFilters();
  }

  private void onSearch(String query) {
    this.searchQuery = query == null ? "" : query.trim().toLowerCase(Locale.GERMANY);
    applyFilters();
  }

  private void applyFilters() {
    List<Sprechtag> filtered =
        sprechtage.stream()
            .filter(sprechtag -> statusFilter == null || sprechtag.getStatus() == statusFilter)
            .filter(
                sprechtag ->
                    sprechtag.getTitel().toLowerCase(Locale.GERMANY).contains(searchQuery))
            .toList();
    table.setSprechtage(filtered);
  }
}
