package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts.Rechtslinks.Rechtslink;
import java.util.List;

/**
 * Äußerster Rahmen jeder Web-View (Issue #230): Inhalt oben, darunter der Footer mit den Links
 * auf Impressum und Datenschutzerklärung. {@link MainLayout} hängt sich als Kind ein, Login und
 * Eltern-Buchung direkt — so trägt jede View denselben Footer. Ohne konfigurierte Adresse
 * entfällt der Footer ganz.
 *
 * <p>{@code @AnonymousAllowed}, weil Vaadin den Zugriff auch an jedem Eltern-Layout prüft: Die
 * Eltern erreichen ihre Seite anonym.
 */
@AnonymousAllowed
@CssImport("./styles/layouts/seiten-layout.css")
public class SeitenLayout extends Div implements RouterLayout {

  private final Div content = new Div();

  SeitenLayout(SeitenLayoutPresenter presenter) {
    addClassName("seiten-layout");
    content.addClassName("seiten-layout__content");
    add(content);

    List<Rechtslink> links = presenter.getRechtslinks();
    if (!links.isEmpty()) {
      add(footer(links));
    }
  }

  @Override
  public void showRouterLayoutContent(HasElement routerLayoutContent) {
    content.getElement().removeAllChildren();
    if (routerLayoutContent != null) {
      content.getElement().appendChild(routerLayoutContent.getElement());
    }
  }

  private Footer footer(List<Rechtslink> links) {
    Footer footer = new Footer();
    footer.addClassName("seiten-layout__footer");
    for (Rechtslink link : links) {
      Anchor anchor = new Anchor(link.url(), getTranslation(link.textKey()));
      anchor.setTarget(AnchorTarget.BLANK);
      anchor.getElement().setAttribute("rel", "noopener");
      anchor.addClassName("seiten-layout__link");
      footer.add(anchor);
    }
    return footer;
  }
}
