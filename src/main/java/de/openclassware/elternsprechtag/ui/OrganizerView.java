package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.ui.component.Card;
import de.openclassware.elternsprechtag.ui.layout.MainLayout;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "organizers", layout = MainLayout.class)
@RolesAllowed("ORGANIZER")
@CssImport("./styles/organizer-view.css")
public class OrganizerView extends Main {

  private final OrganizerPresenter presenter;

  OrganizerView(OrganizerPresenter presenter) {
    this.presenter = presenter;
    addClassName("organizer-view");
    add(createGreeting(), createIntro(), createCard());
  }

  private HorizontalLayout createCard() {
    HorizontalLayout cards = new HorizontalLayout();
    cards.add(createElternsprechtagCard(), manageElternsprechtagCard());
    return cards;
  }

  private Card createElternsprechtagCard() {
    Card card =
        new Card(
            VaadinIcon.PLUS,
            "Neuen Elternsprechtag anlegen",
            "Datum, Zeitfenster, und Lehrkräfte festlegen - in wenigen Schritten startklar.");
    card.primary();
    return card;
  }

  private Card manageElternsprechtagCard() {
    Card card =
        new Card(
            VaadinIcon.LIST,
            "Bestehende verwalten",
            "Termine, Buchungen und Zeitpläne Ihrer laufenden Sprechtage im Blick behalten.");
    return card;
  }

  private Div createGreeting() {
    Div greeting = new Div();
    greeting.addClassName("organizer-view__greeting");
    greeting.setText(String.format("Guten Tag, %s", presenter.getUsername()));
    return greeting;
  }

  private Div createIntro() {
    Div intro = new Div();
    intro.addClassName("organizer-view__intro");
    intro.setText("Was möchten Sie heute tun?");
    return intro;
  }
}
