package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.ui.layout.MainLayout;
import jakarta.annotation.security.RolesAllowed;

@Route(value = EditSprechtagView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/edit-sprechtag-view.css")
public class EditSprechtagView extends Div {

  public static final String ROUTE = "sprechtag";

  public EditSprechtagView() {
    addClassName("edit-sprechtag-view");
    add(createHeader(), createGeneralInfoPanel(), createBottomButtonBar());
  }

  private Component createGeneralInfoPanel() {
    Div panel = new Div();
    panel.addClassName("general-info-panel");
    panel.add(createGeneralInfoPanelDescription(), createGeneralInfoForm());
    return panel;
  }

  private Component createGeneralInfoForm() {
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
    thirdRow.add(description, 2);

    FormLayout layout = new FormLayout();
    layout.addClassName("general-info-panel__form");
    layout.setAutoResponsive(true);
    layout.setExpandFields(true);
    layout.setExpandColumns(true);
    layout.setWidthFull();
    layout.add(firstRow, secondRow, thirdRow);
    return layout;
  }

  private Component createGeneralInfoPanelDescription() {
    Div title = new Div();
    title.addClassName("general-info-panel__titel");
    title.setText("Allgemein");
    Div description = new Div();
    description.addClassName("general-info-panel__description");
    description.setText("Titel und Eckdaten, die Eltern und Lehrkräfte sehen können");
    Div panelDescription = new Div();
    panelDescription.add(title, description);
    return panelDescription;
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
