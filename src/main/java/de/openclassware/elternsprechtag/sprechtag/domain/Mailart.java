package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Die vier Nachrichten, die eine Familie zu ihrer Buchung bekommen kann — je eine Zweckbindung der
 * Eltern-Adresse (ADR 0001, 0002, 0006). Ein Umbuchen oder Nachtragen bestätigt ebenfalls; der
 * Anlass ändert den Text, nicht die Art.
 */
public enum Mailart {
  BESTAETIGUNG,
  ERINNERUNG,
  ABSAGE,
  AUSFALL
}
