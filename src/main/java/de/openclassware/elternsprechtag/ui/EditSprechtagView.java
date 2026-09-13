package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl.KlasseOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import de.openclassware.elternsprechtag.ui.components.Breadcrumb;
import de.openclassware.elternsprechtag.ui.components.FormPanel;
import de.openclassware.elternsprechtag.ui.layouts.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Route(value = EditSprechtagView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/edit-sprechtag-view.css")
public class EditSprechtagView extends Div implements HasUrlParameter<String> {

  public static final String ROUTE = "sprechtag";

  private final EditSprechtagPresenter presenter;
  private final Binder<SprechtagFormular> binder = new Binder<>(SprechtagFormular.class);

  private UUID editingId;
  private Breadcrumb breadcrumb;
  private H2 headerTitle;
  private Button createButton;
  private Button draftButton;

  private TextField titel;
  private TextField location;
  private TextArea description;
  private TextArea schulkontakt;
  private DatePicker datePicker;
  private ComboBox<Integer> slotInMinutes;
  private TimePicker startTime;
  private TimePicker endTime;
  private CheckboxGroup<KlasseOption> klassen;
  private TextField accessToken;
  private TextField shareLink;
  private String origin;

  /** Namen zu Ids — das, was der Konverter zwischen Auswahl und Formular braucht. */
  private final Map<UUID, KlasseOption> klassenById = new LinkedHashMap<>();

  EditSprechtagView(EditSprechtagPresenter presenter) {
    this.presenter = presenter;
    addClassName("edit-sprechtag-view");
    add(
        createHeader(),
        createGeneralInfoPanel(),
        createTimingPanel(),
        createClassesPanel(),
        createAccessTokenPanel(),
        createBottomButtonBar());
    configureBinder();
  }

  private void configureBinder() {
    binder
        .forField(titel)
        .asRequired(getTranslation("edit-sprechtag.validation.titel-required"))
        .bind(SprechtagFormular::getTitel, SprechtagFormular::setTitel);
    binder.forField(location).bind(SprechtagFormular::getOrt, SprechtagFormular::setOrt);
    binder
        .forField(description)
        .bind(SprechtagFormular::getBeschreibung, SprechtagFormular::setBeschreibung);
    binder
        .forField(schulkontakt)
        .asRequired(getTranslation("edit-sprechtag.validation.schulkontakt-required"))
        .bind(SprechtagFormular::getSchulkontakt, SprechtagFormular::setSchulkontakt);
    binder
        .forField(datePicker)
        .asRequired(getTranslation("edit-sprechtag.validation.datum-required"))
        .bind(SprechtagFormular::getDatum, SprechtagFormular::setDatum);
    binder
        .forField(startTime)
        .asRequired(getTranslation("edit-sprechtag.validation.startzeit-required"))
        .bind(SprechtagFormular::getBeginn, SprechtagFormular::setBeginn);
    binder
        .forField(endTime)
        .asRequired(getTranslation("edit-sprechtag.validation.endzeit-required"))
        .bind(SprechtagFormular::getEnde, SprechtagFormular::setEnde);

    // Pflichtfeld: Ohne Slot-Dauer lassen sich keine Termine bilden (`ABDECKUNG.md` Z. 95). Die
    // Auswahl lässt sich leeren, und ohne diese Zeile lief das Veröffentlichen in eine
    // NullPointerException.
    binder
        .forField(slotInMinutes)
        .asRequired(getTranslation("edit-sprechtag.validation.slot-required"))
        .bind(SprechtagFormular::getSlotInMinuten, SprechtagFormular::setSlotInMinuten);

    binder
        .forField(accessToken)
        .bind(SprechtagFormular::getAccessToken, SprechtagFormular::setAccessToken);

    // Die Oberfläche wählt Klassen als Optionen, das Formular trägt ihre Ids: Die Namen gehören der
    // Schulorganisation und haben im Sprechtag nichts zu suchen.
    binder
        .forField(klassen)
        .withValidator(
            selected -> selected != null && !selected.isEmpty(),
            getTranslation("edit-sprechtag.validation.klasse-required"))
        .withConverter(this::zuIds, this::zuOptionen)
        .bind(SprechtagFormular::getKlasseIds, SprechtagFormular::setKlasseIds);

    binder.withValidator(
        form ->
            form.getBeginn() == null
                || form.getEnde() == null
                || form.getEnde().isAfter(form.getBeginn()),
        getTranslation("edit-sprechtag.validation.end-after-start"));
  }

  private Set<UUID> zuIds(Set<KlasseOption> auswahl) {
    return auswahl.stream().map(KlasseOption::id).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Füllt die Klassen-Auswahl. Welche Klassen darin stehen, entscheidet der Use Case — er nimmt die
   * bereits gewählten mit auf, auch stillgelegte.
   */
  private void zeigeKlassen(Set<UUID> bereitsGewaehlt) {
    List<KlasseOption> optionen =
        presenter.waehlbareKlassen(bereitsGewaehlt == null ? Set.of() : bereitsGewaehlt);
    klassenById.clear();
    optionen.forEach(option -> klassenById.put(option.id(), option));
    klassen.setItems(optionen);
  }

  /**
   * Eine Klasse, die es gar nicht mehr gibt, fällt aus der Auswahl. Das bleibt der einzige Fall:
   * Stillgelegte Klassen bietet {@link #zeigeKlassen(Set)} weiterhin an, sonst nähme das Formular
   * sie dem Sprechtag beim nächsten Speichern kommentarlos weg.
   */
  private Set<KlasseOption> zuOptionen(Set<UUID> ids) {
    return ids.stream()
        .map(klassenById::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String sprechtagId) {
    if (sprechtagId == null) {
      return; // Anlege-Modus
    }
    Optional<UUID> id = parseId(sprechtagId);
    Optional<SprechtagFormular> form = id.flatMap(presenter::loadForm);
    if (form.isEmpty()) {
      event.rerouteToError(NotFoundException.class, "Sprechtag not found: " + sprechtagId);
      return;
    }
    editingId = id.get();
    // Vor dem Lesen, nicht danach: Der Konverter unten schlägt die Ids in `klassenById` nach, und
    // was dort fehlt, fiele beim nächsten Speichern still aus dem Sprechtag.
    zeigeKlassen(form.get().getKlasseIds());
    binder.readBean(form.get());
    if (form.get().isZeitstrukturEingefroren()) {
      sperreZeitstruktur();
    }
    breadcrumb.setCurrentText(getTranslation("edit-sprechtag.breadcrumb.title-edit"));
    headerTitle.setText(getTranslation("edit-sprechtag.header.title-edit"));
    // Im Bearbeiten-Modus speichert der Hauptknopf nur — ein Statuswechsel läuft über die Verwaltung
    // und ihre eigenen Wege. Dass „Speichern" früher zugleich veröffentlichte, war der Weg, auf dem
    // sich ein abgesagter Sprechtag wiederbeleben ließ (`ABDECKUNG.md` Z. 93).
    createButton.setText(getTranslation("edit-sprechtag.button.save"));
    draftButton.setVisible(false);
  }

  /**
   * Nimmt Datum, Zeitfenster, Slot-Dauer und Klassen aus der Eingabe und schreibt den Grund daneben
   * (`ABDECKUNG.md` Z. 87–89). Die Weigerung des Aggregats beim Speichern bleibt trotzdem bestehen —
   * sie ist die Wahrheit, das hier ist die Höflichkeit: Der Organizer soll gar nicht erst tippen,
   * was ohnehin abgewiesen würde.
   */
  private void sperreZeitstruktur() {
    datePicker.setReadOnly(true);
    startTime.setReadOnly(true);
    endTime.setReadOnly(true);
    slotInMinutes.setReadOnly(true);
    klassen.setReadOnly(true);
    datePicker.setHelperText(getTranslation("edit-sprechtag.timing.eingefroren"));
    klassen.setHelperText(getTranslation("edit-sprechtag.timing.eingefroren"));
  }

  private Optional<UUID> parseId(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  /** Speichert, ohne den Status anzurühren: anlegen als Entwurf oder zurückschreiben. */
  private void speichere() {
    SprechtagFormular form = new SprechtagFormular();
    if (!binder.writeBeanIfValid(form)) {
      return;
    }
    try {
      presenter.speichere(editingId, form);
      navigateToOrganizerView();
    } catch (RuntimeException fehler) {
      SprechtagMeldungen.zeige(this, SprechtagMeldungen.zu(fehler));
    }
  }

  /** Legt an und veröffentlicht in einem Zug — nur im Anlege-Modus erreichbar. */
  private void legeAnUndVeroeffentliche() {
    SprechtagFormular form = new SprechtagFormular();
    if (!binder.writeBeanIfValid(form)) {
      return;
    }
    if (presenter.legeAnUndVeroeffentliche(form).ohneTermine()) {
      SprechtagMeldungen.zeige(this, SprechtagMeldungen.ohneTermine());
    }
    navigateToOrganizerView();
  }

  private void navigateToOrganizerView() {
    getUI().ifPresent(ui -> ui.navigate(OrganizerView.ROUTE));
  }

  private Component createAccessTokenPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle(getTranslation("edit-sprechtag.zugang.title"));
    panel.setDescription(getTranslation("edit-sprechtag.zugang.description"));
    FormLayout formLayout = panel.getFormLayout();
    accessToken = new TextField();
    accessToken.setLabel(getTranslation("edit-sprechtag.field.zugangscode.label"));
    accessToken.setReadOnly(true);
    accessToken.setHelperText(getTranslation("edit-sprechtag.field.zugangscode.helper"));
    accessToken.setValue(UUID.randomUUID().toString());

    FormRow firstRow = new FormRow();
    firstRow.add(accessToken, 3);
    Button regenerateAccessTokenButton = new Button();
    regenerateAccessTokenButton.setIcon(VaadinIcon.REFRESH.create());
    regenerateAccessTokenButton.setText(getTranslation("edit-sprechtag.button.regenerate"));
    regenerateAccessTokenButton.addClickListener(
        _ -> {
          accessToken.setValue(UUID.randomUUID().toString());
          updateShareLink();
        });
    firstRow.add(regenerateAccessTokenButton, 1);

    shareLink = new TextField();
    shareLink.setLabel(getTranslation("edit-sprechtag.field.link.label"));
    shareLink.setReadOnly(true);
    shareLink.setHelperText(getTranslation("edit-sprechtag.field.link.helper"));

    FormRow secondRow = new FormRow();
    secondRow.add(shareLink, 3);
    Button copyLinkButton = new Button();
    copyLinkButton.setIcon(VaadinIcon.COPY.create());
    copyLinkButton.setText(getTranslation("edit-sprechtag.button.copy-link"));
    copyLinkButton.addClickListener(
        _ ->
            getUI()
                .ifPresent(
                    ui ->
                        ui.getPage()
                            .executeJs("navigator.clipboard.writeText($0)", shareLink.getValue())));
    secondRow.add(copyLinkButton, 1);

    formLayout.add(firstRow, secondRow);
    return panel;
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    attachEvent
        .getUI()
        .getPage()
        .executeJs("return window.location.origin")
        .then(
            String.class,
            fetchedOrigin -> {
              this.origin = fetchedOrigin;
              updateShareLink();
            });
  }

  private void updateShareLink() {
    if (origin != null && shareLink != null) {
      shareLink.setValue(origin + "/" + ElternsprechtagView.ROUTE + "/" + accessToken.getValue());
    }
  }

  private Component createClassesPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle(getTranslation("edit-sprechtag.klassen.title"));
    panel.setDescription(getTranslation("edit-sprechtag.klassen.description"));

    klassen = new CheckboxGroup<>();
    zeigeKlassen(Set.of());
    klassen.setHelperText(getTranslation("edit-sprechtag.klassen.helper", 0));
    klassen.addThemeVariants(CheckboxGroupVariant.AURA_HORIZONTAL);
    klassen.setRenderer(new TextRenderer<>(KlasseOption::name));
    klassen.addValueChangeListener(
        e ->
            klassen.setHelperText(
                getTranslation("edit-sprechtag.klassen.helper", e.getValue().size())));
    panel.getFormLayout().add(klassen);

    return panel;
  }

  private Component createGeneralInfoPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle(getTranslation("edit-sprechtag.general.title"));
    panel.setDescription(getTranslation("edit-sprechtag.general.description"));

    FormRow firstRow = new FormRow();
    titel = new TextField();
    titel.setLabel(getTranslation("edit-sprechtag.field.titel.label"));
    titel.setRequiredIndicatorVisible(true);
    titel.setSizeFull();
    titel.setPlaceholder(getTranslation("edit-sprechtag.field.titel.placeholder"));

    firstRow.add(titel, 2);

    FormRow secondRow = new FormRow();
    location = new TextField();
    location.setLabel(getTranslation("edit-sprechtag.field.ort.label"));
    location.setPlaceholder(getTranslation("edit-sprechtag.field.ort.placeholder"));
    secondRow.add(location, 2);

    FormRow thirdRow = new FormRow();
    description = new TextArea();
    description.setLabel(getTranslation("edit-sprechtag.field.beschreibung.label"));
    description.setPlaceholder(getTranslation("edit-sprechtag.field.beschreibung.placeholder"));
    description.setMinRows(3);
    thirdRow.add(description, 2);

    FormRow fourthRow = new FormRow();
    schulkontakt = new TextArea();
    schulkontakt.setLabel(getTranslation("edit-sprechtag.field.schulkontakt.label"));
    schulkontakt.setPlaceholder(getTranslation("edit-sprechtag.field.schulkontakt.placeholder"));
    schulkontakt.setHelperText(getTranslation("edit-sprechtag.field.schulkontakt.helper"));
    schulkontakt.setRequiredIndicatorVisible(true);
    schulkontakt.setMinRows(3);
    schulkontakt.setMaxLength(1000);
    fourthRow.add(schulkontakt, 2);

    FormLayout formLayout = panel.getFormLayout();
    formLayout.add(firstRow, secondRow, thirdRow, fourthRow);

    return panel;
  }

  private Component createTimingPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle(getTranslation("edit-sprechtag.timing.title"));
    panel.setDescription(getTranslation("edit-sprechtag.timing.description"));
    FormLayout formLayout = panel.getFormLayout();
    FormRow firstRow = new FormRow();

    datePicker = new DatePicker();
    datePicker.setLabel(getTranslation("edit-sprechtag.field.datum.label"));
    datePicker.setRequiredIndicatorVisible(true);

    slotInMinutes = new ComboBox<>();
    slotInMinutes.setLabel(getTranslation("edit-sprechtag.field.slot.label"));
    slotInMinutes.setItems(5, 10, 15, 20, 25, 30);
    slotInMinutes.setValue(15);
    slotInMinutes.setRenderer(
        new TextRenderer<>(minutes -> getTranslation("edit-sprechtag.slot.item", minutes)));

    firstRow.add(datePicker, slotInMinutes);

    startTime = new TimePicker();
    startTime.setLabel(getTranslation("edit-sprechtag.field.startzeit.label"));
    startTime.setRequiredIndicatorVisible(true);

    endTime = new TimePicker();
    endTime.setLabel(getTranslation("edit-sprechtag.field.endzeit.label"));
    endTime.setRequiredIndicatorVisible(true);

    FormRow secondRow = new FormRow();
    secondRow.add(startTime, endTime);

    formLayout.add(firstRow, secondRow);
    return panel;
  }

  private Div createHeader() {
    Div header = new Div();
    header.addClassName("edit-sprechtag-view__header");
    breadcrumb = new Breadcrumb();
    breadcrumb.addLink(getTranslation("breadcrumb.uebersicht"), OrganizerView.class);
    breadcrumb.addCurrent(getTranslation("edit-sprechtag.breadcrumb.title"));
    headerTitle = new H2(getTranslation("edit-sprechtag.header.title"));
    header.add(breadcrumb, headerTitle);
    return header;
  }

  private Div createBottomButtonBar() {
    Div buttonBar = new Div();
    buttonBar.addClassName("edit-sprechtag-view__bottom-button-bar");
    buttonBar.add(createCancelButton(), createSaveAsDraftButton(), createCreateButton());
    return buttonBar;
  }

  private Button createCreateButton() {
    createButton = new Button();
    createButton.setText(getTranslation("edit-sprechtag.button.create"));
    createButton.setIcon(VaadinIcon.CHECK.create());
    createButton.setThemeVariants(ButtonVariant.PRIMARY);
    // Im Anlege-Modus veröffentlicht dieser Knopf; im Bearbeiten-Modus speichert er nur (siehe
    // setParameter). Beide Fälle stehen hier zusammen, weil es derselbe Knopf ist.
    createButton.addClickListener(
        _ -> {
          if (editingId == null) {
            legeAnUndVeroeffentliche();
          } else {
            speichere();
          }
        });
    return createButton;
  }

  private Button createSaveAsDraftButton() {
    draftButton = new Button();
    draftButton.setText(getTranslation("edit-sprechtag.button.draft"));
    draftButton.addClassName("edit-sprechtag-view__draft-button");
    draftButton.addClickListener(_ -> speichere());
    return draftButton;
  }

  private Button createCancelButton() {
    Button button = new Button();
    button.setText(getTranslation("edit-sprechtag.button.cancel"));
    button.setThemeVariants(ButtonVariant.TERTIARY);
    button.addClickListener(_ -> navigateToOrganizerView());
    return button;
  }
}
