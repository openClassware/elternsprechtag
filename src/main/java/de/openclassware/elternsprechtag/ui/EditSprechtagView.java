package de.openclassware.elternsprechtag.ui;

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
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.ui.components.FormPanel;
import de.openclassware.elternsprechtag.ui.layouts.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Route(value = EditSprechtagView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/edit-sprechtag-view.css")
public class EditSprechtagView extends Div {

  public static final String ROUTE = "sprechtag";

  private final EditSprechtagPresenter presenter;
  private final Binder<Sprechtag> binder = new Binder<>(Sprechtag.class);

  private TextField titel;
  private TextField location;
  private TextArea description;
  private DatePicker datePicker;
  private ComboBox<Integer> slotInMinutes;
  private TimePicker startTime;
  private TimePicker endTime;
  private CheckboxGroup<Klasse> klassen;
  private TextField accessToken;

  public EditSprechtagView(EditSprechtagPresenter presenter) {
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
        .asRequired("Titel ist erforderlich")
        .bind(Sprechtag::getTitel, Sprechtag::setTitel);
    binder.forField(location).bind(Sprechtag::getLocation, Sprechtag::setLocation);
    binder.forField(description).bind(Sprechtag::getDescription, Sprechtag::setDescription);
    binder
        .forField(datePicker)
        .asRequired("Datum ist erforderlich")
        .bind(Sprechtag::getStartDate, Sprechtag::setStartDate);
    binder
        .forField(startTime)
        .asRequired("Startzeit ist erforderlich")
        .bind(Sprechtag::getStartTime, Sprechtag::setStartTime);
    binder
        .forField(endTime)
        .asRequired("Endzeit ist erforderlich")
        .bind(Sprechtag::getEndTime, Sprechtag::setEndTime);

    binder.forField(slotInMinutes).bind(Sprechtag::getSlotInMinutes, Sprechtag::setSlotInMinutes);

    binder.forField(accessToken).bind(Sprechtag::getAccessToken, Sprechtag::setAccessToken);

    binder
        .forField(klassen)
        .withValidator(
            klassen -> klassen != null && !klassen.isEmpty(), "Mindestens eine Klasse auswählen")
        .withConverter(set -> (List<Klasse>) new ArrayList<>(set), LinkedHashSet::new)
        .bind(Sprechtag::getKlassen, Sprechtag::setKlassen);

    binder.withValidator(
        sprechtag ->
            sprechtag.getStartTime() == null
                || sprechtag.getEndTime() == null
                || sprechtag.getEndTime().isAfter(sprechtag.getStartTime()),
        "Endzeit muss nach der Startzeit liegen");
  }

  private void save(SprechtagStatusEnum status) {
    Sprechtag sprechtag = new Sprechtag();
    if (binder.writeBeanIfValid(sprechtag)) {
      sprechtag.setStatus(status);
      presenter.save(sprechtag);
      navigateToOrganizerView();
    }
  }

  private void navigateToOrganizerView() {
    getUI().ifPresent(ui -> ui.navigate(OrganizerView.ROUTE));
  }

  private Component createAccessTokenPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle("Zugang");
    panel.setDescription("Mit diesem Code buchen Eltern ihre Termine.");
    FormLayout formLayout = panel.getFormLayout();
    accessToken = new TextField();
    accessToken.setLabel("Zugangscode");
    accessToken.setReadOnly(true);
    accessToken.setHelperText("Wird automatisch erzeugt - bei Bedarf neu generieren");
    accessToken.setValue(UUID.randomUUID().toString());

    FormRow firstRow = new FormRow();
    firstRow.add(accessToken, 3);
    Button regenerateAccessTokenButton = new Button();
    regenerateAccessTokenButton.setIcon(VaadinIcon.REFRESH.create());
    regenerateAccessTokenButton.setText("Neu generieren");
    regenerateAccessTokenButton.addClickListener(
        e -> accessToken.setValue(UUID.randomUUID().toString()));
    firstRow.add(regenerateAccessTokenButton, 1);

    formLayout.add(firstRow);
    return panel;
  }

  private Component createClassesPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle("Klassen");
    panel.setDescription("Welche Klassen nehmen am Sprechtag teil?");

    klassen = new CheckboxGroup<>();
    klassen.setItems(presenter.findAllKlassen());
    klassen.setHelperText("0 Klassen ausgewählt");
    klassen.addThemeVariants(CheckboxGroupVariant.AURA_HORIZONTAL);
    klassen.setRenderer(new TextRenderer<>(Klasse::getName));
    klassen.addValueChangeListener(
        e -> klassen.setHelperText(e.getValue().size() + " Klassen ausgewählt"));
    panel.getFormLayout().add(klassen);

    return panel;
  }

  private Component createGeneralInfoPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle("Allgemein");
    panel.setDescription("Titel und Eckdaten, die Eltern und Lehrkräfte sehen können");

    FormRow firstRow = new FormRow();
    titel = new TextField();
    titel.setLabel("Titel");
    titel.setRequiredIndicatorVisible(true);
    titel.setSizeFull();
    titel.setPlaceholder("z. B. Frühjahrs-Sprechtag 2026");

    firstRow.add(titel, 2);

    FormRow secondRow = new FormRow();
    location = new TextField();
    location.setLabel("Ort");
    location.setPlaceholder("z. B. Hauptgebäude, Aula");
    secondRow.add(location, 2);

    FormRow thirdRow = new FormRow();
    description = new TextArea();
    description.setLabel("Beschreibung");
    description.setPlaceholder("Hinweis für Eltern, z. B. Anfahrt, Ablauf, Anmeldeschluss ...");
    description.setMinRows(3);
    thirdRow.add(description, 2);

    FormLayout formLayout = panel.getFormLayout();
    formLayout.add(firstRow, secondRow, thirdRow);

    return panel;
  }

  private Component createTimingPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle("Termin & Zeiten");
    panel.setDescription("Datum, Zeitfenster und Länge eines Gesprächtermins.");
    FormLayout formLayout = panel.getFormLayout();
    FormRow firstRow = new FormRow();

    datePicker = new DatePicker();
    datePicker.setLabel("Datum");
    datePicker.setRequiredIndicatorVisible(true);

    slotInMinutes = new ComboBox<>();
    slotInMinutes.setLabel("Slot-Länge");
    slotInMinutes.setItems(5, 10, 15, 20, 25, 30);
    slotInMinutes.setValue(15);
    slotInMinutes.setRenderer(new TextRenderer<>(minutes -> minutes + " Minuten"));

    firstRow.add(datePicker, slotInMinutes);

    startTime = new TimePicker();
    startTime.setLabel("Startzeit");
    startTime.setRequiredIndicatorVisible(true);

    endTime = new TimePicker();
    endTime.setLabel("Endzeit");
    endTime.setRequiredIndicatorVisible(true);

    FormRow secondRow = new FormRow();
    secondRow.add(startTime, endTime);

    formLayout.add(firstRow, secondRow);
    return panel;
  }

  private static Div createHeader() {
    Div header = new Div();
    header.addClassName("edit-sprechtag-view__header");
    header.add(new H2("Neuen Elternsprechtag anlegen"));
    return header;
  }

  private Div createBottomButtonBar() {
    Div buttonBar = new Div();
    buttonBar.addClassName("edit-sprechtag-view__bottom-button-bar");
    buttonBar.add(createCancelButton(), createSaveAsDraftButton(), createCreateButton());
    return buttonBar;
  }

  private Button createCreateButton() {
    Button button = new Button();
    button.setText("Sprechtag anlegen");
    button.setIcon(VaadinIcon.CHECK.create());
    button.setThemeVariants(ButtonVariant.PRIMARY);
    button.addClickListener(_ -> save(SprechtagStatusEnum.VEROEFFENTLICHT));
    return button;
  }

  private Button createSaveAsDraftButton() {
    Button button = new Button();
    button.setText("Als Entwurf speichern");
    button.addClassName("edit-sprechtag-view__draft-button");
    button.addClickListener(_ -> save(SprechtagStatusEnum.ENTWURF));
    return button;
  }

  private Button createCancelButton() {
    Button button = new Button();
    button.setText("Abbrechen");
    button.setThemeVariants(ButtonVariant.TERTIARY);
    button.addClickListener(_ -> navigateToOrganizerView());
    return button;
  }
}
