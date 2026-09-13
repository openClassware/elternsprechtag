package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Signalisiert den Versuch, einen Sprechtag zurück auf Entwurf zu nehmen, obwohl bereits gebucht
 * wurde.
 *
 * <p>Der Rückweg löscht die materialisierten Termine — mit Buchungen daran wäre das stiller
 * Datenverlust, und die betroffenen Familien erführen nie davon. Wer einen gebuchten Sprechtag nicht
 * mehr halten kann, sagt ihn ab: Dieser Weg benachrichtigt (`ABDECKUNG.md` Z. 92).
 */
public class SprechtagHatBuchungenException extends RuntimeException {

  public SprechtagHatBuchungenException(String message) {
    super(message);
  }
}
