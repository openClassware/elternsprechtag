package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@AnonymousAllowed
@Route(value = "login", autoLayout = false)
@CssImport("./styles/login-view.css")
public class LoginView extends Div implements BeforeEnterObserver {

  private final LoginForm login;

  LoginView() {
    LoginI18n.Form i18nForm = new LoginI18n.Form();
    i18nForm.setTitle(getTranslation("login.title"));
    i18nForm.setUsername(getTranslation("login.username"));
    i18nForm.setPassword(getTranslation("login.password"));
    i18nForm.setSubmit(getTranslation("login.submit"));

    LoginI18n.ErrorMessage i18nError = new LoginI18n.ErrorMessage();
    i18nError.setTitle(getTranslation("login.error.title"));
    i18nError.setMessage(getTranslation("login.error.message"));

    LoginI18n loginI18n = new LoginI18n();
    loginI18n.setForm(i18nForm);
    loginI18n.setErrorMessage(i18nError);

    login = new LoginForm();
    login.setI18n(loginI18n);
    login.setAction("login");
    login.setForgotPasswordButtonVisible(false);

    // Der Markentext ist Geschwister von Bild und Formular, nicht Kind des Bildes: unter
    // 1024 px steht er im normalen Fluss unter der Karte, sonst würde eine wachsende Karte
    // (Fehlermeldung) ihn überdecken.
    add(media(), panel(), brand());

    addClassName("login-view");
    setSizeFull();
  }

  private Div media() {
    Div media = new Div();
    media.addClassName("login-view__media");
    return media;
  }

  private Div brand() {
    Div name = new Div(getTranslation("login.brand.name"));
    name.addClassName("login-view__brand-name");

    Div claim = new Div(getTranslation("login.brand.claim"));
    claim.addClassName("login-view__brand-claim");

    Div brand = new Div(name, claim);
    brand.addClassName("login-view__brand");
    return brand;
  }

  private Div panel() {
    Div card = new Div(login);
    card.addClassName("login-view__card");

    Div panel = new Div(card);
    panel.addClassName("login-view__panel");
    return panel;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      login.setError(true);
    }
  }
}
