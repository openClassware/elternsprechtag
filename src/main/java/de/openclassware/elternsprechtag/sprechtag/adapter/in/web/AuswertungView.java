package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.BuchungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen.SlotOption;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.SprechtagMeldungen.Meldung;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.Breadcrumb;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.StornoBuchungDialog;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.UmbuchenDialog;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts.MainLayout;
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
  private final Button headerNachtragenButton = new Button();
  private final ComboBox<LehrkraftPlan> lehrkraftFilter = new ComboBox<>();
  private final Div sections = new Div();

  private List<LehrkraftPlan> allePlaene = List.of();
  private UUID sprechtagId;
  private UUID filterLehrkraft;
  private boolean stornoMoeglich;
  private boolean nachtragenMoeglich;

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
    Optional<UUID> id = parseId(sprechtagId);
    Optional<SprechtagAuswertung> auswertung = id.flatMap(presenter::werteAus);
    if (auswertung.isEmpty()) {
      event.rerouteToError(NotFoundException.class, "Sprechtag not found: " + sprechtagId);
      return;
    }
    // Vaadin benutzt dieselbe View-Instanz wieder, wenn nur der URL-Parameter wechselt. Die
    // Filterauswahl soll ein Storno überleben, aber nicht den Sprechtag — sonst stünde die
    // Auswertung des nächsten Sprechtags stillschweigend gefiltert da.
    if (!id.get().equals(sprechtagId)) {
      filterLehrkraft = null;
    }
    this.sprechtagId = id.get();
    render(auswertung.get());
  }

  private void render(SprechtagAuswertung auswertung) {
    headerTitle.setText(auswertung.titel());
    headerMeta.removeAll();
    headerMeta.add(new Span(Formats.dateLong(auswertung.datum())));

    allePlaene = auswertung.plaene();
    stornoMoeglich = presenter.darfStornieren(auswertung);
    nachtragenMoeglich = presenter.darfNachtragen(auswertung);
    headerNachtragenButton.setVisible(nachtragenMoeglich);
    // Die Filterauswahl ist Per-View-Zustand und soll ein Storno überleben: gemerkt, die Items
    // getauscht, dieselbe Lehrkraft wieder gesetzt. Steht sie nicht mehr im Plan, bleibt „alle".
    UUID gewaehlt = filterLehrkraft;
    lehrkraftFilter.setItems(allePlaene);
    lehrkraftFilter.setValue(planMit(gewaehlt));
    renderSections(presenter.filter(allePlaene, filterLehrkraft));
  }

  /** Lädt die Auswertung neu — nach einem Storno ist der bisherige Stand überholt. */
  private void reload() {
    presenter.werteAus(sprechtagId).ifPresent(this::render);
  }

  private LehrkraftPlan planMit(UUID lehrerId) {
    if (lehrerId == null) {
      return null;
    }
    return allePlaene.stream()
        .filter(plan -> plan.lehrerId().equals(lehrerId))
        .findFirst()
        .orElse(null);
  }

  private void onFilterChange(LehrkraftPlan selected) {
    filterLehrkraft = selected == null ? null : selected.lehrerId();
    renderSections(presenter.filter(allePlaene, filterLehrkraft));
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

    Div titleRow = new Div();
    titleRow.addClassName("auswertung__title-row");
    titleRow.add(headerTitle, createNachtragenButton());

    header.add(eyebrow, titleRow, headerMeta);
    return header;
  }

  /** Einstieg ins Nachtragen — nur an einem veröffentlichten Sprechtag sichtbar. */
  private Button createNachtragenButton() {
    headerNachtragenButton.setText(getTranslation("auswertung.nachtragen.button"));
    headerNachtragenButton.addClassName("auswertung__nachtragen-button");
    headerNachtragenButton.setIcon(VaadinIcon.PLUS.create());
    headerNachtragenButton.addThemeVariants(ButtonVariant.PRIMARY);
    headerNachtragenButton.setVisible(false);
    headerNachtragenButton.addClickListener(
        event ->
            getUI().ifPresent(ui -> ui.navigate(NachtragenView.ROUTE + "/" + sprechtagId)));
    return headerNachtragenButton;
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
    if (stornoMoeglich) {
      table.addClassName("auswertung__table--mit-aktion");
    }
    table.add(createTableHead());
    plan.zeilen().stream().map(zeile -> createRow(plan, zeile)).forEach(table::add);
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
    if (stornoMoeglich) {
      head.add(headCell("auswertung.table.aktion"));
    }
    return head;
  }

  private Component headCell(String translationKey) {
    Span cell = new Span(getTranslation(translationKey));
    cell.addClassName("auswertung__head-cell");
    return cell;
  }

  private Component createRow(LehrkraftPlan plan, BuchungsZeile zeile) {
    Div row = new Div();
    row.addClassName("auswertung__row");
    row.add(
        cell(Formats.time(zeile.startzeit()), "auswertung__cell--zeit"),
        cell(zeile.schuelerName(), "auswertung__cell--schueler"),
        cell(zeile.klasse(), "auswertung__cell--klasse"),
        cell(zeile.fach(), "auswertung__cell--fach"),
        cell(zeile.elternName(), "auswertung__cell--eltern"),
        cell(zeile.notiz() == null ? "" : zeile.notiz(), "auswertung__cell--notiz"));
    if (stornoMoeglich) {
      row.add(createStornoAktion(plan, zeile));
    }
    return row;
  }

  private Component createStornoAktion(LehrkraftPlan plan, BuchungsZeile zeile) {
    Div aktion = new Div();
    aktion.addClassName("auswertung__cell");
    aktion.addClassName("auswertung__cell--aktion");

    Button umbuchen = new Button(VaadinIcon.EXCHANGE.create());
    umbuchen.addClassName("auswertung__umbuchen");
    umbuchen.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
    umbuchen.setAriaLabel(getTranslation("auswertung.umbuchen.button"));
    umbuchen.setTooltipText(getTranslation("auswertung.umbuchen.button"));
    umbuchen.addClickListener(_ -> openUmbuchenDialog(plan, zeile));

    Button storno = new Button(VaadinIcon.CLOSE_SMALL.create());
    storno.addClassName("auswertung__storno");
    storno.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR, ButtonVariant.SMALL);
    storno.setAriaLabel(getTranslation("auswertung.storno.button"));
    storno.setTooltipText(getTranslation("auswertung.storno.button"));
    storno.addClickListener(_ -> openStornoDialog(plan, zeile));

    aktion.add(umbuchen, storno);
    return aktion;
  }

  private void openStornoDialog(LehrkraftPlan plan, BuchungsZeile zeile) {
    new StornoBuchungDialog(
            zeile.schuelerName(),
            Formats.time(zeile.startzeit()),
            plan.anzeigeName(),
            () -> storniere(zeile))
        .open();
  }

  private void storniere(BuchungsZeile zeile) {
    Optional<Meldung> weigerung = presenter.storniere(zeile.buchungId());
    if (weigerung.isPresent()) {
      SprechtagMeldungen.zeige(this, weigerung.get());
    } else {
      Notification.show(getTranslation("auswertung.storno.erfolg", zeile.schuelerName()));
    }
    reload();
  }

  private void openUmbuchenDialog(LehrkraftPlan plan, BuchungsZeile zeile) {
    // Die Auswertung ist ein Read-Modell und darf veraltet sein: Zwischen dem Rendern der Zeile
    // und dem Klick kann die Buchung anderswo storniert oder umgebucht worden sein. Derselbe Fang
    // wie bei storniere()/umbuche() — sonst crashte der Klick statt einer Meldung.
    List<SlotOption> optionen;
    try {
      optionen = presenter.freieSlots(zeile.buchungId());
    } catch (RuntimeException fehler) {
      SprechtagMeldungen.zeige(this, SprechtagMeldungen.zu(fehler));
      reload();
      return;
    }
    new UmbuchenDialog(
            zeile.schuelerName(),
            plan.anzeigeName(),
            optionen,
            neuerTerminId -> umbuche(zeile, neuerTerminId))
        .open();
  }

  private void umbuche(BuchungsZeile zeile, UUID neuerTerminId) {
    Optional<Meldung> weigerung = presenter.umbuche(zeile.buchungId(), neuerTerminId);
    if (weigerung.isPresent()) {
      SprechtagMeldungen.zeige(this, weigerung.get());
    } else {
      Notification.show(getTranslation("auswertung.umbuchen.erfolg", zeile.schuelerName()));
    }
    reload();
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
