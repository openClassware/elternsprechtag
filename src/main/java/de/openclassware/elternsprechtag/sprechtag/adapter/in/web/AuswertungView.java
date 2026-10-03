package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.BuchungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Drucken.Datei;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.SlotZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen.SlotOption;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.AuswertungPresenter.AusfallAusgang;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.AuswertungPresenter.Planansicht;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.SprechtagMeldungen.Meldung;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.AngabenEntfernenDialog;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.AusfallDialog;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.Breadcrumb;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.StornoBuchungDialog;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.UmbuchenDialog;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Organizer-Auswertung eines Sprechtags: Terminplan je Lehrkraft. Bewusst dumm — Lade- und
 * Filter-Entscheidungen liegen im {@link AuswertungPresenter}; die View hält nur die aktuelle
 * Filterauswahl und den Suchbegriff (Per-View-Zustand) und rendert das gelieferte Read-Model.
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
  private final Anchor druckLink = new Anchor();
  private final Button druckButton = new Button();
  private final Div anonymisiertHinweis = new Div();
  private final Paragraph anonymisiertText = new Paragraph();
  private final ComboBox<LehrkraftPlan> lehrkraftFilter = new ComboBox<>();
  private final Checkbox stornierteSchalter = new Checkbox();
  private final TextField suchfeld = new TextField();
  private final Div suchhinweis = new Div();
  private final Span suchhinweisText = new Span();
  private final Div sections = new Div();

  private SprechtagAuswertung auswertung;
  private List<LehrkraftPlan> allePlaene = List.of();
  private UUID sprechtagId;
  private UUID filterLehrkraft;
  // Bewusst nicht in der URL: Ein Name als Query-Parameter landete in Browser-Historie und
  // Server-Logs — genau die Datenspur, die die Anonymisierung vermeiden soll.
  private String suchbegriff = "";
  private boolean stornierteAnzeigen;
  private boolean aktionsspalte;
  private boolean nachtragenMoeglich;
  private boolean ausfallMoeglich;

  AuswertungView(AuswertungPresenter presenter) {
    this.presenter = presenter;
    addClassName("auswertung");
    headerTitle.addClassName("auswertung__title");
    headerMeta.addClassName("auswertung__meta");
    sections.addClassName("auswertung__sections");
    add(
        createBreadcrumb(),
        createHeader(),
        createAnonymisiertHinweis(),
        createFilter(),
        createSuchhinweis(),
        sections);
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
    // Verglichen wird mit dem Feld, nicht mit dem gleichnamigen String-Parameter — eine UUID ist
    // nie gleich einem String, die Auswahl fiele sonst bei jeder Navigation.
    if (!id.get().equals(this.sprechtagId)) {
      filterLehrkraft = null;
      stornierteAnzeigen = false;
      suchbegriff = "";
      suchfeld.clear();
    }
    this.sprechtagId = id.get();
    render(auswertung.get());
  }

  private void render(SprechtagAuswertung auswertung) {
    headerTitle.setText(auswertung.titel());
    headerMeta.removeAll();
    headerMeta.add(new Span(Formats.dateLong(auswertung.datum())));

    this.auswertung = auswertung;
    allePlaene = auswertung.plaene();
    aktionsspalte = presenter.hatAktionsspalte(auswertung);
    stornierteSchalter.setVisible(presenter.hatStornierte(auswertung));
    stornierteSchalter.setValue(stornierteAnzeigen);
    nachtragenMoeglich = presenter.darfNachtragen(auswertung);
    ausfallMoeglich = presenter.darfAusfallErfassen(auswertung);
    headerNachtragenButton.setVisible(nachtragenMoeglich);
    druckLink.setVisible(presenter.darfDrucken(auswertung));
    presenter
        .anonymisierungsHinweis(auswertung)
        .ifPresentOrElse(
            datum -> {
              anonymisiertText.setText(
                  getTranslation("auswertung.anonymisiert.text", Formats.dateLong(datum)));
              anonymisiertHinweis.setVisible(true);
            },
            () -> anonymisiertHinweis.setVisible(false));
    // Die Filterauswahl ist Per-View-Zustand und soll ein Storno überleben: gemerkt, die Items
    // getauscht, dieselbe Lehrkraft wieder gesetzt. Steht sie nicht mehr im Plan, bleibt „alle".
    UUID gewaehlt = filterLehrkraft;
    lehrkraftFilter.setItems(allePlaene);
    lehrkraftFilter.setValue(planMit(gewaehlt));
    renderSections();
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
    renderSections();
  }

  private void onStornierteChange(boolean anzeigen) {
    stornierteAnzeigen = anzeigen;
    renderSections();
  }

  private void onSucheChange(String begriff) {
    suchbegriff = begriff;
    renderSections();
  }

  /** Der Hinweis auf ausgeblendete stornierte Treffer schaltet „Stornierte anzeigen" ein. */
  private void zeigeStornierte() {
    stornierteSchalter.setValue(true);
    onStornierteChange(true);
  }

  private void renderSections() {
    Planansicht ansicht =
        presenter.ansicht(allePlaene, filterLehrkraft, suchbegriff, stornierteAnzeigen);
    sections.removeAll();
    ansicht.plaene().stream().map(this::createSection).forEach(sections::add);
    ansicht
        .keinTrefferFuer()
        .ifPresent(begriff -> sections.add(createKeinTreffer(begriff)));
    int verborgen = ansicht.verborgeneStornierte();
    suchhinweis.setVisible(verborgen > 0);
    if (verborgen > 0) {
      suchhinweisText.setText(
          verborgen == 1
              ? getTranslation("auswertung.suche.stornierte.one")
              : getTranslation("auswertung.suche.stornierte.other", verborgen));
    }
  }

  private Component createKeinTreffer(String begriff) {
    Div leer = new Div();
    leer.addClassName("auswertung__leer");
    leer.setText(getTranslation("auswertung.suche.leer", begriff));
    return leer;
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

    Div aktionen = new Div();
    aktionen.addClassName("auswertung__header-aktionen");
    aktionen.add(createDruckLink(), createNachtragenButton());

    Div titleRow = new Div();
    titleRow.addClassName("auswertung__title-row");
    titleRow.add(headerTitle, aktionen);

    header.add(eyebrow, titleRow, headerMeta);
    return header;
  }

  /**
   * Download der Tagespläne (Issue #120): immer ein ZIP mit allen Lehrkräften, unabhängig von
   * Filter und Suche. Die Datei entsteht erst beim Klick, frisch aus der Datenbank. Sichtbar setzt
   * {@link #render(SprechtagAuswertung)}.
   */
  private Anchor createDruckLink() {
    druckLink.addClassName("auswertung__druck-link");
    druckLink.setHref(
        DownloadHandler.fromInputStream(
            event -> {
              // Der Download läuft außerhalb der Sitzungssperre; die Id gehört der UI und wird
              // unter ihrer Sperre gelesen, gedruckt wird danach ohne sie.
              AtomicReference<UUID> id = new AtomicReference<>();
              event.getUI().accessSynchronously(() -> id.set(sprechtagId));
              Optional<Datei> gedruckt = presenter.drucke(id.get());
              if (gedruckt.isEmpty()) {
                return DownloadResponse.error(404);
              }
              Datei datei = gedruckt.get();
              return new DownloadResponse(
                  new ByteArrayInputStream(datei.inhalt()),
                  datei.name(),
                  datei.medientyp(),
                  datei.inhalt().length);
            }),
        AttachmentType.DOWNLOAD);
    druckButton.setText(getTranslation("auswertung.druck.plaene"));
    druckButton.setIcon(VaadinIcon.DOWNLOAD_ALT.create());
    // Der Link trägt Fokus und Klick; der Knopf darin ist nur seine Gestalt.
    druckButton.setTabIndex(-1);
    druckLink.add(druckButton);
    druckLink.setVisible(false);
    return druckLink;
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

  /**
   * Erklärt die Pseudonyme nach Ablauf der Aufbewahrungsfrist (Issue #127). Bewusst als Information
   * gestaltet, nicht als Warnung — es ist der geplante Normalfall. Sichtbar nur, wenn der Presenter
   * ein Datum liefert.
   */
  private Component createAnonymisiertHinweis() {
    anonymisiertHinweis.addClassName("auswertung__anonymisiert");
    anonymisiertHinweis.getElement().setAttribute("role", "note");
    anonymisiertHinweis.setVisible(false);

    Span icon = new Span(VaadinIcon.INFO_CIRCLE_O.create());
    icon.addClassName("auswertung__anonymisiert-icon");

    Div texte = new Div();
    texte.addClassName("auswertung__anonymisiert-texte");
    Span titel = new Span(getTranslation("auswertung.anonymisiert.titel"));
    titel.addClassName("auswertung__anonymisiert-titel");
    anonymisiertText.addClassName("auswertung__anonymisiert-text");
    texte.add(titel, anonymisiertText);

    anonymisiertHinweis.add(icon, texte);
    return anonymisiertHinweis;
  }

  private Component createFilter() {
    Div filterRow = new Div();
    filterRow.addClassName("auswertung__filter");
    // Die Namenssuche (Issue #121) ist der Einstieg der Telefonauskunft und steht deshalb vorn.
    // Gefiltert wird schon beim Tippen, kurz verzögert — der Name wird oft buchstabiert.
    suchfeld.addClassName("auswertung__suche");
    suchfeld.setLabel(getTranslation("auswertung.suche.label"));
    suchfeld.setPlaceholder(getTranslation("auswertung.suche.placeholder"));
    suchfeld.setPrefixComponent(VaadinIcon.SEARCH.create());
    suchfeld.setClearButtonVisible(true);
    suchfeld.setValueChangeMode(ValueChangeMode.LAZY);
    suchfeld.addValueChangeListener(
        event -> {
          if (event.isFromClient()) {
            onSucheChange(event.getValue());
          }
        });
    lehrkraftFilter.addClassName("auswertung__filter-select");
    lehrkraftFilter.setLabel(getTranslation("auswertung.filter.label"));
    lehrkraftFilter.setPlaceholder(getTranslation("auswertung.filter.alle"));
    lehrkraftFilter.setClearButtonVisible(true);
    lehrkraftFilter.setItemLabelGenerator(LehrkraftPlan::anzeigeName);
    lehrkraftFilter.addValueChangeListener(event -> onFilterChange(event.getValue()));
    // Stornierte Buchungen stehen nur auf Wunsch im Plan — dort, um ihre Angaben auf Verlangen der
    // Familie zu entfernen (Issue #129). Ohne Stornierte gibt es den Schalter nicht.
    stornierteSchalter.setLabel(getTranslation("auswertung.stornierte.label"));
    stornierteSchalter.addClassName("auswertung__stornierte-schalter");
    stornierteSchalter.setVisible(false);
    stornierteSchalter.addValueChangeListener(
        event -> {
          if (event.isFromClient()) {
            onStornierteChange(event.getValue());
          }
        });
    filterRow.add(suchfeld, lehrkraftFilter, stornierteSchalter);
    return filterRow;
  }

  /**
   * Hinweis unter der Filterzeile, wenn die Suche stornierte Buchungen trifft, die der Schalter
   * gerade ausblendet — beim Auskunftsverlangen gehören sie dazu. Sichtbar nur, wenn der Presenter
   * eine Zahl liefert.
   */
  private Component createSuchhinweis() {
    suchhinweis.addClassName("auswertung__suchhinweis");
    suchhinweis.getElement().setAttribute("role", "status");
    suchhinweis.setVisible(false);
    suchhinweisText.addClassName("auswertung__suchhinweis-text");
    Button anzeigen = new Button(getTranslation("auswertung.stornierte.label"));
    anzeigen.addClassName("auswertung__suchhinweis-button");
    anzeigen.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
    anzeigen.addClickListener(_ -> zeigeStornierte());
    suchhinweis.add(suchhinweisText, anzeigen);
    return suchhinweis;
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
    if (plan.entfalleneAnzahl() > 0) {
      head.add(createEntfallenHinweis(plan.entfalleneAnzahl()));
    }
    if (ausfallMoeglich) {
      head.add(createAusfallButton(plan));
    }
    return head;
  }

  private Component createEntfallenHinweis(int entfalleneAnzahl) {
    Span hinweis = new Span(entfallenLabel(entfalleneAnzahl));
    hinweis.addClassName("auswertung__section-entfallen");
    return hinweis;
  }

  private String entfallenLabel(int entfalleneAnzahl) {
    return entfalleneAnzahl == 1
        ? getTranslation("auswertung.section.entfallen.one")
        : getTranslation("auswertung.section.entfallen.other", entfalleneAnzahl);
  }

  private Component createAusfallButton(LehrkraftPlan plan) {
    Button button = new Button(getTranslation("auswertung.ausfall.button"));
    button.addClassName("auswertung__ausfall-button");
    button.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
    button.addClickListener(_ -> openAusfallDialog(plan));
    return button;
  }

  private void openAusfallDialog(LehrkraftPlan plan) {
    // Die Auswertung ist ein Read-Modell und darf veraltet sein — dieselbe Begründung wie beim
    // Umbuchen-Dialog. Der Dialog zeigt, was der Query-Port gerade liefert.
    List<SlotZeile> slots = presenter.ausfallSlots(sprechtagId, plan.lehrerId());
    new AusfallDialog(plan.anzeigeName(), slots, this::entfalleLassen).open();
  }

  private void entfalleLassen(List<UUID> terminIds) {
    AusfallAusgang ausgang = presenter.entfalleLassen(terminIds);
    if (ausgang.istWeigerung()) {
      SprechtagMeldungen.zeige(this, ausgang.weigerung());
    } else {
      Notification.show(
          getTranslation(
              "auswertung.ausfall.erfolg",
              ausgang.ergebnis().entfalleneTermine(),
              ausgang.ergebnis().benachrichtigteAdressen()));
    }
    reload();
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
    List<BuchungsZeile> zeilen = presenter.sichtbareZeilen(plan, stornierteAnzeigen);
    if (zeilen.isEmpty()) {
      Div empty = new Div();
      empty.addClassName("auswertung__section-empty");
      empty.setText(getTranslation("auswertung.section.empty"));
      return empty;
    }

    Div table = new Div();
    table.addClassName("auswertung__table");
    if (aktionsspalte) {
      table.addClassName("auswertung__table--mit-aktion");
    }
    table.add(createTableHead());
    zeilen.stream().map(zeile -> createRow(plan, zeile)).forEach(table::add);
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
    if (aktionsspalte) {
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
    if (zeile.storniert()) {
      row.addClassName("auswertung__row--storniert");
    }
    row.add(
        createZeitCell(zeile),
        cell(zeile.schuelerName(), "auswertung__cell--schueler"),
        cell(zeile.klasse(), "auswertung__cell--klasse"),
        cell(zeile.fach(), "auswertung__cell--fach"),
        cell(zeile.elternName(), "auswertung__cell--eltern"),
        createNotizCell(zeile));
    if (aktionsspalte) {
      row.add(createAktion(plan, zeile));
    }
    return row;
  }

  /** Die Uhrzeit bleibt Scan-Anker; eine stornierte Zeile trägt darunter ihren Zustand. */
  private Component createZeitCell(BuchungsZeile zeile) {
    Div cell = new Div();
    cell.addClassName("auswertung__cell");
    cell.addClassName("auswertung__cell--zeit");
    cell.add(new Span(Formats.time(zeile.startzeit())));
    presenter
        .zustandsEtikett(zeile)
        .ifPresent(
            schluessel -> {
              Span etikett = new Span(getTranslation(schluessel));
              etikett.addClassName("auswertung__zeile-zustand");
              cell.add(etikett);
            });
    return cell;
  }

  /**
   * Die Notiz — oder, sind die Angaben entfernt, der Vermerk dazu. Die Notiz ist dann ohnehin leer,
   * und der Vermerk erklärt die Platzhalter in derselben Zeile.
   */
  private Component createNotizCell(BuchungsZeile zeile) {
    Optional<String> vermerk =
        presenter
            .entferntVermerk(auswertung, zeile)
            .map(datum -> getTranslation("auswertung.zeile.entfernt", Formats.dateLong(datum)));
    if (vermerk.isPresent()) {
      Span cell = cell(vermerk.get(), "auswertung__cell--notiz");
      cell.addClassName("auswertung__cell--entfernt");
      return cell;
    }
    return cell(zeile.notiz() == null ? "" : zeile.notiz(), "auswertung__cell--notiz");
  }

  private Component createAktion(LehrkraftPlan plan, BuchungsZeile zeile) {
    Div aktion = new Div();
    aktion.addClassName("auswertung__cell");
    aktion.addClassName("auswertung__cell--aktion");
    if (presenter.darfStornieren(auswertung, zeile)) {
      aktion.add(createUmbuchenButton(plan, zeile), createStornoButton(plan, zeile));
    }
    if (presenter.darfAngabenEntfernen(auswertung, zeile)) {
      aktion.add(createEntfernenButton(plan, zeile));
    }
    return aktion;
  }

  private Button createEntfernenButton(LehrkraftPlan plan, BuchungsZeile zeile) {
    Button entfernen = new Button(VaadinIcon.ERASER.create());
    entfernen.addClassName("auswertung__entfernen");
    entfernen.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR, ButtonVariant.SMALL);
    entfernen.setAriaLabel(getTranslation("auswertung.entfernen.button"));
    entfernen.setTooltipText(getTranslation("auswertung.entfernen.button"));
    entfernen.addClickListener(_ -> openEntfernenDialog(plan, zeile));
    return entfernen;
  }

  private void openEntfernenDialog(LehrkraftPlan plan, BuchungsZeile zeile) {
    new AngabenEntfernenDialog(
            zeile.schuelerName(),
            zeile.elternName(),
            Formats.time(zeile.startzeit()),
            plan.anzeigeName(),
            () -> entferneAngaben(zeile))
        .open();
  }

  private void entferneAngaben(BuchungsZeile zeile) {
    Optional<Meldung> weigerung = presenter.entferneAngaben(zeile.buchungId());
    if (weigerung.isPresent()) {
      SprechtagMeldungen.zeige(this, weigerung.get());
    } else {
      Notification.show(getTranslation("auswertung.entfernen.erfolg", zeile.schuelerName()));
    }
    reload();
  }

  private Button createUmbuchenButton(LehrkraftPlan plan, BuchungsZeile zeile) {
    Button umbuchen = new Button(VaadinIcon.EXCHANGE.create());
    umbuchen.addClassName("auswertung__umbuchen");
    umbuchen.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
    umbuchen.setAriaLabel(getTranslation("auswertung.umbuchen.button"));
    umbuchen.setTooltipText(getTranslation("auswertung.umbuchen.button"));
    umbuchen.addClickListener(_ -> openUmbuchenDialog(plan, zeile));
    return umbuchen;
  }

  private Button createStornoButton(LehrkraftPlan plan, BuchungsZeile zeile) {
    Button storno = new Button(VaadinIcon.CLOSE_SMALL.create());
    storno.addClassName("auswertung__storno");
    storno.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR, ButtonVariant.SMALL);
    storno.setAriaLabel(getTranslation("auswertung.storno.button"));
    storno.setTooltipText(getTranslation("auswertung.storno.button"));
    storno.addClickListener(_ -> openStornoDialog(plan, zeile));
    return storno;
  }

  private void openStornoDialog(LehrkraftPlan plan, BuchungsZeile zeile) {
    new StornoBuchungDialog(
            zeile.schuelerName(),
            Formats.time(zeile.startzeit()),
            plan.anzeigeName(),
            angabenEntfernen -> storniere(zeile, angabenEntfernen))
        .open();
  }

  private void storniere(BuchungsZeile zeile, boolean angabenEntfernen) {
    Optional<Meldung> weigerung = presenter.storniere(zeile.buchungId(), angabenEntfernen);
    if (weigerung.isPresent()) {
      SprechtagMeldungen.zeige(this, weigerung.get());
    } else {
      Notification.show(
          getTranslation(presenter.stornoErfolg(angabenEntfernen), zeile.schuelerName()));
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

  private Span cell(String text, String modifier) {
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
