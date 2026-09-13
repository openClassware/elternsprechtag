package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht.SprechtagZeile;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.AuswertungView;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.EditSprechtagView;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@CssImport("./styles/components/sprechtag-table.css")
public class SprechtagTable extends Div {

  /**
   * Display order of the status actions offered in the row menu. Angeboten wird nur, was
   * {@link SprechtagStatus#erlaubteUebergaenge()} hergibt — und selbst das nur als Vorschlag: Ob der
   * Weg wirklich gangbar ist, entscheidet das Aggregat beim Klick.
   */
  private static final List<SprechtagStatus> TRANSITION_ORDER =
      List.of(
          SprechtagStatus.VEROEFFENTLICHT,
          SprechtagStatus.ABGESCHLOSSEN,
          SprechtagStatus.ENTWURF,
          SprechtagStatus.ABGESAGT);

  private final Div body = new Div();
  private final BiConsumer<SprechtagZeile, SprechtagStatus> onStatusChange;
  private final Consumer<SprechtagZeile> onDuplicate;
  private final Consumer<SprechtagZeile> onShare;

  public SprechtagTable(
      List<SprechtagZeile> sprechtage,
      BiConsumer<SprechtagZeile, SprechtagStatus> onStatusChange,
      Consumer<SprechtagZeile> onDuplicate,
      Consumer<SprechtagZeile> onShare) {
    this.onStatusChange = onStatusChange;
    this.onDuplicate = onDuplicate;
    this.onShare = onShare;
    addClassName("sprechtag-table");
    body.addClassName("sprechtag-table__body");
    add(createHead(), body);
    setSprechtage(sprechtage);
  }

  public void setSprechtage(List<SprechtagZeile> sprechtage) {
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

  private Component createRow(SprechtagZeile sprechtag) {
    Div row = new Div();
    row.addClassName("sprechtag-table__row");
    row.add(
        new DateBadge(sprechtag.datum()),
        createTitle(sprechtag),
        createTime(sprechtag),
        createKlassen(sprechtag),
        new StatusBadge(sprechtag.status()),
        createMenu(sprechtag));
    return row;
  }

  private Component createTitle(SprechtagZeile sprechtag) {
    Div title = new Div();
    title.addClassName("sprechtag-table__title-cell");

    Div name = new Div();
    name.addClassName("sprechtag-table__title");
    name.setText(sprechtag.titel());
    title.add(name);

    if (sprechtag.ort() != null && !sprechtag.ort().isBlank()) {
      Div location = new Div();
      location.addClassName("sprechtag-table__location");
      location.add(VaadinIcon.MAP_MARKER.create(), new Span(sprechtag.ort()));
      title.add(location);
    }

    return title;
  }

  private Component createTime(SprechtagZeile sprechtag) {
    Div time = new Div();
    time.addClassName("sprechtag-table__time");
    time.add(
        VaadinIcon.CLOCK.create(),
        new Span(Formats.time(sprechtag.beginn()) + "–" + Formats.time(sprechtag.ende())));
    return time;
  }

  private Component createKlassen(SprechtagZeile sprechtag) {
    Span klassen = new Span();
    klassen.addClassName("sprechtag-table__klassen");
    klassen.setText(String.join(", ", sprechtag.klassen()));
    return klassen;
  }

  private Component createMenu(SprechtagZeile sprechtag) {
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
    if (sprechtag.status() == SprechtagStatus.VEROEFFENTLICHT
        || sprechtag.status() == SprechtagStatus.ABGESCHLOSSEN) {
      contextMenu.addItem(
          createMenuItemContent(VaadinIcon.CLIPBOARD_TEXT, "manage-sprechtag.menu.evaluate"),
          event -> navigateToAuswertung(sprechtag));
    }
    if (sprechtag.status() == SprechtagStatus.VEROEFFENTLICHT) {
      contextMenu.addItem(
          createMenuItemContent(VaadinIcon.LINK, "manage-sprechtag.menu.share"),
          event -> onShare.accept(sprechtag));
    }

    for (SprechtagStatus target : TRANSITION_ORDER) {
      if (sprechtag.status().erlaubteUebergaenge().contains(target)) {
        contextMenu.addItem(
            createMenuItemContent(iconFor(target), labelKeyFor(target)),
            event -> onStatusChange.accept(sprechtag, target));
      }
    }

    return menu;
  }

  private VaadinIcon iconFor(SprechtagStatus target) {
    return switch (target) {
      case VEROEFFENTLICHT -> VaadinIcon.PLAY;
      case ABGESCHLOSSEN -> VaadinIcon.CHECK;
      case ABGESAGT -> VaadinIcon.BAN;
      case ENTWURF -> VaadinIcon.PENCIL;
    };
  }

  private String labelKeyFor(SprechtagStatus target) {
    return switch (target) {
      case VEROEFFENTLICHT -> "manage-sprechtag.menu.activate";
      case ABGESCHLOSSEN -> "manage-sprechtag.menu.complete";
      case ABGESAGT -> "manage-sprechtag.menu.cancel";
      case ENTWURF -> "manage-sprechtag.menu.draft";
    };
  }

  private Component createMenuItemContent(VaadinIcon icon, String translationKey) {
    Div content = new Div();
    content.addClassName("sprechtag-table__menu-item");
    content.add(icon.create(), new Span(getTranslation(translationKey)));
    return content;
  }

  private void navigateToEdit(SprechtagZeile sprechtag) {
    getUI().ifPresent(ui -> ui.navigate(EditSprechtagView.ROUTE + "/" + sprechtag.id()));
  }

  private void navigateToAuswertung(SprechtagZeile sprechtag) {
    getUI().ifPresent(ui -> ui.navigate(AuswertungView.ROUTE + "/" + sprechtag.id()));
  }
}
