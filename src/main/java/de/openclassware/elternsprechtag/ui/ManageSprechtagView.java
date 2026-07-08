package de.openclassware.elternsprechtag.ui;

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
import de.openclassware.elternsprechtag.ui.components.ShareLinkDialog;
import de.openclassware.elternsprechtag.ui.components.SprechtagFilterBar;
import de.openclassware.elternsprechtag.ui.components.SprechtagTable;
import de.openclassware.elternsprechtag.ui.layouts.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Route(value = ManageSprechtagView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/manage-sprechtag-view.css")
public class ManageSprechtagView extends Div {

  public static final String ROUTE = "sprechtage";

  private final ManageSprechtagPresenter presenter;
  private final SprechtagTable table;
  private final TextField search;
  private final Div filterRow;

  private List<Sprechtag> sprechtage;
  private SprechtagFilterBar filterBar;
  private SprechtagStatusEnum statusFilter;
  private String searchQuery = "";

  public ManageSprechtagView(ManageSprechtagPresenter presenter) {
    this.presenter = presenter;
    this.sprechtage = presenter.findAllSprechtage();
    addClassName("manage-sprechtag-view");
    this.table =
        new SprechtagTable(sprechtage, this::onStatusChange, this::onDuplicate, this::onShare);
    this.search = createSearch();
    this.filterBar = new SprechtagFilterBar(sprechtage, statusFilter, this::onStatusSelected);
    this.filterRow = createFilterRow();
    add(createBreadcrumb(), createHeader(), filterRow, table);
  }

  private Breadcrumb createBreadcrumb() {
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

  private Button createNewButton() {
    Button newButton = new Button(getTranslation("manage-sprechtag.new-button"));
    newButton.addClassName("manage-sprechtag-view__new-button");
    newButton.setIcon(VaadinIcon.PLUS.create());
    newButton.addThemeVariants(ButtonVariant.PRIMARY);
    newButton.addClickListener(_ -> getUI().ifPresent(ui -> ui.navigate(EditSprechtagView.ROUTE)));
    return newButton;
  }

  private Div createFilterRow() {
    Div row = new Div();
    row.addClassName("manage-sprechtag-view__filter-row");
    row.add(filterBar, search);
    return row;
  }

  private TextField createSearch() {
    TextField field = new TextField();
    field.addClassName("manage-sprechtag-view__search");
    field.setPlaceholder(getTranslation("manage-sprechtag.search.placeholder"));
    field.setPrefixComponent(VaadinIcon.SEARCH.create());
    field.setClearButtonVisible(true);
    field.setValueChangeMode(ValueChangeMode.EAGER);
    field.addValueChangeListener(event -> onSearch(event.getValue()));
    return field;
  }

  private void onStatusSelected(SprechtagStatusEnum status) {
    this.statusFilter = status;
    applyFilters();
  }

  private void onSearch(String query) {
    this.searchQuery = query == null ? "" : query.trim().toLowerCase(Locale.GERMANY);
    applyFilters();
  }

  private void onStatusChange(Sprechtag sprechtag, SprechtagStatusEnum newStatus) {
    presenter.changeStatus(sprechtag.getId(), newStatus);
    reload();
  }

  private void onDuplicate(Sprechtag sprechtag) {
    UUID copyId = presenter.duplicate(sprechtag.getId());
    getUI().ifPresent(ui -> ui.navigate(EditSprechtagView.ROUTE + "/" + copyId));
  }

  private void onShare(Sprechtag sprechtag) {
    getUI()
        .ifPresent(
            ui ->
                ui.getPage()
                    .executeJs("return window.location.origin")
                    .then(
                        String.class,
                        origin ->
                            new ShareLinkDialog(
                                    origin
                                        + "/"
                                        + ElternsprechtagView.ROUTE
                                        + "/"
                                        + sprechtag.getAccessToken())
                                .open()));
  }

  private void reload() {
    this.sprechtage = presenter.findAllSprechtage();
    SprechtagFilterBar newFilterBar =
        new SprechtagFilterBar(sprechtage, statusFilter, this::onStatusSelected);
    filterRow.replace(filterBar, newFilterBar);
    filterBar = newFilterBar;
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
