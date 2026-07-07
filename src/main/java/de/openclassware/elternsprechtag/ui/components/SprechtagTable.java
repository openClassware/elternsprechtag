package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.ui.EditSprechtagView;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@CssImport("./styles/components/sprechtag-table.css")
public class SprechtagTable extends Div {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final Div body = new Div();

  public SprechtagTable(List<Sprechtag> sprechtage) {
    addClassName("sprechtag-table");
    body.addClassName("sprechtag-table__body");
    add(createHead(), body);
    setSprechtage(sprechtage);
  }

  public void setSprechtage(List<Sprechtag> sprechtage) {
    body.removeAll();
    if (sprechtage.isEmpty()) {
      body.add(createEmptyState());
      return;
    }
    sprechtage.stream().map(this::createRow).forEach(body::add);
  }

  private Component createHead() {
    Div head = new Div();
    head.addClassName("sprechtag-table__head");
    head.add(
        headCell("manage-sprechtag.table.datum"),
        headCell("manage-sprechtag.table.titel"),
        headCell("manage-sprechtag.table.zeit"),
        headCell("manage-sprechtag.table.klassen"),
        headCell("manage-sprechtag.table.status"),
        new Span());
    return head;
  }

  private Component headCell(String translationKey) {
    Span cell = new Span(getTranslation(translationKey));
    cell.addClassName("sprechtag-table__head-cell");
    return cell;
  }

  private Component createEmptyState() {
    Div empty = new Div();
    empty.addClassName("sprechtag-table__empty");

    H3 title = new H3(getTranslation("manage-sprechtag.empty.title"));
    title.addClassName("sprechtag-table__empty-title");

    Div description = new Div();
    description.addClassName("sprechtag-table__empty-description");
    description.setText(getTranslation("manage-sprechtag.empty.description"));

    empty.add(title, description);
    return empty;
  }

  private Component createRow(Sprechtag sprechtag) {
    Div row = new Div();
    row.addClassName("sprechtag-table__row");
    row.add(
        new DateBadge(sprechtag),
        createTitle(sprechtag),
        createTime(sprechtag),
        createKlassen(sprechtag),
        new StatusBadge(sprechtag.getStatus()),
        createMenu(sprechtag));
    return row;
  }

  private Component createTitle(Sprechtag sprechtag) {
    Div title = new Div();
    title.addClassName("sprechtag-table__title-cell");

    Div name = new Div();
    name.addClassName("sprechtag-table__title");
    name.setText(sprechtag.getTitel());
    title.add(name);

    if (sprechtag.getLocation() != null && !sprechtag.getLocation().isBlank()) {
      Div location = new Div();
      location.addClassName("sprechtag-table__location");
      location.add(VaadinIcon.MAP_MARKER.create(), new Span(sprechtag.getLocation()));
      title.add(location);
    }

    return title;
  }

  private Component createTime(Sprechtag sprechtag) {
    Div time = new Div();
    time.addClassName("sprechtag-table__time");
    time.add(
        VaadinIcon.CLOCK.create(),
        new Span(
            sprechtag.getStartTime().format(TIME_FORMATTER)
                + "–"
                + sprechtag.getEndTime().format(TIME_FORMATTER)));
    return time;
  }

  private Component createKlassen(Sprechtag sprechtag) {
    Span klassen = new Span();
    klassen.addClassName("sprechtag-table__klassen");
    klassen.setText(
        sprechtag.getKlassen().stream().map(Klasse::getName).collect(Collectors.joining(", ")));
    return klassen;
  }

  private Component createMenu(Sprechtag sprechtag) {
    Div menu = new Div();
    menu.addClassName("sprechtag-table__menu");
    menu.add(VaadinIcon.ELLIPSIS_DOTS_V.create());

    ContextMenu contextMenu = new ContextMenu(menu);
    contextMenu.setOpenOnClick(true);
    contextMenu.addItem(
        createMenuItemContent(VaadinIcon.EDIT, "manage-sprechtag.menu.edit"),
        event -> navigateToEdit(sprechtag));

    return menu;
  }

  private Component createMenuItemContent(VaadinIcon icon, String translationKey) {
    Div content = new Div();
    content.addClassName("sprechtag-table__menu-item");
    content.add(icon.create(), new Span(getTranslation(translationKey)));
    return content;
  }

  private void navigateToEdit(Sprechtag sprechtag) {
    getUI().ifPresent(ui -> ui.navigate(EditSprechtagView.ROUTE + "/" + sprechtag.getId()));
  }
}
