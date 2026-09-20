package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungBereitsStorniertException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagHatBuchungenException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.StatusuebergangException;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import de.openclassware.elternsprechtag.sprechtag.domain.ZeitstrukturEingefrorenException;

/**
 * Übersetzt die fachlichen Weigerungen des Kontexts in i18n-Schlüssel — die des Sprechtag-Aggregats
 * ebenso wie die Vorbedingungen des Stornos.
 *
 * <p>„Gesperrt" allein genügt nicht — `ABDECKUNG.md` verlangt bei jedem dieser Fälle eine
 * <em>Begründung</em>. Die Domäne trägt ihren Grund in der Exception-Meldung, aber die ist für Log
 * und Test geschrieben und geht nicht durch die Übersetzungsdatei; deshalb steht hier die Zuordnung
 * zu dem Text, den der Organizer zu sehen bekommt.
 *
 * <p>Was nicht zugeordnet ist, wird nicht geschluckt: Ein unerwarteter Fehler soll als Fehler
 * sichtbar werden und nicht als freundlicher Hinweis.
 */
final class SprechtagMeldungen {

  private SprechtagMeldungen() {}

  /**
   * Was die Oberfläche nach einer Aktion anzeigt: der i18n-Schlüssel und ob es eine Weigerung war
   * ({@code fehler}) oder nur ein Hinweis.
   */
  record Meldung(String schluessel, boolean fehler) {

    static Meldung fehler(String schluessel) {
      return new Meldung(schluessel, true);
    }

    static Meldung hinweis(String schluessel) {
      return new Meldung(schluessel, false);
    }
  }

  /** Ein veröffentlichter Sprechtag ohne einen einzigen Termin (`ABDECKUNG.md` Z. 94). */
  static Meldung ohneTermine() {
    return Meldung.hinweis("sprechtag.hinweis.keine-termine");
  }

  /**
   * Zeigt die Meldung an. Die Übersetzung holt sie sich am aufrufenden View — {@code getTranslation}
   * hängt an der Komponente, nicht an dieser Klasse.
   */
  static void zeige(Component quelle, Meldung meldung) {
    Notification notification = Notification.show(quelle.getTranslation(meldung.schluessel()));
    notification.addThemeVariants(
        meldung.fehler() ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_CONTRAST);
  }

  /**
   * @return die Begründung zur fachlichen Weigerung
   * @throws RuntimeException die übergebene Ausnahme, wenn sie keine fachliche Weigerung ist
   */
  static Meldung zu(RuntimeException fehler) {
    if (fehler instanceof ZeitstrukturEingefrorenException) {
      return Meldung.fehler("sprechtag.fehler.zeitstruktur-eingefroren");
    }
    if (fehler instanceof SprechtagHatBuchungenException) {
      return Meldung.fehler("sprechtag.fehler.hat-buchungen");
    }
    if (fehler instanceof StatusuebergangException) {
      return Meldung.fehler("sprechtag.fehler.status-uebergang");
    }
    if (fehler instanceof BuchungNichtGefundenException) {
      return Meldung.fehler("buchung.fehler.nicht-gefunden");
    }
    if (fehler instanceof BuchungBereitsStorniertException) {
      return Meldung.fehler("buchung.fehler.bereits-storniert");
    }
    if (fehler instanceof SprechtagNichtVeroeffentlichtException) {
      return Meldung.fehler("buchung.fehler.sprechtag-nicht-veroeffentlicht");
    }
    if (fehler instanceof TerminBelegtException) {
      return Meldung.fehler("buchung.fehler.termin-belegt");
    }
    throw fehler;
  }
}
