package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.ui.OrganizerPresenter.OrganizerModel;
import de.openclassware.elternsprechtag.ui.components.Card;
import de.openclassware.elternsprechtag.ui.components.NextElternsprechtage;
import de.openclassware.elternsprechtag.ui.components.NoNextElternsprechtage;
import de.openclassware.elternsprechtag.ui.layouts.MainLayout;
import jakarta.annotation.security.RolesAllowed;

@Route(value = OrganizerView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/organizer-view.css")
public class OrganizerView extends Div {

  public static final String ROUTE = "organizers";

  OrganizerView(OrganizerPresenter presenter) {
    addClassName("organizer-view");
    OrganizerModel model = presenter.load();
    add(
        createGreeting(model.username()),
        createIntro(),
        createCards(),
        createNextElternsprechtageHeading(),
        createUpcoming(model));
  }

  private Component createUpcoming(OrganizerModel model) {
    return model.hasUpcoming()
        ? new NextElternsprechtage(model.upcoming())
        : new NoNextElternsprechtage();
  }

  private Component createNextElternsprechtageHeading() {
    H2 ihreElternsprechtage = new H2(getTranslation("organizer.next-heading"));
    ihreElternsprechtage.addClassName("organizer-view__next-elternsprechtage-heading");
    return ihreElternsprechtage;
  }

  private HorizontalLayout createCards() {
    HorizontalLayout cards = new HorizontalLayout();
    cards.add(createElternsprechtagCard(), manageElternsprechtagCard());
    return cards;
  }

  private Card createElternsprechtagCard() {
    Card card =
        new Card(
            VaadinIcon.PLUS,
            getTranslation("organizer.card.new.title"),
            getTranslation("organizer.card.new.description"));
    card.primary();
    card.addClickListener(_ -> card.getUI().ifPresent(this::navigateToEditSprechtag));
    return card;
  }

  private void navigateToEditSprechtag(UI ui) {
    ui.navigate(EditSprechtagView.ROUTE);
  }

  private Card manageElternsprechtagCard() {
    Card card =
        new Card(
            VaadinIcon.LIST,
            getTranslation("organizer.card.manage.title"),
            getTranslation("organizer.card.manage.description"));
    card.addClickListener(_ -> card.getUI().ifPresent(this::navigateToManageSprechtag));
    return card;
  }

  private void navigateToManageSprechtag(UI ui) {
    ui.navigate(ManageSprechtagView.ROUTE);
  }

  private Div createGreeting(String username) {
    Div greeting = new Div();
    greeting.addClassName("organizer-view__greeting");
    greeting.setText(getTranslation("organizer.greeting", username));
    return greeting;
  }

  private Div createIntro() {
    Div intro = new Div();
    intro.addClassName("organizer-view__intro");
    intro.setText(getTranslation("organizer.intro"));
    return intro;
  }
}
