package de.openclassware.elternsprechtag.ui.layout;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
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
@CssImport("./styles/main-layout.css")
public class MainLayout extends VerticalLayout implements RouterLayout {

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

      Span logo = new Span("E");
      logo.addClassName("main-header__logo");

      H1 title = new H1("Elternsprechtag");
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
      logout.setText("Abmelden");
      logout.setIcon(VaadinIcon.SIGN_OUT.create());
      logout.addClickListener(event -> presenter.logout());

      user.add(school, divider, avatar, username, logout);
      return user;
    }
  }
}
