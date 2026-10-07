package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import de.openclassware.elternsprechtag.security.Roles;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed(Roles.ORGANIZER)
@Layout
@CssImport("./styles/layouts/main-layout.css")
public class MainLayout extends VerticalLayout implements RouterLayout {

  /** Startseite der Anwender-Doku. Fest verdrahtet: Hilfe je Ansicht gibt es nicht. */
  private static final String HELP_URL = "https://docs.openclassware.de";

  private final Main content = new Main();

  public MainLayout(MainLayoutPresenter presenter) {
    addClassName("main-layout");
    content.addClassName("main-layout__content");
    setPadding(false);
    add(new MainLayoutHeader(presenter), content);
  }

  @Override
  public void showRouterLayoutContent(HasElement routerLayoutContent) {
    content.getElement().removeAllChildren();
    if (routerLayoutContent != null) {
      content.getElement().appendChild(routerLayoutContent.getElement());
    }
  }

  static final class MainLayoutHeader extends Header {

    private final MainLayoutPresenter presenter;

    public MainLayoutHeader(MainLayoutPresenter presenter) {
      this.presenter = presenter;
      addClassName("main-header");
      add(createBrand(), createUser());
    }

    private Div createBrand() {
      Div brand = new Div();
      brand.addClassName("main-header__brand");

      Span logo = new Span(getTranslation("main.brand.logo"));
      logo.addClassName("main-header__logo");

      H1 title = new H1(getTranslation("main.brand.title"));
      title.addClassName("main-header__title");

      brand.add(logo, title);
      return brand;
    }

    private Div createUser() {
      Div user = new Div();
      user.addClassName("main-header__user");

      Span school = new Span();
      school.setText(presenter.getSchoolname());
      school.addClassName("main-header__school");

      Span divider = new Span();
      divider.addClassName("main-header__divider");

      Span avatar = new Span();
      avatar.setText(presenter.getAvatar());
      avatar.addClassName("main-header__avatar");

      Span username = new Span();
      username.setText(presenter.getUsername());
      username.addClassName("main-header__username");

      Button logout = new Button();
      logout.setIcon(VaadinIcon.SIGN_OUT.create());
      Span logoutLabel = new Span(getTranslation("main.logout"));
      logoutLabel.addClassName("main-header__logout-label");
      logout.getElement().appendChild(logoutLabel.getElement());
      // Auf schmalen Schirmen nur das Icon sichtbar (Label per CSS ausgeblendet) — daher aria-label.
      logout.getElement().setAttribute("aria-label", getTranslation("main.logout"));
      logout.addClickListener(event -> presenter.logout());

      user.add(createHelp(), school, divider, avatar, username, logout);
      return user;
    }

    private Anchor createHelp() {
      Anchor help = new Anchor(HELP_URL);
      help.setTarget(AnchorTarget.BLANK);
      help.getElement().setAttribute("rel", "noopener");
      help.addClassName("main-header__help");
      Span helpLabel = new Span(getTranslation("main.help"));
      helpLabel.addClassName("main-header__help-label");
      help.add(VaadinIcon.QUESTION_CIRCLE_O.create(), helpLabel);
      // Wie beim Abmelden bleibt auf schmalen Schirmen nur das Icon — daher aria-label.
      help.getElement().setAttribute("aria-label", getTranslation("main.help.aria-label"));
      return help;
    }
  }
}
