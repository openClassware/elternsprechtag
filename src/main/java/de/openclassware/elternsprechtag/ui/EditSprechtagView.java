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
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.services.KlassenService.KlasseOption;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagForm;
import de.openclassware.elternsprechtag.ui.components.Breadcrumb;
import de.openclassware.elternsprechtag.ui.components.FormPanel;
import de.openclassware.elternsprechtag.ui.layouts.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import java.util.Optional;
import java.util.UUID;

@Route(value = EditSprechtagView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/edit-sprechtag-view.css")
public class EditSprechtagView extends Div implements HasUrlParameter<String> {

  public static final String ROUTE = "sprechtag";

  private final EditSprechtagPresenter presenter;
  private final Binder<SprechtagForm> binder = new Binder<>(SprechtagForm.class);

  private UUID editingId;
  private Breadcrumb breadcrumb;
  private H2 headerTitle;
  private Button createButton;

  private TextField titel;
  private TextField location;
  private TextArea description;
  private DatePicker datePicker;
  private ComboBox<Integer> slotInMinutes;
  private TimePicker startTime;
  private TimePicker endTime;
  private CheckboxGroup<KlasseOption> klassen;
  private TextField accessToken;
  private TextField shareLink;
  private String origin;

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
        .asRequired(getTranslation("edit-sprechtag.validation.titel-required"))
        .bind(SprechtagForm::getTitel, SprechtagForm::setTitel);
    binder.forField(location).bind(SprechtagForm::getLocation, SprechtagForm::setLocation);
    binder.forField(description).bind(SprechtagForm::getDescription, SprechtagForm::setDescription);
    binder
        .forField(datePicker)
        .asRequired(getTranslation("edit-sprechtag.validation.datum-required"))
        .bind(SprechtagForm::getStartDate, SprechtagForm::setStartDate);
    binder
        .forField(startTime)
        .asRequired(getTranslation("edit-sprechtag.validation.startzeit-required"))
        .bind(SprechtagForm::getStartTime, SprechtagForm::setStartTime);
    binder
        .forField(endTime)
        .asRequired(getTranslation("edit-sprechtag.validation.endzeit-required"))
        .bind(SprechtagForm::getEndTime, SprechtagForm::setEndTime);

    binder
        .forField(slotInMinutes)
        .bind(SprechtagForm::getSlotInMinutes, SprechtagForm::setSlotInMinutes);

    binder.forField(accessToken).bind(SprechtagForm::getAccessToken, SprechtagForm::setAccessToken);

    binder
        .forField(klassen)
        .withValidator(
            selected -> selected != null && !selected.isEmpty(),
            getTranslation("edit-sprechtag.validation.klasse-required"))
        .bind(SprechtagForm::getKlassen, SprechtagForm::setKlassen);

    binder.withValidator(
        form ->
            form.getStartTime() == null
                || form.getEndTime() == null
                || form.getEndTime().isAfter(form.getStartTime()),
        getTranslation("edit-sprechtag.validation.end-after-start"));
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String sprechtagId) {
    if (sprechtagId == null) {
      return; // Anlege-Modus
    }
    Optional<UUID> id = parseId(sprechtagId);
    Optional<SprechtagForm> form = id.flatMap(presenter::loadForm);
    if (form.isEmpty()) {
      event.rerouteToError(NotFoundException.class, "Sprechtag not found: " + sprechtagId);
      return;
    }
    editingId = id.get();
    binder.readBean(form.get());
    breadcrumb.setCurrentText(getTranslation("edit-sprechtag.breadcrumb.title-edit"));
    headerTitle.setText(getTranslation("edit-sprechtag.header.title-edit"));
    createButton.setText(getTranslation("edit-sprechtag.button.save"));
  }

  private Optional<UUID> parseId(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private void save(SprechtagStatusEnum status) {
    SprechtagForm form = new SprechtagForm();
    if (binder.writeBeanIfValid(form)) {
      presenter.save(editingId, form, status);
      navigateToOrganizerView();
    }
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
    klassen.setItems(presenter.findAllKlassen());
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

    FormLayout formLayout = panel.getFormLayout();
    formLayout.add(firstRow, secondRow, thirdRow);

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
    createButton.addClickListener(_ -> save(SprechtagStatusEnum.VEROEFFENTLICHT));
    return createButton;
  }

  private Button createSaveAsDraftButton() {
    Button button = new Button();
    button.setText(getTranslation("edit-sprechtag.button.draft"));
    button.addClassName("edit-sprechtag-view__draft-button");
    button.addClickListener(_ -> save(SprechtagStatusEnum.ENTWURF));
    return button;
  }

  private Button createCancelButton() {
    Button button = new Button();
    button.setText(getTranslation("edit-sprechtag.button.cancel"));
    button.setThemeVariants(ButtonVariant.TERTIARY);
    button.addClickListener(_ -> navigateToOrganizerView());
    return button;
  }
}
