package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@AnonymousAllowed
@Route(value = "login", autoLayout = false)
public class LoginView extends Div implements BeforeEnterObserver {

  private final LoginForm login;

  public LoginView() {
    LoginI18n.Form i18nForm = new LoginI18n.Form();
    i18nForm.setTitle("Anmeldung");
    i18nForm.setUsername("Nutzername");
    i18nForm.setPassword("Passwort");
    i18nForm.setSubmit("Anmelden");

    LoginI18n loginI18n = new LoginI18n();
    loginI18n.setForm(i18nForm);

    login = new LoginForm();
    login.setI18n(loginI18n);
    login.setAction("login");
    login.setForgotPasswordButtonVisible(false);

    VerticalLayout layout = new VerticalLayout();
    layout.setAlignItems(FlexComponent.Alignment.CENTER);
    layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    layout.add(login);
    layout.setSizeFull();

    add(layout);
    setSizeFull();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      login.setError(true);
    }
  }
}
