package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;

/**
 * Bestätigungsdialog vor dem Storno einer Buchung. Er <em>identifiziert</em> die Zeile, statt nur zu
 * warnen: Schülername, Uhrzeit und Lehrkraft sind genau die Felder, an denen der Organizer die
 * Buchung am Telefon wiedererkennt.
 *
 * <p>Reine Anzeige-Komponente nach dem Vorbild von {@link CancelSprechtagDialog}: fertige Werte
 * herein, {@link Runnable} bei Bestätigung — keine Entscheidung liegt hier.
 */
@CssImport("./styles/components/storno-buchung-dialog.css")
public class StornoBuchungDialog extends Dialog {

  /**
   * @param zeit bereits formatiert — die Uhrzeit kommt über die zentrale Formatter-Stelle herein
   */
  public StornoBuchungDialog(
      String schuelerName, String zeit, String lehrkraft, Runnable onConfirm) {
    setHeaderTitle(getTranslation("auswertung.storno.title"));
    addClassName("storno-buchung-dialog");

    Div message = new Div();
    message.addClassName("storno-buchung-dialog__message");
    message.setText(getTranslation("auswertung.storno.message"));

    Div buchung = new Div();
    buchung.addClassName("storno-buchung-dialog__buchung");
    buchung.setText(getTranslation("auswertung.storno.buchung", schuelerName, zeit, lehrkraft));

    add(message, buchung);

    Button abort = new Button(getTranslation("auswertung.storno.abort"), _ -> close());
    abort.addThemeVariants(ButtonVariant.TERTIARY);

    Button confirm =
        new Button(
            getTranslation("auswertung.storno.confirm"),
            _ -> {
              onConfirm.run();
              close();
            });
    confirm.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);

    getFooter().add(abort, confirm);
  }
}
