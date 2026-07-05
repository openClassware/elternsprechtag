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
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.ui.component.FormPanel;
import de.openclassware.elternsprechtag.ui.layout.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.UUID;

@Route(value = EditSprechtagView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/edit-sprechtag-view.css")
public class EditSprechtagView extends Div {

  public static final String ROUTE = "sprechtag";

  public EditSprechtagView() {
    addClassName("edit-sprechtag-view");
    add(
        createHeader(),
        createGeneralInfoPanel(),
        createTimingPanel(),
        createClassesPanel(),
        createAccessTokenPanel(),
        createBottomButtonBar());
  }

  private Component createAccessTokenPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle("Zugang");
    panel.setDescription("Mit diesem Code buchen Eltern ihre Termine.");
    FormLayout formLayout = panel.getFormLayout();
    TextField accessToken = new TextField();
    accessToken.setLabel("Zugangscode");
    accessToken.setReadOnly(true);
    accessToken.setHelperText("Wird automatisch erzeugt - bei Bedarf neu generieren");
    accessToken.setValue(UUID.randomUUID().toString());

    FormRow firstRow = new FormRow();
    firstRow.add(accessToken,3);
    Button regenerateAccessTokenButton = new Button();
    regenerateAccessTokenButton.setIcon(VaadinIcon.REFRESH.create());
    regenerateAccessTokenButton.setText("Neu generieren");
    firstRow.add(regenerateAccessTokenButton, 1);

    formLayout.add(firstRow);
    return panel;
  }

  private Component createClassesPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle("Klassen");
    panel.setDescription("Welche Klassen nehmen am Sprechtag teil?");

    CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();
    checkboxGroup.setItems("5a", "5b", "5c", "6b", "7a", "7b", "8a", "8b", "9a", "9b");
    checkboxGroup.setHelperText("0 Klassen ausgewählt");
    checkboxGroup.addThemeVariants(CheckboxGroupVariant.AURA_HORIZONTAL);
    checkboxGroup.addValueChangeListener(
        e -> checkboxGroup.setHelperText(e.getValue().size() + " Klassen ausgewählt"));
    panel.getFormLayout().add(checkboxGroup);

    return panel;
  }

  private Component createGeneralInfoPanel() {
    FormPanel panel = new FormPanel();
    panel.setTitle("Allgemein");
    panel.setDescription("Titel und Eckdaten, die Eltern und Lehrkräfte sehen können");

    FormRow firstRow = new FormRow();
    TextField titel = new TextField();
    titel.setLabel("Titel");
    titel.setRequiredIndicatorVisible(true);
    titel.setSizeFull();
    titel.setPlaceholder("z. B. Frühjahrs-Sprechtag 2026");

    firstRow.add(titel, 2);

    FormRow secondRow = new FormRow();
    TextField location = new TextField();
    location.setLabel("Ort");
    location.setRequiredIndicatorVisible(true);
    location.setPlaceholder("z. B. Hauptgebäude, Aula");
    secondRow.add(location);
    ComboBox<String> status = new ComboBox<>();
    status.setLabel("Status");
    secondRow.add(status);

    FormRow thirdRow = new FormRow();
    TextArea description = new TextArea();
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

    DatePicker datePicker = new DatePicker();
    datePicker.setLabel("Datum");
    datePicker.setRequiredIndicatorVisible(true);

    ComboBox<String> slot = new ComboBox<>();
    slot.setLabel("Slot-Länge");
    slot.setItems(
        "5 Minuten",
        "10 Minuten",
        "15 Minuten",
        "20 Minuten",
        "25 Minuten",
        "30 Minuten",
        "35 Minuten");

    firstRow.add(datePicker, slot);

    TimePicker starttime = new TimePicker();
    starttime.setLabel("Startzeit");
    starttime.setRequiredIndicatorVisible(true);

    TimePicker endtime = new TimePicker();
    endtime.setLabel("Endzeit");
    endtime.setRequiredIndicatorVisible(true);

    FormRow secondRow = new FormRow();
    secondRow.add(starttime, endtime);

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
    return button;
  }

  private Button createSaveAsDraftButton() {
    Button button = new Button();
    button.setText("Als Entwurf speichern");
    button.addClassName("edit-sprechtag-view__draft-button");
    return button;
  }

  private Button createCancelButton() {
    Button button = new Button();
    button.setText("Abbrechen");
    button.setThemeVariants(ButtonVariant.TERTIARY);
    return button;
  }
}
