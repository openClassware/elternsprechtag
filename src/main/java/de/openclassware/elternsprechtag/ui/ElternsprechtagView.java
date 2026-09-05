package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.openclassware.elternsprechtag.services.BuchungService;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsAnfrage;
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftOption;
import de.openclassware.elternsprechtag.services.BuchungService.SlotOption;
import de.openclassware.elternsprechtag.services.KlassenService.KlasseOption;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagPublic;
import de.openclassware.elternsprechtag.ui.components.StepHeader;
import java.util.List;

@Route(value = ElternsprechtagView.ROUTE, autoLayout = false)
@AnonymousAllowed
@CssImport("./styles/elternsprechtag-view.css")
public class ElternsprechtagView extends Div implements HasUrlParameter<String> {

  public static final String ROUTE = "elternsprechtag";

  /** Zeichengrenze einer Notiz; deckt sich mit der Spaltenlänge in der Buchung. */
  private static final int NOTIZ_MAX_LENGTH = 500;

  private final ElternsprechtagPresenter presenter;

  private SprechtagPublic sprechtag;

  private TextField elternName;
  private TextField schuelerName;
  private EmailField elternEmail;
  private Select<KlasseOption> klasse;

  private Div footerStatus;
  private Button bookButton;

  /** Buchungs-„Warenkorb" + Entscheidungslogik; Vaadin-frei und unit-getestet. */
  private final BookingSession session = new BookingSession();

  private Div lehrkraftListe;
  private Div summaryContainer;

  ElternsprechtagView(ElternsprechtagPresenter presenter) {
    this.presenter = presenter;
    addClassName("elternsprechtag-view");
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String token) {
    removeAll();
    ElternsprechtagPresenter.ZugangsErgebnis ergebnis = presenter.pruefeZugang(token);
    switch (ergebnis.zugang()) {
      case BUCHBAR -> add(createHeader(), createBookingCard(ergebnis.sprechtag()));
      case ABGESAGT ->
          add(
              createMessage(
                  "elternsprechtag.cancelled.title", "elternsprechtag.cancelled.description"));
      case NICHT_VERFUEGBAR ->
          add(
              createMessage(
                  "elternsprechtag.unavailable.title", "elternsprechtag.unavailable.description"));
    }
  }

  private Component createHeader() {
    Div header = new Div();
    header.addClassName("elternsprechtag-header");

    Span name = new Span(presenter.getSchoolname());
    name.addClassName("elternsprechtag-header__school");

    header.add(name);
    return header;
  }

  /** Single card holding the Sprechtag head plus all booking steps and the footer. */
  private Component createBookingCard(SprechtagPublic sprechtag) {
    this.sprechtag = sprechtag;
    session.reset(List.of());

    Div card = new Div();
    card.addClassName("elternsprechtag-view__card");

    Div body = new Div();
    body.addClassName("elternsprechtag-view__body");
    body.add(createAngaben(sprechtag), createBuchung(sprechtag), createAuswahl());

    card.add(createInfo(sprechtag), body, createFooter());

    refreshLehrkraefte();
    refreshSummary();
    refreshFooter();
    return card;
  }

  private Component createInfo(SprechtagPublic sprechtag) {
    return createKopf(sprechtag, true);
  }

  /**
   * Sprechtag-Kopf (Titel + Meta), geteilt von Buchungs- und Bestätigungsseite. Nur beim Buchen kommen
   * die Intro-Zeile und die Beschreibung dazu.
   */
  private Div createKopf(SprechtagPublic sprechtag, boolean withBookingText) {
    Div kopf = new Div();
    kopf.addClassName("elternsprechtag-view__kopf");

    H1 title = new H1(sprechtag.titel());
    title.addClassName("elternsprechtag-view__title");
    kopf.add(title);

    Div meta = new Div();
    meta.addClassName("elternsprechtag-view__meta");
    meta.add(
        metaItem(VaadinIcon.CALENDAR, Formats.dateLong(sprechtag.startDate())),
        metaItem(
            VaadinIcon.CLOCK,
            Formats.time(sprechtag.startTime()) + "–" + Formats.time(sprechtag.endTime())));
    if (sprechtag.location() != null && !sprechtag.location().isBlank()) {
      meta.add(metaItem(VaadinIcon.MAP_MARKER, sprechtag.location()));
    }
    kopf.add(meta);

    if (withBookingText) {
      Paragraph intro = new Paragraph(getTranslation("elternsprechtag.intro"));
      intro.addClassName("elternsprechtag-view__intro");
      kopf.add(intro);

      if (sprechtag.description() != null && !sprechtag.description().isBlank()) {
        Paragraph description = new Paragraph(sprechtag.description());
        description.addClassName("elternsprechtag-view__description");
        kopf.add(description);
      }
    }

    return kopf;
  }

  private Component createAngaben(SprechtagPublic sprechtag) {
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
    elternEmail.setHelperText(getTranslation("elternsprechtag.angaben.email.helper"));
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
    return section;
  }

  /**
   * Schritt 2: die Lehrkräfte der Klasse als einspaltige Liste. Das Terminraster der aufgeklappten
   * Lehrkraft wird beim Rendern direkt hinter ihre Karte gesetzt, sodass Auswahl und Termine als ein
   * Block gelesen werden.
   */
  private Component createBuchung(SprechtagPublic sprechtag) {
    Div section = new Div();
    section.addClassName("elternsprechtag-view__section");

    Div stepHead = new Div();
    stepHead.addClassName("elternsprechtag-view__step-head");
    stepHead.add(
        new StepHeader(2, getTranslation("elternsprechtag.lehrkraft.step-title")), createLegend());
    section.add(stepHead);

    // Über der Liste, nicht darunter: Der Satz trägt die Mehrfachbuchungs-Botschaft und bliebe
    // unter einer langen Liste mit aufgeklapptem Raster ungelesen.
    Paragraph hint =
        new Paragraph(getTranslation("elternsprechtag.termin.hint", sprechtag.slotInMinutes()));
    hint.addClassName("elternsprechtag-view__slot-hint");
    section.add(hint);

    lehrkraftListe = new Div();
    lehrkraftListe.addClassName("elternsprechtag-view__lehrkraft-liste");
    section.add(lehrkraftListe);

    return section;
  }

  /** Schritt 3: die gewählten Termine als Kontrolle vor dem Absenden. */
  private Component createAuswahl() {
    Div section = new Div();
    section.addClassName("elternsprechtag-view__section");
    section.add(new StepHeader(3, getTranslation("elternsprechtag.summary.step-title")));

    summaryContainer = new Div();
    summaryContainer.addClassName("elternsprechtag-view__summary-container");
    section.add(summaryContainer);

    return section;
  }

  /** Reagiert auf die Klassenwahl: lädt die echten Lehrkräfte/Termine und verwirft die Auswahl. */
  private void onKlasseChanged() {
    session.reset(loadOptionen());
    refreshAfterSelectionChange();
  }

  private List<LehrkraftOption> loadOptionen() {
    KlasseOption selected = klasse.getValue();
    return selected == null
        ? List.of()
        : presenter.ladeLehrkraftOptionen(sprechtag.id(), selected.id());
  }

  private Component createLegend() {
    Div legend = new Div();
    legend.addClassName("elternsprechtag-view__legend");
    legend.add(
        legendItem("frei", "elternsprechtag.termin.legend.frei"),
        legendItem("belegt", "elternsprechtag.termin.legend.belegt"));
    return legend;
  }

  private Component legendItem(String modifier, String translationKey) {
    Div item = new Div();
    item.addClassName("elternsprechtag-view__legend-item");
    Span swatch = new Span();
    swatch.addClassName("elternsprechtag-view__legend-swatch");
    swatch.addClassName("elternsprechtag-view__legend-swatch--" + modifier);
    item.add(swatch, new Span(getTranslation(translationKey)));
    return item;
  }

  /** Gestrichelter Hinweiskasten für einen leeren Bereich. */
  private Component placeholder(String translationKey) {
    Div placeholder = new Div();
    placeholder.addClassName("elternsprechtag-view__placeholder");
    placeholder.setText(getTranslation(translationKey));
    return placeholder;
  }

  /**
   * Rendert die Lehrkraft-Liste samt dem Terminraster der aufgeklappten Lehrkraft und gibt deren
   * Eintrag zurück — oder {@code null}, wenn keine aufgeklappt ist.
   */
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
    Div card = new Div();
    card.addClassName("elternsprechtag-view__lehrkraft");
    if (session.isActive(lehrkraft) || session.istGewaehlt(lehrkraft.lehrauftragId())) {
      card.addClassName("elternsprechtag-view__lehrkraft--selected");
    }

    Span badge = new Span(lehrkraft.kuerzel());
    badge.addClassName("elternsprechtag-view__lehrkraft-badge");

    Div info = new Div();
    info.addClassName("elternsprechtag-view__lehrkraft-info");
    Div name = new Div();
    name.addClassName("elternsprechtag-view__lehrkraft-name");
    name.setText(lehrkraft.lehrerName());
    Div faecher = new Div();
    faecher.addClassName("elternsprechtag-view__lehrkraft-faecher");
    faecher.setText(String.join(", ", lehrkraft.faecher()));
    info.add(name, faecher);

    card.add(badge, info);

    SlotOption chosen = session.gewaehlterSlot(lehrkraft.lehrauftragId());
    if (chosen != null) {
      Span pill = new Span();
      pill.addClassName("elternsprechtag-view__lehrkraft-pill");
      pill.add(VaadinIcon.CHECK.create(), new Span(Formats.time(chosen.zeit())));
      card.add(pill);
    }

    card.addClickListener(
        event -> {
          session.setActive(lehrkraft);
          refreshLehrkraefte();
        });
    return card;
  }

  /** Terminraster der aufgeklappten Lehrkraft; sitzt in der Liste direkt hinter ihrer Karte. */
  private Component createSlotPanel(LehrkraftOption lehrkraft) {
    Div panel = new Div();
    panel.addClassName("elternsprechtag-view__slot-panel");

    if (lehrkraft.slots().isEmpty()) {
      Div placeholder = new Div();
      placeholder.addClassName("elternsprechtag-view__placeholder");
      placeholder.setText(getTranslation("elternsprechtag.termin.empty"));
      panel.add(placeholder);
      return panel;
    }

    Div grid = new Div();
    grid.addClassName("elternsprechtag-view__slot-grid");
    lehrkraft.slots().forEach(slot -> grid.add(createSlot(slot)));
    panel.add(grid);

    // Ohne Termin gibt es keine Buchung, an der die Notiz hängen könnte.
    if (session.istGewaehlt(lehrkraft.lehrauftragId())) {
      panel.add(createNotizField(lehrkraft));
    }
    return panel;
  }

  /**
   * Notizfeld der aufgeklappten Lehrkraft. Es schreibt bei jedem Tastendruck ins Modell, damit beim
   * Panel-Wechsel nichts verloren geht, und frischt nur die Zusammenfassung auf — ein Neuaufbau des
   * Panels würde dem Feld den Fokus nehmen.
   */
  private Component createNotizField(LehrkraftOption lehrkraft) {
    TextArea notiz =
        new TextArea(getTranslation("elternsprechtag.notiz.label", lehrkraft.lehrerName()));
    notiz.addClassName("elternsprechtag-view__notiz-field");
    notiz.setPlaceholder(getTranslation("elternsprechtag.notiz.placeholder"));
    notiz.setHelperText(getTranslation("elternsprechtag.notiz.helper"));
    notiz.setWidthFull();
    notiz.setMaxLength(NOTIZ_MAX_LENGTH);
    notiz.setValue(session.notiz(lehrkraft.lehrauftragId()));
    notiz.setValueChangeMode(ValueChangeMode.EAGER);
    notiz.addValueChangeListener(
        event -> {
          session.setNotiz(lehrkraft.lehrauftragId(), event.getValue());
          refreshSummary();
        });
    return notiz;
  }

  private Component createSlot(SlotOption slot) {
    Div slotEl = new Div();
    slotEl.addClassName("elternsprechtag-view__slot");
    slotEl.add(new Span(Formats.time(slot.zeit())));

    switch (session.slotState(slot)) {
      case BELEGT -> slotEl.addClassName("elternsprechtag-view__slot--belegt");
      case KONFLIKT -> slotEl.addClassName("elternsprechtag-view__slot--konflikt");
      case GEWAEHLT -> {
        slotEl.addClassName("elternsprechtag-view__slot--selected");
        Span check = new Span();
        check.addClassName("elternsprechtag-view__slot-check");
        check.add(VaadinIcon.CHECK.create());
        slotEl.add(check);
        slotEl.addClickListener(event -> deselectSlot());
      }
      case FREI -> slotEl.addClickListener(event -> selectSlot(slot));
    }
    return slotEl;
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

  /** Springt zur Lehrkraft einer Auswahl-Zeile: klappt sie auf und scrollt ihre Karte ins Bild. */
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

    // Ohne eigenen Kopf: Der Schritt trägt bereits den Titel, die Anzahl steht im Footer.
    Div panel = new Div();
    panel.addClassName("elternsprechtag-view__summary");

    // In Lehrkraft-Reihenfolge rendern, nicht in Auswahl-Reihenfolge.
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
    Div row = new Div();
    row.addClassName("elternsprechtag-view__summary-row");

    // Klickfläche für den Sprung zur Lehrkraft; der Entfernen-Button liegt bewusst außerhalb,
    // damit sein Klick nicht zugleich das Panel aufklappt.
    Div link = new Div();
    link.addClassName("elternsprechtag-view__summary-link");
    link.add(createSummaryBadge(lehrkraft), createSummaryInfo(lehrkraft, slot));
    link.addClickListener(event -> springeZu(lehrkraft));

    Button remove = new Button(VaadinIcon.CLOSE_SMALL.create());
    remove.addClassName("elternsprechtag-view__summary-remove");
    remove.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
    remove.addClickListener(
        event -> {
          session.entferne(lehrkraft.lehrauftragId());
          refreshAfterSelectionChange();
        });

    row.add(link, remove);
    return row;
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
    // Snapshot vor removeAll(): Die Bestätigungskarte rendert aus diesen Werten, nicht aus dem
    // dann bereits geleerten Formular.
    Angaben angaben =
        new Angaben(
            schuelerName.getValue().trim(),
            klasse.getValue().name(),
            elternName.getValue().trim(),
            elternEmail.getValue().trim());
    BuchungsAnfrage anfrage =
        new BuchungsAnfrage(
            angaben.eltern(), angaben.kind(), angaben.email(), session.toWuensche());

    try {
      int gebucht = presenter.buchen(anfrage);
      showConfirmation(gebucht, angaben);
    } catch (BuchungService.TerminBelegtException conflict) {
      Notification notification =
          Notification.show(getTranslation("elternsprechtag.footer.conflict"));
      notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
      handleConflict();
    }
  }

  /** Nach einem Konflikt: Optionen neu laden und ungültig gewordene Slots aus der Auswahl werfen. */
  private void handleConflict() {
    session.reload(loadOptionen());
    refreshAfterSelectionChange();
  }

  /** Die abgeschickten Formularwerte; überdauert das Leeren des Formulars. */
  private record Angaben(String kind, String klasseName, String eltern, String email) {}

  /** Bestätigungskarte im Stil der Buchungsseite: Erfolgs-Banner, Sprechtag-Kopf, Empfänger, Termine. */
  private void showConfirmation(int count, Angaben angaben) {
    removeAll();
    Div card = new Div();
    card.addClassName("elternsprechtag-view__card");
    card.add(
        createSuccessBanner(count), createKopf(sprechtag, false), createConfirmBody(angaben, count));
    add(createHeader(), card);
  }

  private Component createSuccessBanner(int count) {
    Div banner = new Div();
    banner.addClassName("elternsprechtag-view__confirm-success");

    Span icon = new Span(VaadinIcon.CHECK_CIRCLE.create());
    icon.addClassName("elternsprechtag-view__confirm-success-icon");

    Div texts = new Div();
    texts.addClassName("elternsprechtag-view__confirm-success-text");
    H1 title = new H1(getTranslation("elternsprechtag.success.title"));
    title.addClassName("elternsprechtag-view__confirm-success-title");
    Paragraph description =
        new Paragraph(getTranslation("elternsprechtag.success.description", countLabel(count)));
    description.addClassName("elternsprechtag-view__confirm-success-desc");
    texts.add(title, description);

    banner.add(icon, texts);
    return banner;
  }

  private Component createConfirmBody(Angaben angaben, int count) {
    Div body = new Div();
    body.addClassName("elternsprechtag-view__body");
    body.add(createRecipient(angaben), createBookedTermine(count));
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

    // Zukunftsform: Die Mail geht asynchron nach Commit raus, ist beim Rendern also unterwegs.
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
    Span title = new Span(getTranslation("elternsprechtag.confirm.termine.title"));
    title.addClassName("elternsprechtag-view__summary-title");
    Span countLabel = new Span(countLabel(count));
    countLabel.addClassName("elternsprechtag-view__summary-count");
    head.add(title, countLabel);
    panel.add(head);

    // In Lehrkraft-Reihenfolge, wie die Auswahl-Zusammenfassung — nur statisch (ohne Entfernen).
    for (LehrkraftOption lehrkraft : session.optionen()) {
      SlotOption slot = session.gewaehlterSlot(lehrkraft.lehrauftragId());
      if (slot != null) {
        panel.add(createConfirmRow(lehrkraft, slot));
      }
    }
    return panel;
  }

  private Component createConfirmRow(LehrkraftOption lehrkraft, SlotOption slot) {
    Div row = new Div();
    row.addClassName("elternsprechtag-view__summary-row");
    row.add(createSummaryBadge(lehrkraft), createSummaryInfo(lehrkraft, slot));
    return row;
  }

  private Component createSummaryBadge(LehrkraftOption lehrkraft) {
    Span badge = new Span(lehrkraft.kuerzel());
    badge.addClassName("elternsprechtag-view__summary-badge");
    return badge;
  }

  /**
   * Textblock einer Terminzeile — Lehrkraft mit Uhrzeit, ihre Fächer und, sofern geschrieben, die
   * Notiz schreibgeschützt und im vollen Wortlaut. Auswahl und Bestätigung teilen ihn sich; nur die
   * Auswahl hängt noch den Entfernen-Button daneben.
   */
  private Component createSummaryInfo(LehrkraftOption lehrkraft, SlotOption slot) {
    Div info = new Div();
    info.addClassName("elternsprechtag-view__summary-info");

    Div main = new Div();
    main.addClassName("elternsprechtag-view__summary-main");
    main.setText(
        getTranslation(
            "elternsprechtag.summary.row", lehrkraft.lehrerName(), Formats.time(slot.zeit())));

    Div sub = new Div();
    sub.addClassName("elternsprechtag-view__summary-sub");
    sub.setText(String.join(", ", lehrkraft.faecher()));
    info.add(main, sub);

    String text = session.notiz(lehrkraft.lehrauftragId());
    if (!text.isEmpty()) {
      Paragraph notiz = new Paragraph(text);
      notiz.addClassName("elternsprechtag-view__summary-notiz");
      info.add(notiz);
    }
    return info;
  }

  private void refreshFooter() {
    if (footerStatus == null) {
      return;
    }
    int count = session.auswahlAnzahl();
    bookButton.setEnabled(bookingValid());
    bookButton.setText(
        count == 0
            ? getTranslation("elternsprechtag.footer.button.empty")
            : getTranslation("elternsprechtag.footer.button", countLabel(count)));
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

  /** E-Mail ist Pflicht und muss dem Format genügen; {@link EmailField} prüft das Muster selbst. */
  private boolean emailValid() {
    return !elternEmail.getValue().isBlank() && !elternEmail.isInvalid();
  }

  private Component metaItem(VaadinIcon icon, String text) {
    Div item = new Div();
    item.addClassName("elternsprechtag-view__meta-item");
    item.add(icon.create(), new Span(text));
    return item;
  }

  private Component createMessage(String titleKey, String descriptionKey) {
    Div message = new Div();
    message.addClassName("elternsprechtag-view__message");
    H1 title = new H1(getTranslation(titleKey));
    title.addClassName("elternsprechtag-view__message-title");
    Paragraph description = new Paragraph(getTranslation(descriptionKey));
    description.addClassName("elternsprechtag-view__message-description");
    message.add(title, description);
    return message;
  }
}
