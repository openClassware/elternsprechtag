package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.ui.components.StepHeader;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

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

  public ElternsprechtagView(ElternsprechtagPresenter presenter) {
    this.presenter = presenter;
    addClassName("elternsprechtag-view");
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String token) {
    removeAll();
    Optional<Sprechtag> sprechtag = presenter.findByAccessToken(token);
    if (sprechtag.isPresent() && sprechtag.get().getStatus() == SprechtagStatusEnum.VEROEFFENTLICHT) {
      add(createHeader(), createInfo(sprechtag.get()), createAngaben(sprechtag.get()));
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

    schuelerName = new TextField(getTranslation("elternsprechtag.angaben.kind.label"));
    schuelerName.setPlaceholder(getTranslation("elternsprechtag.angaben.kind.placeholder"));
    schuelerName.setRequiredIndicatorVisible(true);

    klasse = new Select<>();
    klasse.setLabel(getTranslation("elternsprechtag.angaben.klasse.label"));
    klasse.setPlaceholder(getTranslation("elternsprechtag.angaben.klasse.placeholder"));
    klasse.setItemLabelGenerator(Klasse::getName);
    klasse.setItems(
        sprechtag.getKlassen().stream()
            .sorted(Comparator.comparing(Klasse::getName))
            .toList());
    klasse.setRequiredIndicatorVisible(true);

    FormLayout form = new FormLayout();
    form.addClassName("elternsprechtag-view__form");
    form.setResponsiveSteps(
        new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("640px", 2));
    form.add(elternName, schuelerName, klasse);

    card.add(form);
    return card;
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
