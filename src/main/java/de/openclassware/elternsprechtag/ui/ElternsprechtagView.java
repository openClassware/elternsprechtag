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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.ui.components.StepHeader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Route(value = ElternsprechtagView.ROUTE, autoLayout = false)
@AnonymousAllowed
@CssImport("./styles/elternsprechtag-view.css")
public class ElternsprechtagView extends Div implements HasUrlParameter<String> {

  public static final String ROUTE = "elternsprechtag";

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.GERMANY);
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final ElternsprechtagPresenter presenter;

  private TextField elternName;
  private TextField schuelerName;
  private Select<Klasse> klasse;
  private TextArea notiz;

  private Div footerStatus;
  private Button bookButton;

  /** Stub subject offering for the UI-only booking flow (no persistence yet). */
  private record Fachangebot(String shortName, String name, String teacher, Set<LocalTime> belegt) {}

  private enum SlotState {
    FREI,
    BELEGT,
    GEWAEHLT,
    KONFLIKT
  }

  private static final List<Fachangebot> STUB_FAECHER =
      List.of(
          new Fachangebot(
              "De", "Deutsch", "Fr. Berger", Set.of(LocalTime.of(15, 15), LocalTime.of(16, 45))),
          new Fachangebot(
              "Ma", "Mathematik", "Hr. Klein", Set.of(LocalTime.of(14, 0), LocalTime.of(17, 30))),
          new Fachangebot(
              "En", "Englisch", "Fr. Wagner", Set.of(LocalTime.of(14, 45), LocalTime.of(15, 30))),
          new Fachangebot(
              "Bi",
              "Biologie",
              "Hr. Ott",
              Set.of(
                  LocalTime.of(14, 30),
                  LocalTime.of(15, 15),
                  LocalTime.of(16, 45),
                  LocalTime.of(17, 45))),
          new Fachangebot(
              "Ge", "Geschichte", "Fr. Sommer", Set.of(LocalTime.of(16, 0), LocalTime.of(16, 15))),
          new Fachangebot(
              "Sp", "Sport", "Hr. Lang", Set.of(LocalTime.of(14, 15), LocalTime.of(17, 0))));

  private final Map<Fachangebot, LocalTime> selection = new LinkedHashMap<>();
  private Fachangebot activeFach;
  private List<LocalTime> slotTimes = List.of();

  private Div fachGrid;
  private Div slotArea;
  private Div summaryContainer;

  public ElternsprechtagView(ElternsprechtagPresenter presenter) {
    this.presenter = presenter;
    addClassName("elternsprechtag-view");
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String token) {
    removeAll();
    Optional<Sprechtag> sprechtag = presenter.findByAccessToken(token);
    if (sprechtag.isPresent() && sprechtag.get().getStatus() == SprechtagStatusEnum.VEROEFFENTLICHT) {
      Sprechtag published = sprechtag.get();
      add(
          createHeader(),
          createInfo(published),
          createAngaben(published),
          createBuchung(published),
          createNotiz());
    } else if (sprechtag.isPresent() && sprechtag.get().getStatus() == SprechtagStatusEnum.ABGESAGT) {
      add(createMessage("elternsprechtag.cancelled.title", "elternsprechtag.cancelled.description"));
    } else {
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

  private Component createInfo(Sprechtag sprechtag) {
    Div card = new Div();
    card.addClassName("elternsprechtag-view__card");

    H1 title = new H1(sprechtag.getTitel());
    title.addClassName("elternsprechtag-view__title");
    card.add(title);

    Div meta = new Div();
    meta.addClassName("elternsprechtag-view__meta");
    meta.add(
        metaItem(VaadinIcon.CALENDAR, sprechtag.getStartDate().format(DATE_FORMATTER)),
        metaItem(
            VaadinIcon.CLOCK,
            sprechtag.getStartTime().format(TIME_FORMATTER)
                + "–"
                + sprechtag.getEndTime().format(TIME_FORMATTER)));
    if (sprechtag.getLocation() != null && !sprechtag.getLocation().isBlank()) {
      meta.add(metaItem(VaadinIcon.MAP_MARKER, sprechtag.getLocation()));
    }
    card.add(meta);

    Paragraph intro = new Paragraph(getTranslation("elternsprechtag.intro"));
    intro.addClassName("elternsprechtag-view__intro");
    card.add(intro);

    if (sprechtag.getDescription() != null && !sprechtag.getDescription().isBlank()) {
      Paragraph description = new Paragraph(sprechtag.getDescription());
      description.addClassName("elternsprechtag-view__description");
      card.add(description);
    }

    return card;
  }

  private Component createAngaben(Sprechtag sprechtag) {
    Div card = new Div();
    card.addClassName("elternsprechtag-view__card");
    card.add(new StepHeader(1, getTranslation("elternsprechtag.angaben.step-title")));

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
    klasse.setItemLabelGenerator(Klasse::getName);
    klasse.setItems(
        sprechtag.getKlassen().stream()
            .sorted(Comparator.comparing(Klasse::getName))
            .toList());
    klasse.setRequiredIndicatorVisible(true);
    klasse.addValueChangeListener(event -> refreshFooter());

    FormLayout form = new FormLayout();
    form.addClassName("elternsprechtag-view__form");
    form.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("640px", 2));
    form.add(elternName, schuelerName, klasse);

    card.add(form);
    return card;
  }

  private Component createBuchung(Sprechtag sprechtag) {
    selection.clear();
    activeFach = null;
    slotTimes = computeSlotTimes(sprechtag);

    Div card = new Div();
    card.addClassName("elternsprechtag-view__card");

    card.add(new StepHeader(2, getTranslation("elternsprechtag.fach.step-title")));
    fachGrid = new Div();
    fachGrid.addClassName("elternsprechtag-view__fach-grid");
    card.add(fachGrid);

    Div step3Head = new Div();
    step3Head.addClassName("elternsprechtag-view__step3-head");
    step3Head.add(
        new StepHeader(3, getTranslation("elternsprechtag.termin.step-title")), createLegend());
    card.add(step3Head);

    slotArea = new Div();
    slotArea.addClassName("elternsprechtag-view__slot-area");
    card.add(slotArea);

    Paragraph hint =
        new Paragraph(
            getTranslation("elternsprechtag.termin.hint", sprechtag.getSlotInMinutes()));
    hint.addClassName("elternsprechtag-view__slot-hint");
    card.add(hint);

    summaryContainer = new Div();
    card.add(summaryContainer);

    refreshFachGrid();
    refreshSlotArea();
    refreshSummary();
    return card;
  }

  private List<LocalTime> computeSlotTimes(Sprechtag sprechtag) {
    List<LocalTime> times = new ArrayList<>();
    LocalTime time = sprechtag.getStartTime();
    while (time.isBefore(sprechtag.getEndTime())) {
      times.add(time);
      time = time.plusMinutes(sprechtag.getSlotInMinutes());
    }
    return times;
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

  private void refreshFachGrid() {
    fachGrid.removeAll();
    STUB_FAECHER.forEach(fach -> fachGrid.add(createFachCard(fach)));
  }

  private Component createFachCard(Fachangebot fach) {
    Div fachCard = new Div();
    fachCard.addClassName("elternsprechtag-view__fach");
    if (fach.equals(activeFach) || selection.containsKey(fach)) {
      fachCard.addClassName("elternsprechtag-view__fach--selected");
    }

    Span badge = new Span(fach.shortName());
    badge.addClassName("elternsprechtag-view__fach-badge");

    Div info = new Div();
    info.addClassName("elternsprechtag-view__fach-info");
    Div name = new Div();
    name.addClassName("elternsprechtag-view__fach-name");
    name.setText(fach.name());
    Div teacher = new Div();
    teacher.addClassName("elternsprechtag-view__fach-teacher");
    teacher.setText(fach.teacher());
    info.add(name, teacher);

    fachCard.add(badge, info);

    LocalTime chosen = selection.get(fach);
    if (chosen != null) {
      Span pill = new Span();
      pill.addClassName("elternsprechtag-view__fach-pill");
      pill.add(VaadinIcon.CHECK.create(), new Span(chosen.format(TIME_FORMATTER)));
      fachCard.add(pill);
    }

    fachCard.addClickListener(
        event -> {
          activeFach = fach;
          refreshFachGrid();
          refreshSlotArea();
        });
    return fachCard;
  }

  private void refreshSlotArea() {
    slotArea.removeAll();
    if (activeFach == null) {
      Div placeholder = new Div();
      placeholder.addClassName("elternsprechtag-view__placeholder");
      placeholder.setText(getTranslation("elternsprechtag.termin.placeholder"));
      slotArea.add(placeholder);
      return;
    }
    Div grid = new Div();
    grid.addClassName("elternsprechtag-view__slot-grid");
    slotTimes.forEach(time -> grid.add(createSlot(time)));
    slotArea.add(grid);
  }

  private Component createSlot(LocalTime time) {
    Div slot = new Div();
    slot.addClassName("elternsprechtag-view__slot");
    slot.add(new Span(time.format(TIME_FORMATTER)));

    switch (slotState(time)) {
      case BELEGT -> slot.addClassName("elternsprechtag-view__slot--belegt");
      case KONFLIKT -> slot.addClassName("elternsprechtag-view__slot--konflikt");
      case GEWAEHLT -> {
        slot.addClassName("elternsprechtag-view__slot--selected");
        Span check = new Span();
        check.addClassName("elternsprechtag-view__slot-check");
        check.add(VaadinIcon.CHECK.create());
        slot.add(check);
        slot.addClickListener(event -> deselectSlot());
      }
      case FREI -> slot.addClickListener(event -> selectSlot(time));
    }
    return slot;
  }

  private SlotState slotState(LocalTime time) {
    if (activeFach.belegt().contains(time)) {
      return SlotState.BELEGT;
    }
    if (time.equals(selection.get(activeFach))) {
      return SlotState.GEWAEHLT;
    }
    for (Map.Entry<Fachangebot, LocalTime> entry : selection.entrySet()) {
      if (!entry.getKey().equals(activeFach) && entry.getValue().equals(time)) {
        return SlotState.KONFLIKT;
      }
    }
    return SlotState.FREI;
  }

  private void selectSlot(LocalTime time) {
    selection.put(activeFach, time);
    refreshAfterSelectionChange();
  }

  private void deselectSlot() {
    selection.remove(activeFach);
    refreshAfterSelectionChange();
  }

  private void refreshAfterSelectionChange() {
    refreshFachGrid();
    refreshSlotArea();
    refreshSummary();
    refreshFooter();
  }

  private void refreshSummary() {
    summaryContainer.removeAll();
    if (selection.isEmpty()) {
      return;
    }

    Div panel = new Div();
    panel.addClassName("elternsprechtag-view__summary");

    Div head = new Div();
    head.addClassName("elternsprechtag-view__summary-head");
    Span title = new Span(getTranslation("elternsprechtag.summary.title"));
    title.addClassName("elternsprechtag-view__summary-title");
    Span count = new Span(countLabel(selection.size()));
    count.addClassName("elternsprechtag-view__summary-count");
    head.add(title, count);
    panel.add(head);

    selection.forEach((fach, time) -> panel.add(createSummaryRow(fach, time)));
    summaryContainer.add(panel);
  }

  private String countLabel(int count) {
    return count == 1
        ? getTranslation("elternsprechtag.summary.count.one")
        : getTranslation("elternsprechtag.summary.count.other", count);
  }

  private Component createSummaryRow(Fachangebot fach, LocalTime time) {
    Div row = new Div();
    row.addClassName("elternsprechtag-view__summary-row");

    Span badge = new Span(fach.shortName());
    badge.addClassName("elternsprechtag-view__summary-badge");

    Div info = new Div();
    info.addClassName("elternsprechtag-view__summary-info");
    Div main = new Div();
    main.addClassName("elternsprechtag-view__summary-main");
    main.setText(
        getTranslation("elternsprechtag.summary.row", fach.name(), time.format(TIME_FORMATTER)));
    Div sub = new Div();
    sub.addClassName("elternsprechtag-view__summary-sub");
    sub.setText(fach.teacher());
    info.add(main, sub);

    Button remove = new Button(VaadinIcon.CLOSE_SMALL.create());
    remove.addClassName("elternsprechtag-view__summary-remove");
    remove.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
    remove.addClickListener(
        event -> {
          selection.remove(fach);
          refreshAfterSelectionChange();
        });

    row.add(badge, info, remove);
    return row;
  }

  private Component createNotiz() {
    Div card = new Div();
    card.addClassName("elternsprechtag-view__notiz");

    Div body = new Div();
    body.addClassName("elternsprechtag-view__notiz-body");
    body.add(
        new StepHeader(
            4,
            getTranslation("elternsprechtag.notiz.step-title"),
            getTranslation("elternsprechtag.notiz.optional")));

    notiz = new TextArea();
    notiz.addClassName("elternsprechtag-view__notiz-field");
    notiz.setPlaceholder(getTranslation("elternsprechtag.notiz.placeholder"));
    notiz.setWidthFull();
    notiz.setMaxLength(500);
    body.add(notiz);

    card.add(body, createFooter());
    refreshFooter();
    return card;
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
    bookButton.addClickListener(
        event -> Notification.show(getTranslation("elternsprechtag.footer.notification")));

    footer.add(footerStatus, bookButton);
    return footer;
  }

  private void refreshFooter() {
    if (footerStatus == null) {
      return;
    }
    int count = selection.size();
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
    return !selection.isEmpty() && namesFilled() && klasse.getValue() != null;
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
