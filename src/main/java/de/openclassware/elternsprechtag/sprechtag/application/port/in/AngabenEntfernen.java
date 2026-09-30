package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNichtGefundenException;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungNochAktivException;
import java.util.UUID;

/**
 * Use Case: der Organizer entfernt die Angaben einer einzelnen Buchung — das Löschverlangen einer
 * Familie (Issue #129). Die Anonymisierung nach Ablauf der Aufbewahrungsfrist, vorgezogen auf eine
 * Buchung: Die Familie weicht einem Pseudonym, die Notiz fällt, Status und Belegung bleiben — die
 * Auslastungszahl bleibt unverfälscht.
 *
 * <p>Genau eine Buchung, keine Familie: Weitere Termine derselben Familie entfernt der Organizer
 * einzeln. Ein Protokoll, wer entfernt hat, gibt es bewusst nicht — nur den Zeitpunkt an der
 * Buchung. Verschickt wird nichts.
 */
public interface AngabenEntfernen {

  /**
   * Entfernt die Angaben der Buchung. Eine Buchung, deren Angaben schon gefallen sind, bleibt
   * unverändert — ein zweiter Klick aus einem alten Tab ist kein Fehler.
   *
   * @throws BuchungNichtGefundenException wenn es die Buchung nicht gibt
   * @throws BuchungNochAktivException wenn sie an einem veröffentlichten Sprechtag noch gilt — dort
   *     ist das Löschverlangen ein Storno mit entfernten Angaben ({@link Stornieren})
   */
  void entferne(UUID buchungId);
}
