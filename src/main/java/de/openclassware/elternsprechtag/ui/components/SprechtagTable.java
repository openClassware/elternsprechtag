package de.openclassware.elternsprechtag.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagRow;
import de.openclassware.elternsprechtag.ui.EditSprechtagView;
import de.openclassware.elternsprechtag.ui.Formats;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@CssImport("./styles/components/sprechtag-table.css")
public class SprechtagTable extends Div {

  /** Display order of the status actions offered in the row menu. */
  private static final List<SprechtagStatusEnum> TRANSITION_ORDER =
      List.of(
          SprechtagStatusEnum.VEROEFFENTLICHT,
          SprechtagStatusEnum.ABGESCHLOSSEN,
          SprechtagStatusEnum.ABGESAGT);

  private final Div body = new Div();
  private final BiConsumer<SprechtagRow, SprechtagStatusEnum> onStatusChange;
  private final Consumer<SprechtagRow> onDuplicate;
  private final Consumer<SprechtagRow> onShare;

  public SprechtagTable(
      List<SprechtagRow> sprechtage,
      BiConsumer<SprechtagRow, SprechtagStatusEnum> onStatusChange,
      Consumer<SprechtagRow> onDuplicate,
      Consumer<SprechtagRow> onShare) {
    this.onStatusChange = onStatusChange;
    this.onDuplicate = onDuplicate;
    this.onShare = onShare;
    addClassName("sprechtag-table");
    body.addClassName("sprechtag-table__body");
    add(createHead(), body);
    setSprechtage(sprechtage);
  }

  public void setSprechtage(List<SprechtagRow> sprechtage) {
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

  private Component createRow(SprechtagRow sprechtag) {
    Div row = new Div();
    row.addClassName("sprechtag-table__row");
    row.add(
        new DateBadge(sprechtag.startDate()),
        createTitle(sprechtag),
        createTime(sprechtag),
        createKlassen(sprechtag),
        new StatusBadge(sprechtag.status()),
        createMenu(sprechtag));
    return row;
  }

  private Component createTitle(SprechtagRow sprechtag) {
    Div title = new Div();
    title.addClassName("sprechtag-table__title-cell");

    Div name = new Div();
    name.addClassName("sprechtag-table__title");
    name.setText(sprechtag.titel());
    title.add(name);

    if (sprechtag.location() != null && !sprechtag.location().isBlank()) {
      Div location = new Div();
      location.addClassName("sprechtag-table__location");
      location.add(VaadinIcon.MAP_MARKER.create(), new Span(sprechtag.location()));
      title.add(location);
    }

    return title;
  }

  private Component createTime(SprechtagRow sprechtag) {
    Div time = new Div();
    time.addClassName("sprechtag-table__time");
    time.add(
        VaadinIcon.CLOCK.create(),
        new Span(Formats.time(sprechtag.startTime()) + "–" + Formats.time(sprechtag.endTime())));
    return time;
  }

  private Component createKlassen(SprechtagRow sprechtag) {
    Span klassen = new Span();
    klassen.addClassName("sprechtag-table__klassen");
    klassen.setText(String.join(", ", sprechtag.klassen()));
    return klassen;
  }

  private Component createMenu(SprechtagRow sprechtag) {
    Div menu = new Div();
    menu.addClassName("sprechtag-table__menu");
    menu.add(VaadinIcon.ELLIPSIS_DOTS_V.create());

    ContextMenu contextMenu = new ContextMenu(menu);
    contextMenu.setOpenOnClick(true);
    contextMenu.addItem(
        createMenuItemContent(VaadinIcon.EDIT, "manage-sprechtag.menu.edit"),
        event -> navigateToEdit(sprechtag));
    contextMenu.addItem(
        createMenuItemContent(VaadinIcon.COPY, "manage-sprechtag.menu.duplicate"),
        event -> onDuplicate.accept(sprechtag));
    if (sprechtag.status() == SprechtagStatusEnum.VEROEFFENTLICHT) {
      contextMenu.addItem(
          createMenuItemContent(VaadinIcon.LINK, "manage-sprechtag.menu.share"),
          event -> onShare.accept(sprechtag));
    }

    for (SprechtagStatusEnum target : TRANSITION_ORDER) {
      if (sprechtag.status().allowedTransitions().contains(target)) {
        contextMenu.addItem(
            createMenuItemContent(iconFor(target), labelKeyFor(target)),
            event -> onStatusChange.accept(sprechtag, target));
      }
    }

    return menu;
  }

  private VaadinIcon iconFor(SprechtagStatusEnum target) {
    return switch (target) {
      case VEROEFFENTLICHT -> VaadinIcon.PLAY;
      case ABGESCHLOSSEN -> VaadinIcon.CHECK;
      case ABGESAGT -> VaadinIcon.BAN;
      case ENTWURF -> VaadinIcon.PENCIL;
    };
  }

  private String labelKeyFor(SprechtagStatusEnum target) {
    return switch (target) {
      case VEROEFFENTLICHT -> "manage-sprechtag.menu.activate";
      case ABGESCHLOSSEN -> "manage-sprechtag.menu.complete";
      case ABGESAGT -> "manage-sprechtag.menu.cancel";
      case ENTWURF -> "manage-sprechtag.menu.edit";
    };
  }

  private Component createMenuItemContent(VaadinIcon icon, String translationKey) {
    Div content = new Div();
    content.addClassName("sprechtag-table__menu-item");
    content.add(icon.create(), new Span(getTranslation(translationKey)));
    return content;
  }

  private void navigateToEdit(SprechtagRow sprechtag) {
    getUI().ifPresent(ui -> ui.navigate(EditSprechtagView.ROUTE + "/" + sprechtag.id()));
  }
}
