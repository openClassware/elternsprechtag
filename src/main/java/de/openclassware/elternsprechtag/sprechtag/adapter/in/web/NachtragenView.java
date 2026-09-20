package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.NachtragenPresenter.SprechtagKopf;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.AuswahlZeile;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.Breadcrumb;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.LehrkraftKarte;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.SlotLegende;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.StepHeader;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components.TerminRaster;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts.MainLayout;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.LehrkraftOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.SlotOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl.KlasseOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import de.openclassware.elternsprechtag.sprechtag.domain.ZeitkonfliktException;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Organizer-Nachtrag: der Organizer bucht im Namen einer Familie, die noch keine Buchung hat —
 * derselbe Ablauf wie die Eltern-Strecke ({@link ElternsprechtagView}), nur hinter der Anmeldung
 * und im Anwendungs-Layout. Eine eigene Ansicht statt eines Modus an der Eltern-Ansicht: Die
 * beiden Zugangsniveaus (anonym per Token vs. angemeldeter Organizer) heißen in Vaadin zwei
 * Ansichtsklassen.
 *
 * <p>Baut aus denselben Komponenten wie die Eltern-Ansicht ({@code components}-Paket) und nutzt
 * denselben {@link BookingSession}-Warenkorb unverändert mit.
 */
@Route(value = NachtragenView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/elternsprechtag-view.css")
@CssImport("./styles/nachtragen-view.css")
public class NachtragenView extends Div implements HasUrlParameter<String> {

  public static final String ROUTE = "nachtragen";

  private final NachtragenPresenter presenter;

  private UUID sprechtagId;
  private SprechtagKopf sprechtag;

  private TextField elternName;
  private TextField schuelerName;
  private EmailField elternEmail;
  private Checkbox stellvertreterSchalter;
  private Select<KlasseOption> klasse;

  private Div footerStatus;
  private Button bookButton;

  /** Buchungs-„Warenkorb" + Entscheidungslogik; Vaadin-frei und unit-getestet — unverändert wie in {@link ElternsprechtagView}. */
  private final BookingSession session = new BookingSession();

  private Div lehrkraftListe;
  private Div summaryContainer;

  NachtragenView(NachtragenPresenter presenter) {
    this.presenter = presenter;
    addClassName("nachtragen-view");
  }

  @Override
  public void setParameter(BeforeEvent event, String sprechtagId) {
    removeAll();
    Optional<UUID> id = parseId(sprechtagId);
    Optional<SprechtagKopf> geladen = id.flatMap(presenter::ladeSprechtag);
    if (geladen.isEmpty()) {
      event.rerouteToError(NotFoundException.class, "Sprechtag not found: " + sprechtagId);
      return;
    }
    this.sprechtagId = id.get();
    add(createBreadcrumb(), createBookingCard(geladen.get()));
  }

  private Component createBookingCard(SprechtagKopf sprechtag) {
    this.sprechtag = sprechtag;
    session.reset(List.of());

    Div card = new Div();
    card.addClassName("elternsprechtag-view__card");

    Div body = new Div();
    body.addClassName("elternsprechtag-view__body");
    body.add(createAngaben(sprechtag), createBuchung(sprechtag), createAuswahl());

    card.add(createKopf(sprechtag), body, createFooter());

    refreshLehrkraefte();
    refreshSummary();
    refreshFooter();
    return card;
  }

  private Div createKopf(SprechtagKopf sprechtag) {
    Div kopf = new Div();
    kopf.addClassName("elternsprechtag-view__kopf");

    H1 title = new H1(sprechtag.titel());
    title.addClassName("elternsprechtag-view__title");
    kopf.add(title);

    Div meta = new Div();
    meta.addClassName("elternsprechtag-view__meta");
    meta.add(
        metaItem(VaadinIcon.CALENDAR, Formats.dateLong(sprechtag.datum())),
        metaItem(
            VaadinIcon.CLOCK,
            Formats.time(sprechtag.beginn()) + "–" + Formats.time(sprechtag.ende())));
    if (sprechtag.ort() != null && !sprechtag.ort().isBlank()) {
      meta.add(metaItem(VaadinIcon.MAP_MARKER, sprechtag.ort()));
    }
    kopf.add(meta);

    Paragraph intro = new Paragraph(getTranslation("nachtragen.intro"));
    intro.addClassName("elternsprechtag-view__intro");
    kopf.add(intro);

    return kopf;
  }

  private Component createAngaben(SprechtagKopf sprechtag) {
    Div section = new Div();
    section.addClassName("elternsprechtag-view__section");
    section.add(new StepHeader(1, getTranslation("elternsprechtag.angaben.step-title")));

    elternName = new TextField(getTranslation("elternsprechtag.angaben.name.label"));
    elternName.setPlaceholder(getTranslation("elternsprechtag.angaben.name.placeholder"));
    elternName.setRequiredIndicatorVisible(true);
    elternName.setValueChangeMode(ValueChangeMode.EAGER);
    elternName.addValueChangeListener(event -> refreshFooter());

    schuelerName = new TextField(getTranslation("elternsprechtag.angaben.kind.label"));
    schuelerName.setPlaceholder(getTranslation("elternsprechtag.angaben.kind.placeholder"));
    schuelerName.setRequiredIndicatorVisible(true);
    schuelerName.setValueChangeMode(ValueChangeMode.EAGER);
    schuelerName.addValueChangeListener(event -> refreshFooter());

    elternEmail = new EmailField(getTranslation("elternsprechtag.angaben.email.label"));
    elternEmail.setPlaceholder(getTranslation("elternsprechtag.angaben.email.placeholder"));
    elternEmail.setRequiredIndicatorVisible(true);
    elternEmail.setErrorMessage(getTranslation("elternsprechtag.angaben.email.error"));
    elternEmail.setClearButtonVisible(true);
    elternEmail.setValueChangeMode(ValueChangeMode.EAGER);
    elternEmail.addValueChangeListener(event -> refreshFooter());

    klasse = new Select<>();
    klasse.setLabel(getTranslation("elternsprechtag.angaben.klasse.label"));
    klasse.setPlaceholder(getTranslation("elternsprechtag.angaben.klasse.placeholder"));
    klasse.setItemLabelGenerator(KlasseOption::name);
    klasse.setItems(sprechtag.klassen());
    klasse.setRequiredIndicatorVisible(true);
    klasse.addValueChangeListener(event -> onKlasseChanged());

    FormLayout form = new FormLayout();
    form.addClassName("elternsprechtag-view__form");
    form.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("640px", 2));
    form.add(elternName, schuelerName, elternEmail, klasse);

    section.add(form);
    if (presenter.stellvertreteradresseVerfuegbar()) {
      section.add(createStellvertreterSchalter());
    }
    return section;
  }

  /**
   * Schalter für Familien ohne eigene E-Mail-Adresse: setzt die konfigurierte Stellvertreteradresse
   * ein und sperrt das Feld, solange er aktiv ist. Ob er überhaupt erscheint, hat der Presenter
   * bereits entschieden — die Ansicht fragt nur noch danach.
   */
  private Component createStellvertreterSchalter() {
    stellvertreterSchalter = new Checkbox(getTranslation("nachtragen.stellvertreter.label"));
    stellvertreterSchalter.addClassName("nachtragen-view__stellvertreter-schalter");
    stellvertreterSchalter.getElement().getThemeList().add("switch");
    stellvertreterSchalter.setHelperText(getTranslation("nachtragen.stellvertreter.helper"));
    stellvertreterSchalter.addValueChangeListener(event -> onStellvertreterChanged(event.getValue()));
    return stellvertreterSchalter;
  }

  private void onStellvertreterChanged(boolean aktiv) {
    if (aktiv) {
      elternEmail.setValue(presenter.stellvertreteradresse());
      elternEmail.setReadOnly(true);
    } else {
      elternEmail.setReadOnly(false);
      elternEmail.clear();
    }
    refreshFooter();
  }

  private Component createBuchung(SprechtagKopf sprechtag) {
    Div section = new Div();
    section.addClassName("elternsprechtag-view__section");

    Div stepHead = new Div();
    stepHead.addClassName("elternsprechtag-view__step-head");
    stepHead.add(
        new StepHeader(2, getTranslation("elternsprechtag.lehrkraft.step-title")),
        new SlotLegende());
    section.add(stepHead);

    Paragraph hint =
        new Paragraph(getTranslation("elternsprechtag.termin.hint", sprechtag.slotInMinuten()));
    hint.addClassName("elternsprechtag-view__slot-hint");
    section.add(hint);

    lehrkraftListe = new Div();
    lehrkraftListe.addClassName("elternsprechtag-view__lehrkraft-liste");
    section.add(lehrkraftListe);

    return section;
  }

  private Component createAuswahl() {
    Div section = new Div();
    section.addClassName("elternsprechtag-view__section");
    section.add(new StepHeader(3, getTranslation("elternsprechtag.summary.step-title")));

    summaryContainer = new Div();
    summaryContainer.addClassName("elternsprechtag-view__summary-container");
    section.add(summaryContainer);

    return section;
  }

  private void onKlasseChanged() {
    session.reset(loadOptionen());
    refreshAfterSelectionChange();
  }

  private List<LehrkraftOption> loadOptionen() {
    KlasseOption selected = klasse.getValue();
    return selected == null
        ? List.of()
        : presenter.ladeLehrkraftOptionen(sprechtagId, selected.id());
  }

  private Component placeholder(String translationKey) {
    Div placeholder = new Div();
    placeholder.addClassName("elternsprechtag-view__placeholder");
    placeholder.setText(getTranslation(translationKey));
    return placeholder;
  }

  private Component refreshLehrkraefte() {
    lehrkraftListe.removeAll();
    if (!session.hatOptionen()) {
      lehrkraftListe.add(placeholder("elternsprechtag.lehrkraft.placeholder"));
      return null;
    }
    Component offenes = null;
    for (LehrkraftOption lehrkraft : session.optionen()) {
      Div item = new Div();
      item.addClassName("elternsprechtag-view__lehrkraft-item");
      item.add(createLehrkraftCard(lehrkraft));
      if (session.isActive(lehrkraft)) {
        item.addClassName("elternsprechtag-view__lehrkraft-item--offen");
        item.add(createSlotPanel(lehrkraft));
        offenes = item;
      }
      lehrkraftListe.add(item);
    }
    return offenes;
  }

  private Component createLehrkraftCard(LehrkraftOption lehrkraft) {
    boolean selected =
        session.isActive(lehrkraft) || session.istGewaehlt(lehrkraft.lehrauftragId());
    SlotOption chosen = session.gewaehlterSlot(lehrkraft.lehrauftragId());
    return new LehrkraftKarte(
        lehrkraft,
        selected,
        chosen,
        () -> {
          session.setActive(lehrkraft);
          refreshLehrkraefte();
        });
  }

  private Component createSlotPanel(LehrkraftOption lehrkraft) {
    UUID lehrauftragId = lehrkraft.lehrauftragId();
    return new TerminRaster(
        lehrkraft.slots(),
        this::zustandVon,
        this::selectSlot,
        this::deselectSlot,
        session.notiz(lehrauftragId),
        session.istGewaehlt(lehrauftragId),
        notizText -> {
          session.setNotiz(lehrauftragId, notizText);
          refreshSummary();
        });
  }

  private TerminRaster.SlotZustand zustandVon(SlotOption slot) {
    return switch (session.slotState(slot)) {
      case FREI -> TerminRaster.SlotZustand.FREI;
      case BELEGT -> TerminRaster.SlotZustand.BELEGT;
      case GEWAEHLT -> TerminRaster.SlotZustand.GEWAEHLT;
      case KONFLIKT -> TerminRaster.SlotZustand.KONFLIKT;
    };
  }

  private void selectSlot(SlotOption slot) {
    session.waehle(slot);
    refreshAfterSelectionChange();
  }

  private void deselectSlot() {
    session.abwaehlen();
    refreshAfterSelectionChange();
  }

  private void refreshAfterSelectionChange() {
    refreshLehrkraefte();
    refreshSummary();
    refreshFooter();
  }

  private void springeZu(LehrkraftOption lehrkraft) {
    session.setActive(lehrkraft);
    Component offenes = refreshLehrkraefte();
    if (offenes != null) {
      offenes.getElement().scrollIntoView();
    }
  }

  private void refreshSummary() {
    summaryContainer.removeAll();
    if (!session.hatAuswahl()) {
      Div placeholder = new Div();
      placeholder.addClassName("elternsprechtag-view__placeholder");
      placeholder.setText(getTranslation("elternsprechtag.summary.placeholder"));
      summaryContainer.add(placeholder);
      return;
    }

    Div panel = new Div();
    panel.addClassName("elternsprechtag-view__summary");
    for (LehrkraftOption lehrkraft : session.optionen()) {
      SlotOption slot = session.gewaehlterSlot(lehrkraft.lehrauftragId());
      if (slot != null) {
        panel.add(createSummaryRow(lehrkraft, slot));
      }
    }
    summaryContainer.add(panel);
  }

  private String countLabel(int count) {
    return count == 1
        ? getTranslation("elternsprechtag.summary.count.one")
        : getTranslation("elternsprechtag.summary.count.other", count);
  }

  private Component createSummaryRow(LehrkraftOption lehrkraft, SlotOption slot) {
    return new AuswahlZeile(
        lehrkraft,
        slot,
        session.notiz(lehrkraft.lehrauftragId()),
        () -> springeZu(lehrkraft),
        () -> {
          session.entferne(lehrkraft.lehrauftragId());
          refreshAfterSelectionChange();
        });
  }

  private Component createFooter() {
    Div footer = new Div();
    footer.addClassName("elternsprechtag-view__footer");

    footerStatus = new Div();
    footerStatus.addClassName("elternsprechtag-view__footer-status");

    bookButton = new Button();
    bookButton.setIcon(VaadinIcon.ARROW_RIGHT.create());
    bookButton.setIconAfterText(true);
    bookButton.addThemeVariants(ButtonVariant.PRIMARY);
    bookButton.addClickListener(event -> submit());

    footer.add(footerStatus, bookButton);
    return footer;
  }

  private void submit() {
    if (!bookingValid()) {
      return;
    }
    Angaben angaben =
        new Angaben(
            schuelerName.getValue().trim(),
            klasse.getValue().name(),
            elternName.getValue().trim(),
            elternEmail.getValue().trim());
    NachtragsAnfrage anfrage =
        new NachtragsAnfrage(angaben.eltern(), angaben.kind(), angaben.email(), zuWuenschen());

    try {
      int gebucht = presenter.trageNach(anfrage);
      showConfirmation(gebucht, angaben);
    } catch (TerminBelegtException conflict) {
      zeigeFehler("nachtragen.footer.conflict");
      handleConflict();
    } catch (ZeitkonfliktException zeitkonflikt) {
      zeigeFehler("nachtragen.footer.zeitkonflikt");
      handleConflict();
    } catch (SprechtagNichtVeroeffentlichtException nichtVeroeffentlicht) {
      zeigeFehler("nachtragen.footer.nicht-veroeffentlicht");
    }
  }

  private List<NachtragsWunsch> zuWuenschen() {
    return session.toWuensche().stream()
        .map(wunsch -> new NachtragsWunsch(wunsch.lehrauftragId(), wunsch.terminId(), wunsch.notiz()))
        .toList();
  }

  private void zeigeFehler(String translationKey) {
    Notification notification = Notification.show(getTranslation(translationKey));
    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  private void handleConflict() {
    session.reload(loadOptionen());
    refreshAfterSelectionChange();
  }

  private record Angaben(String kind, String klasseName, String eltern, String email) {}

  private void showConfirmation(int count, Angaben angaben) {
    removeAll();
    Div card = new Div();
    card.addClassName("elternsprechtag-view__card");
    card.add(createSuccessBanner(count), createConfirmKopf(), createConfirmBody(angaben, count));
    add(createBreadcrumb(), card);
  }

  private Div createConfirmKopf() {
    Div kopf = new Div();
    kopf.addClassName("elternsprechtag-view__kopf");
    H1 title = new H1(sprechtag.titel());
    title.addClassName("elternsprechtag-view__title");
    kopf.add(title);

    Div meta = new Div();
    meta.addClassName("elternsprechtag-view__meta");
    meta.add(metaItem(VaadinIcon.CALENDAR, Formats.dateLong(sprechtag.datum())));
    kopf.add(meta);
    return kopf;
  }

  private Component createSuccessBanner(int count) {
    Div banner = new Div();
    banner.addClassName("elternsprechtag-view__confirm-success");

    Span icon = new Span(VaadinIcon.CHECK_CIRCLE.create());
    icon.addClassName("elternsprechtag-view__confirm-success-icon");

    Div texts = new Div();
    texts.addClassName("elternsprechtag-view__confirm-success-text");
    H1 title = new H1(getTranslation("nachtragen.success.title"));
    title.addClassName("elternsprechtag-view__confirm-success-title");
    Paragraph description =
        new Paragraph(getTranslation("nachtragen.success.description", countLabel(count)));
    description.addClassName("elternsprechtag-view__confirm-success-desc");
    texts.add(title, description);

    banner.add(icon, texts);
    return banner;
  }

  private Component createConfirmBody(Angaben angaben, int count) {
    Div body = new Div();
    body.addClassName("elternsprechtag-view__body");
    body.add(createRecipient(angaben), createBookedTermine(count), createZurueckButton());
    return body;
  }

  private Component createRecipient(Angaben angaben) {
    Div recipient = new Div();
    recipient.addClassName("elternsprechtag-view__confirm-recipient");

    Div fuer = new Div();
    fuer.addClassName("elternsprechtag-view__confirm-for");
    fuer.setText(
        getTranslation("elternsprechtag.confirm.fuer", angaben.kind(), angaben.klasseName()));

    Div elternLine = new Div();
    elternLine.addClassName("elternsprechtag-view__confirm-eltern");
    elternLine.setText(getTranslation("elternsprechtag.confirm.eltern", angaben.eltern()));

    Div mailLine = new Div();
    mailLine.addClassName("elternsprechtag-view__confirm-mail");
    mailLine.setText(getTranslation("elternsprechtag.confirm.mail", angaben.email()));

    recipient.add(fuer, elternLine, mailLine);
    return recipient;
  }

  private Component createBookedTermine(int count) {
    Div panel = new Div();
    panel.addClassName("elternsprechtag-view__summary");

    Div head = new Div();
    head.addClassName("elternsprechtag-view__summary-head");
    Span title = new Span(getTranslation("nachtragen.confirm.termine.title"));
    title.addClassName("elternsprechtag-view__summary-title");
    Span countLabel = new Span(countLabel(count));
    countLabel.addClassName("elternsprechtag-view__summary-count");
    head.add(title, countLabel);
    panel.add(head);

    for (LehrkraftOption lehrkraft : session.optionen()) {
      SlotOption slot = session.gewaehlterSlot(lehrkraft.lehrauftragId());
      if (slot != null) {
        panel.add(createConfirmRow(lehrkraft, slot));
      }
    }
    return panel;
  }

  private Component createConfirmRow(LehrkraftOption lehrkraft, SlotOption slot) {
    return new AuswahlZeile(lehrkraft, slot, session.notiz(lehrkraft.lehrauftragId()), null, null);
  }

  private Component createZurueckButton() {
    Div wrapper = new Div();
    wrapper.addClassName("nachtragen-view__zurueck");
    Button zurueck = new Button(getTranslation("nachtragen.confirm.zurueck"));
    zurueck.addThemeVariants(ButtonVariant.TERTIARY);
    zurueck.addClickListener(
        event ->
            getUI().ifPresent(ui -> ui.navigate(AuswertungView.ROUTE + "/" + sprechtagId)));
    wrapper.add(zurueck);
    return wrapper;
  }

  private Breadcrumb createBreadcrumb() {
    Breadcrumb breadcrumb = new Breadcrumb();
    breadcrumb.addClassName("nachtragen-view__breadcrumb");
    breadcrumb.addLink(getTranslation("breadcrumb.uebersicht"), OrganizerView.class);
    breadcrumb.addLink(
        getTranslation("manage-sprechtag.header.title"), ManageSprechtagView.class);
    breadcrumb.addCurrent(getTranslation("nachtragen.breadcrumb.title"));
    return breadcrumb;
  }

  private void refreshFooter() {
    if (footerStatus == null) {
      return;
    }
    int count = session.auswahlAnzahl();
    bookButton.setEnabled(bookingValid());
    bookButton.setText(
        count == 0
            ? getTranslation("nachtragen.footer.button.empty")
            : getTranslation("nachtragen.footer.button", countLabel(count)));
    footerStatus.setText(footerStatusText(count));
  }

  private String footerStatusText(int count) {
    if (count == 0) {
      return getTranslation("elternsprechtag.footer.empty");
    }
    String selected = getTranslation("elternsprechtag.footer.selected", countLabel(count));
    if (!namesFilled()) {
      return selected + " — " + getTranslation("elternsprechtag.footer.blocker.angaben");
    }
    if (!emailValid()) {
      return selected + " — " + getTranslation("elternsprechtag.footer.blocker.email");
    }
    if (klasse.getValue() == null) {
      return selected + " — " + getTranslation("elternsprechtag.footer.blocker.klasse");
    }
    return selected;
  }

  private boolean bookingValid() {
    return session.hatAuswahl() && namesFilled() && emailValid() && klasse.getValue() != null;
  }

  private boolean namesFilled() {
    return !elternName.getValue().isBlank() && !schuelerName.getValue().isBlank();
  }

  private boolean emailValid() {
    return !elternEmail.getValue().isBlank() && !elternEmail.isInvalid();
  }

  private Component metaItem(VaadinIcon icon, String text) {
    Div item = new Div();
    item.addClassName("elternsprechtag-view__meta-item");
    item.add(icon.create(), new Span(text));
    return item;
  }

  private Optional<UUID> parseId(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
