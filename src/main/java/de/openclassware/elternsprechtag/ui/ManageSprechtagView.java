package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;
import de.openclassware.elternsprechtag.security.Roles;
import de.openclassware.elternsprechtag.ui.components.SprechtagTable;
import de.openclassware.elternsprechtag.ui.layouts.MainLayout;
import jakarta.annotation.security.RolesAllowed;

@Route(value = ManageSprechtagView.ROUTE, layout = MainLayout.class)
@RolesAllowed(Roles.ORGANIZER)
@CssImport("./styles/manage-sprechtag-view.css")
public class ManageSprechtagView extends Div {

  public static final String ROUTE = "sprechtage";

  private final ManageSprechtagPresenter presenter;

  public ManageSprechtagView(ManageSprechtagPresenter presenter) {
    this.presenter = presenter;
    addClassName("manage-sprechtag-view");
    add(createHeader(), new SprechtagTable(presenter.findAllSprechtage()));
  }

  private Div createHeader() {
    Div header = new Div();
    header.addClassName("manage-sprechtag-view__header");
    header.add(new H2(getTranslation("manage-sprechtag.header.title")));
    return header;
  }
}
