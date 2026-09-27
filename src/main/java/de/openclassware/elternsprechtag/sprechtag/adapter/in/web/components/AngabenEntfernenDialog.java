package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;

/**
 * Rückfrage vor „Angaben entfernen" (Issue #129) — dem Löschverlangen einer Familie. Wie der
 * {@link StornoBuchungDialog} identifiziert sie die Zeile, hier zusätzlich mit den Eltern: An ihnen
 * erkennt der Organizer, wer die Löschung verlangt hat.
 *
 * <p>Der Hinweis auf weitere Termine ist fest, ohne Zählung: Die Aktion betrifft genau eine Buchung,
 * eine Familien-Identität kennt das Modell bewusst nicht.
 *
 * <p>Reine Anzeige-Komponente: fertige Werte herein, {@link Runnable} bei Bestätigung.
 */
@CssImport("./styles/components/angaben-entfernen-dialog.css")
public class AngabenEntfernenDialog extends Dialog {

  /**
   * @param zeit bereits formatiert — die Uhrzeit kommt über die zentrale Formatter-Stelle herein
   */
  public AngabenEntfernenDialog(
      String schuelerName, String elternName, String zeit, String lehrkraft, Runnable onConfirm) {
    setHeaderTitle(getTranslation("auswertung.entfernen.title"));
    addClassName("angaben-entfernen-dialog");

    Div message = new Div();
    message.addClassName("angaben-entfernen-dialog__message");
    message.setText(getTranslation("auswertung.entfernen.message"));

    Div buchung = new Div();
    buchung.addClassName("angaben-entfernen-dialog__buchung");
    buchung.setText(
        getTranslation("auswertung.entfernen.buchung", schuelerName, elternName, zeit, lehrkraft));

    Div weitere = new Div();
    weitere.addClassName("angaben-entfernen-dialog__weitere");
    weitere.setText(getTranslation("auswertung.entfernen.weitere"));

    add(message, buchung, weitere);

    Button abort = new Button(getTranslation("auswertung.entfernen.abort"), _ -> close());
    abort.addThemeVariants(ButtonVariant.TERTIARY);

    Button confirm =
        new Button(
            getTranslation("auswertung.entfernen.confirm"),
            _ -> {
              onConfirm.run();
              close();
            });
    confirm.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);

    getFooter().add(abort, confirm);
  }
}
