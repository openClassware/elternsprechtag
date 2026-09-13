package de.openclassware.elternsprechtag.sprechtag.domain;

import java.util.Objects;

/**
 * Wie die Eltern die Schule erreichen — mehrzeiliger Freitext (Ansprechpartner, Telefon,
 * Sprechzeiten). Pflicht ab dem Entwurf, nicht erst beim Veröffentlichen: Ein Sprechtag ohne
 * Schulkontakt ist unvollständig, egal in welchem Status, und jedes „die Eltern wenden sich an die
 * Schule" der übrigen Abläufe (Absage, Änderungswunsch) liefe ins Leere.
 *
 * <p>Prüfinhalt ist bei Freitext zwangsläufig nur „nicht leer". Der Wert wird getrimmt gespeichert,
 * damit die Mail-Vorlage ihn unbesehen einsetzen kann.
 */
public record Schulkontakt(String text) {

  public Schulkontakt {
    Objects.requireNonNull(text, "text");
    text = text.trim();
    if (text.isEmpty()) {
      throw new IllegalArgumentException("Ein Sprechtag ohne Schulkontakt ist unvollständig");
    }
  }

  public static Schulkontakt von(String text) {
    return new Schulkontakt(text);
  }
}
