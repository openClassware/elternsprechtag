package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Ob ein Slot überhaupt angeboten wird. Das ist eine <em>Absicht des Organizers</em> und deshalb
 * gespeichert — anders als „belegt", das aus der Existenz einer aktiven Buchung folgt und keine
 * eigene Spalte mehr hat (ADR 0003, ABDECKUNG.md Z. 230).
 */
public enum Verfuegbarkeit {
  VERFUEGBAR,
  ENTFAELLT
}
