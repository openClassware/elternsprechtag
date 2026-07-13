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

  private final ElternsprechtagPresenter presenter;

  private SprechtagPublic sprechtag;

  private TextField elternName;
  private TextField schuelerName;
  private Select<KlasseOption> klasse;
  private TextArea notiz;

  private Div footerStatus;
  private Button bookButton;

  /** Buchungs-„Warenkorb" + Entscheidungslogik; Vaadin-frei und unit-getestet. */
  private final BookingSession session = new BookingSession();

  private Div lehrkraftGrid;
  private Div slotArea;
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
    body.add(createAngaben(sprechtag), createBuchung(sprechtag), createNotiz());

    card.add(createInfo(sprechtag), body, createFooter());

    refreshLehrkraftGrid();
    refreshSlotArea();
    refreshSummary();
    refreshFooter();
    return card;
  }

  private Component createInfo(SprechtagPublic sprechtag) {
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

    Paragraph intro = new Paragraph(getTranslation("elternsprechtag.intro"));
    intro.addClassName("elternsprechtag-view__intro");
    kopf.add(intro);

    if (sprechtag.description() != null && !sprechtag.description().isBlank()) {
      Paragraph description = new Paragraph(sprechtag.description());
      description.addClassName("elternsprechtag-view__description");
      kopf.add(description);
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
    form.add(elternName, schuelerName, klasse);

    section.add(form);
    return section;
  }

  private Component createBuchung(SprechtagPublic sprechtag) {
    Div section = new Div();
    section.addClassName("elternsprechtag-view__section");

    section.add(new StepHeader(2, getTranslation("elternsprechtag.lehrkraft.step-title")));
    lehrkraftGrid = new Div();
    lehrkraftGrid.addClassName("elternsprechtag-view__lehrkraft-grid");
    section.add(lehrkraftGrid);

    Div step3Head = new Div();
    step3Head.addClassName("elternsprechtag-view__step3-head");
    step3Head.add(
        new StepHeader(3, getTranslation("elternsprechtag.termin.step-title")), createLegend());
    section.add(step3Head);

    slotArea = new Div();
    slotArea.addClassName("elternsprechtag-view__slot-area");
    section.add(slotArea);

    Paragraph hint =
        new Paragraph(getTranslation("elternsprechtag.termin.hint", sprechtag.slotInMinutes()));
    hint.addClassName("elternsprechtag-view__slot-hint");
    section.add(hint);

    summaryContainer = new Div();
    section.add(summaryContainer);

    return section;
  }

  /** Reagiert auf die Klassenwahl: lädt die echten Lehrkräfte/Termine und verwirft die Auswahl. */
  private void onKlasseChanged() {
    session.reset(loadOptionen());
    refreshLehrkraftGrid();
    refreshSlotArea();
    refreshSummary();
    refreshFooter();
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

  private void refreshLehrkraftGrid() {
    lehrkraftGrid.removeAll();
    if (!session.hatOptionen()) {
      Div placeholder = new Div();
      placeholder.addClassName("elternsprechtag-view__placeholder");
      placeholder.setText(getTranslation("elternsprechtag.lehrkraft.placeholder"));
      lehrkraftGrid.add(placeholder);
      return;
    }
    session.optionen().forEach(lehrkraft -> lehrkraftGrid.add(createLehrkraftCard(lehrkraft)));
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
          refreshLehrkraftGrid();
          refreshSlotArea();
        });
    return card;
  }

  private void refreshSlotArea() {
    slotArea.removeAll();
    LehrkraftOption active = session.active();
    if (active == null) {
      Div placeholder = new Div();
      placeholder.addClassName("elternsprechtag-view__placeholder");
      placeholder.setText(getTranslation("elternsprechtag.termin.placeholder"));
      slotArea.add(placeholder);
      return;
    }
    if (active.slots().isEmpty()) {
      Div placeholder = new Div();
      placeholder.addClassName("elternsprechtag-view__placeholder");
      placeholder.setText(getTranslation("elternsprechtag.termin.empty"));
      slotArea.add(placeholder);
      return;
    }
    Div grid = new Div();
    grid.addClassName("elternsprechtag-view__slot-grid");
    active.slots().forEach(slot -> grid.add(createSlot(slot)));
    slotArea.add(grid);
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
    refreshLehrkraftGrid();
    refreshSlotArea();
    refreshSummary();
    refreshFooter();
  }

  private void refreshSummary() {
    summaryContainer.removeAll();
    if (!session.hatAuswahl()) {
      return;
    }

    Div panel = new Div();
    panel.addClassName("elternsprechtag-view__summary");

    Div head = new Div();
    head.addClassName("elternsprechtag-view__summary-head");
    Span title = new Span(getTranslation("elternsprechtag.summary.title"));
    title.addClassName("elternsprechtag-view__summary-title");
    Span count = new Span(countLabel(session.auswahlAnzahl()));
    count.addClassName("elternsprechtag-view__summary-count");
    head.add(title, count);
    panel.add(head);

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

    Span badge = new Span(lehrkraft.kuerzel());
    badge.addClassName("elternsprechtag-view__summary-badge");

    Div info = new Div();
    info.addClassName("elternsprechtag-view__summary-info");
    Div main = new Div();
    main.addClassName("elternsprechtag-view__summary-main");
    main.setText(
        getTranslation(
            "elternsprechtag.summary.row",
            lehrkraft.lehrerName(),
            Formats.time(slot.zeit())));
    Div sub = new Div();
    sub.addClassName("elternsprechtag-view__summary-sub");
    sub.setText(String.join(", ", lehrkraft.faecher()));
    info.add(main, sub);

    Button remove = new Button(VaadinIcon.CLOSE_SMALL.create());
    remove.addClassName("elternsprechtag-view__summary-remove");
    remove.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
    remove.addClickListener(
        event -> {
          session.entferne(lehrkraft.lehrauftragId());
          refreshAfterSelectionChange();
        });

    row.add(badge, info, remove);
    return row;
  }

  private Component createNotiz() {
    Div section = new Div();
    section.addClassName("elternsprechtag-view__section");
    section.add(
        new StepHeader(
            4,
            getTranslation("elternsprechtag.notiz.step-title"),
            getTranslation("elternsprechtag.notiz.optional")));

    notiz = new TextArea();
    notiz.addClassName("elternsprechtag-view__notiz-field");
    notiz.setPlaceholder(getTranslation("elternsprechtag.notiz.placeholder"));
    notiz.setWidthFull();
    notiz.setMaxLength(500);
    section.add(notiz);

    return section;
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
    String notizText =
        notiz.getValue() == null || notiz.getValue().isBlank() ? null : notiz.getValue().trim();
    BuchungsAnfrage anfrage =
        new BuchungsAnfrage(
            elternName.getValue().trim(),
            schuelerName.getValue().trim(),
            notizText,
            session.toWuensche());

    try {
      int gebucht = presenter.buchen(anfrage).size();
      showConfirmation(gebucht);
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
    refreshLehrkraftGrid();
    refreshSlotArea();
    refreshSummary();
    refreshFooter();
  }

  private void showConfirmation(int count) {
    removeAll();
    Div message = new Div();
    message.addClassName("elternsprechtag-view__message");
    H1 title = new H1(getTranslation("elternsprechtag.success.title"));
    title.addClassName("elternsprechtag-view__message-title");
    Paragraph description =
        new Paragraph(getTranslation("elternsprechtag.success.description", countLabel(count)));
    description.addClassName("elternsprechtag-view__message-description");
    message.add(title, description);
    add(createHeader(), message);
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
    if (klasse.getValue() == null) {
      return selected + " — " + getTranslation("elternsprechtag.footer.blocker.klasse");
    }
    return selected;
  }

  private boolean bookingValid() {
    return session.hatAuswahl() && namesFilled() && klasse.getValue() != null;
  }

  private boolean namesFilled() {
    return !elternName.getValue().isBlank() && !schuelerName.getValue().isBlank();
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
