package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.AusfallSlot;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.Slotzustand;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Die Sammelaktion „Lehrkraft fällt aus" (#156): alle Slots einer Lehrkraft an diesem Sprechtag —
 * freie, gebuchte und bereits entfallene — zum Ankreuzen, mit „alle auswählen" für den häufigen
 * Ganztagsfall.
 *
 * <p>Warum ein Dialog und nicht die Auswertungstabelle: Die Auswertung ist die Buchungsliste des
 * Organizers. Freie Slots dort einzublenden — typisch deutlich mehr Zeilen als Buchungen —
 * beschädigte ihren Zweck und das Tabellen-Karten-Muster.
 *
 * <p>Der Dialog bleibt <b>dumm</b>: Was wählbar ist und wie viele Termine und Familien eine Auswahl
 * trifft, entscheidet {@link AusfallAuswahl} — Vaadin-frei und deshalb unit-getestet. Hier wird nur
 * gerendert. Heraus gehen Termin-Ids; was daraus tatsächlich wird, meldet der Vorgang.
 */
@CssImport("./styles/components/lehrkraft-ausfall-dialog.css")
public class LehrkraftAusfallDialog extends Dialog {

  private final AusfallAuswahl auswahl;

  private final Button alleButton = new Button();
  private final Div liste = new Div();
  private final Div vorschau = new Div();
  private final Button confirm = new Button();

  public LehrkraftAusfallDialog(
      String lehrkraft, List<AusfallSlot> slots, Consumer<List<UUID>> onConfirm) {
    this.auswahl = new AusfallAuswahl(slots);

    setHeaderTitle(getTranslation("auswertung.ausfall.title"));
    addClassName("ausfall-dialog");

    add(zeile("ausfall-dialog__message", getTranslation("auswertung.ausfall.message", lehrkraft)));

    if (auswahl.slots().isEmpty()) {
      add(zeile("ausfall-dialog__leer", getTranslation("auswertung.ausfall.keine-slots")));
    } else {
      liste.addClassName("ausfall-dialog__liste");
      add(createToolbar(), liste);
      zeichneListe();
    }

    vorschau.addClassName("ausfall-dialog__vorschau");
    // Der Hinweis steht fest über dem Bestätigen, nicht erst nach dem Klick: Ein Rückweg existiert
    // nicht — es gibt keinen Use Case, der einen entfallenen Termin wieder anbietet.
    add(vorschau, zeile("ausfall-dialog__warnung", getTranslation("auswertung.ausfall.warnung")));

    confirm.setText(getTranslation("auswertung.ausfall.confirm"));
    confirm.addClassName("ausfall-dialog__confirm");
    confirm.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);
    confirm.addClickListener(
        _ -> {
          if (!auswahl.hatAuswahl()) {
            return;
          }
          onConfirm.accept(auswahl.terminIds());
          close();
        });

    Button abort = new Button(getTranslation("auswertung.ausfall.abort"), _ -> close());
    abort.addThemeVariants(ButtonVariant.TERTIARY);
    getFooter().add(abort, confirm);

    aktualisiere();
  }

  /** „Alle auswählen" — und derselbe Knopf wieder zurück. */
  private Div createToolbar() {
    Div toolbar = new Div();
    toolbar.addClassName("ausfall-dialog__toolbar");
    alleButton.addClassName("ausfall-dialog__alle");
    alleButton.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
    alleButton.setEnabled(auswahl.hatWaehlbare());
    alleButton.addClickListener(
        _ -> {
          auswahl.waehleAlle(!auswahl.alleWaehlbarenGewaehlt());
          // Die Häkchen haben sich von außen geändert; Vaadins Checkbox erfährt davon nichts.
          zeichneListe();
          aktualisiere();
        });
    toolbar.add(alleButton);
    return toolbar;
  }

  private void zeichneListe() {
    liste.removeAll();
    auswahl.slots().stream().map(this::createZeile).forEach(liste::add);
  }

  private Div createZeile(AusfallSlot slot) {
    Div zeile = new Div();
    zeile.addClassName("ausfall-dialog__zeile");

    Checkbox kreuz = new Checkbox();
    kreuz.addClassName("ausfall-dialog__check");
    kreuz.setValue(auswahl.istGewaehlt(slot.terminId()));
    // Bereits entfallene Slots sind sichtbar, aber nicht mehr wählbar. Die Entscheidung trifft das
    // Modell, nicht die Checkbox.
    kreuz.setEnabled(auswahl.istWaehlbar(slot));
    kreuz.setAriaLabel(Formats.time(slot.zeit()));
    kreuz.addValueChangeListener(
        event -> {
          auswahl.waehle(slot.terminId(), Boolean.TRUE.equals(event.getValue()));
          aktualisiere();
        });

    Span zeit = new Span(Formats.time(slot.zeit()));
    zeit.addClassName("ausfall-dialog__zeit");

    Span zustand = new Span(zustandsText(slot.zustand()));
    zustand.addClassName("ausfall-dialog__zustand");
    zustand.addClassName("ausfall-dialog__zustand--" + slot.zustand().name().toLowerCase());

    zeile.add(kreuz, zeit, zustand);

    // Namen nur bei gebuchten Slots — und nur Namen: Die Eltern-Adresse ist der
    // Bündelungsschlüssel der Zählung und steht nirgends auf dem Schirm.
    if (slot.zustand() == Slotzustand.GEBUCHT) {
      Span familie =
          new Span(
              getTranslation(
                  "auswertung.ausfall.familie", slot.schuelerName(), slot.elternName()));
      familie.addClassName("ausfall-dialog__familie");
      zeile.add(familie);
    }
    return zeile;
  }

  private String zustandsText(Slotzustand zustand) {
    return switch (zustand) {
      case FREI -> getTranslation("auswertung.ausfall.zustand.frei");
      case GEBUCHT -> getTranslation("auswertung.ausfall.zustand.gebucht");
      case ENTFALLEN -> getTranslation("auswertung.ausfall.zustand.entfallen");
    };
  }

  /**
   * Nach jeder Auswahländerung: Vorschau, „alle"-Knopf und Bestätigen neu beschriften. Die Zahlen
   * kommen aus dem Modell — <b>keine Server-Query je Klick</b>, der Familien-Schlüssel liegt schon
   * in jeder Zeile.
   */
  private void aktualisiere() {
    // Die Familienzahl ist in ihren Schlüsseln als Schätzung formuliert („etwa M Familien"):
    // Der Dialog liest ein Read-Modell, das veralten darf.
    vorschau.setText(
        getTranslation(
            "auswertung.ausfall.vorschau",
            zahlLabel("auswertung.ausfall.vorschau.termine", auswahl.anzahlTermine()),
            zahlLabel("auswertung.ausfall.vorschau.familien", auswahl.anzahlFamilien())));
    alleButton.setText(
        auswahl.alleWaehlbarenGewaehlt()
            ? getTranslation("auswertung.ausfall.keine-waehlen")
            : getTranslation("auswertung.ausfall.alle-waehlen"));
    confirm.setEnabled(auswahl.hatAuswahl());
  }

  /** Singular, Plural und Null als drei Schlüssel — „1 Termine gewählt" ist kein Deutsch. */
  private String zahlLabel(String praefix, int anzahl) {
    return switch (anzahl) {
      case 0 -> getTranslation(praefix + ".none");
      case 1 -> getTranslation(praefix + ".one");
      default -> getTranslation(praefix + ".other", anzahl);
    };
  }

  private Div zeile(String klasse, String text) {
    Div div = new Div();
    div.addClassName(klasse);
    div.setText(text);
    return div;
  }
}
