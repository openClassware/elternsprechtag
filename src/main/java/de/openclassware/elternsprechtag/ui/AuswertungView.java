package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsZeile;
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftPlan;
import de.openclassware.elternsprechtag.services.BuchungService.SprechtagAuswertung;
import de.openclassware.elternsprechtag.ui.components.Breadcrumb;
import de.openclassware.elternsprechtag.ui.layouts.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Organizer-Auswertung eines Sprechtags: Terminplan je Lehrkraft. Bewusst dumm — Lade- und
 * Filter-Entscheidungen liegen im {@link AuswertungPresenter}; die View hält nur die aktuelle
 * Filterauswahl (Per-View-Zustand) und rendert das gelieferte Read-Model.
 */
@Route(value = AuswertungView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/auswertung-view.css")
public class AuswertungView extends Div implements HasUrlParameter<String> {

  public static final String ROUTE = "auswertung";

  private final AuswertungPresenter presenter;

  private final H2 headerTitle = new H2();
  private final Div headerMeta = new Div();
  private final ComboBox<LehrkraftPlan> lehrkraftFilter = new ComboBox<>();
  private final Div sections = new Div();

  private List<LehrkraftPlan> allePlaene = List.of();

  AuswertungView(AuswertungPresenter presenter) {
    this.presenter = presenter;
    addClassName("auswertung");
    headerTitle.addClassName("auswertung__title");
    headerMeta.addClassName("auswertung__meta");
    sections.addClassName("auswertung__sections");
    add(createBreadcrumb(), createHeader(), createFilter(), sections);
  }

  @Override
  public void setParameter(BeforeEvent event, String sprechtagId) {
    Optional<SprechtagAuswertung> auswertung =
        parseId(sprechtagId).flatMap(presenter::werteAus);
    if (auswertung.isEmpty()) {
      event.rerouteToError(NotFoundException.class, "Sprechtag not found: " + sprechtagId);
      return;
    }
    render(auswertung.get());
  }

  private void render(SprechtagAuswertung auswertung) {
    headerTitle.setText(auswertung.titel());
    headerMeta.removeAll();
    headerMeta.add(new Span(Formats.dateLong(auswertung.datum())));

    allePlaene = auswertung.plaene();
    lehrkraftFilter.setItems(allePlaene);
    lehrkraftFilter.clear();
    renderSections(allePlaene);
  }

  private void onFilterChange(LehrkraftPlan selected) {
    UUID lehrerId = selected == null ? null : selected.lehrerId();
    renderSections(presenter.filter(allePlaene, lehrerId));
  }

  private void renderSections(List<LehrkraftPlan> plaene) {
    sections.removeAll();
    plaene.stream().map(this::createSection).forEach(sections::add);
  }

  private Breadcrumb createBreadcrumb() {
    Breadcrumb breadcrumb = new Breadcrumb();
    breadcrumb.addClassName("auswertung__breadcrumb");
    breadcrumb.addLink(getTranslation("breadcrumb.uebersicht"), OrganizerView.class);
    breadcrumb.addLink(
        getTranslation("manage-sprechtag.header.title"), ManageSprechtagView.class);
    breadcrumb.addCurrent(getTranslation("auswertung.breadcrumb.title"));
    return breadcrumb;
  }

  private Component createHeader() {
    Div header = new Div();
    header.addClassName("auswertung__header");
    Span eyebrow = new Span(getTranslation("auswertung.header.title"));
    eyebrow.addClassName("auswertung__eyebrow");
    header.add(eyebrow, headerTitle, headerMeta);
    return header;
  }

  private Component createFilter() {
    Div filterRow = new Div();
    filterRow.addClassName("auswertung__filter");
    lehrkraftFilter.addClassName("auswertung__filter-select");
    lehrkraftFilter.setLabel(getTranslation("auswertung.filter.label"));
    lehrkraftFilter.setPlaceholder(getTranslation("auswertung.filter.alle"));
    lehrkraftFilter.setClearButtonVisible(true);
    lehrkraftFilter.setItemLabelGenerator(LehrkraftPlan::anzeigeName);
    lehrkraftFilter.addValueChangeListener(event -> onFilterChange(event.getValue()));
    filterRow.add(lehrkraftFilter);
    return filterRow;
  }

  private Component createSection(LehrkraftPlan plan) {
    Div section = new Div();
    section.addClassName("auswertung__section");
    section.add(createSectionHead(plan), createSectionBody(plan));
    return section;
  }

  private Component createSectionHead(LehrkraftPlan plan) {
    Div head = new Div();
    head.addClassName("auswertung__section-head");

    Span name = new Span(plan.anzeigeName());
    name.addClassName("auswertung__section-name");

    Span kuerzel = new Span(plan.kuerzel());
    kuerzel.addClassName("auswertung__section-kuerzel");

    Span count = new Span(countLabel(plan.anzahl()));
    count.addClassName("auswertung__section-count");

    head.add(name, kuerzel, count);
    return head;
  }

  private String countLabel(int anzahl) {
    if (anzahl == 0) {
      return getTranslation("auswertung.section.empty");
    }
    return anzahl == 1
        ? getTranslation("auswertung.section.count.one")
        : getTranslation("auswertung.section.count.other", anzahl);
  }

  private Component createSectionBody(LehrkraftPlan plan) {
    if (plan.zeilen().isEmpty()) {
      Div empty = new Div();
      empty.addClassName("auswertung__section-empty");
      empty.setText(getTranslation("auswertung.section.empty"));
      return empty;
    }

    Div table = new Div();
    table.addClassName("auswertung__table");
    table.add(createTableHead());
    plan.zeilen().stream().map(this::createRow).forEach(table::add);
    return table;
  }

  private Component createTableHead() {
    Div head = new Div();
    head.addClassName("auswertung__row");
    head.addClassName("auswertung__row--head");
    head.add(
        headCell("auswertung.table.zeit"),
        headCell("auswertung.table.schueler"),
        headCell("auswertung.table.klasse"),
        headCell("auswertung.table.fach"),
        headCell("auswertung.table.eltern"),
        headCell("auswertung.table.notiz"));
    return head;
  }

  private Component headCell(String translationKey) {
    Span cell = new Span(getTranslation(translationKey));
    cell.addClassName("auswertung__head-cell");
    return cell;
  }

  private Component createRow(BuchungsZeile zeile) {
    Div row = new Div();
    row.addClassName("auswertung__row");
    row.add(
        cell(Formats.time(zeile.startzeit()), "auswertung__cell--zeit"),
        cell(zeile.schuelerName(), "auswertung__cell--schueler"),
        cell(zeile.klasse(), "auswertung__cell--klasse"),
        cell(zeile.fach(), "auswertung__cell--fach"),
        cell(zeile.elternName(), "auswertung__cell--eltern"),
        cell(zeile.notiz() == null ? "" : zeile.notiz(), "auswertung__cell--notiz"));
    return row;
  }

  private Component cell(String text, String modifier) {
    Span cell = new Span(text);
    cell.addClassName("auswertung__cell");
    cell.addClassName(modifier);
    return cell;
  }

  private Optional<UUID> parseId(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
