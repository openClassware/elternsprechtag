package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Auf einem Termin wurde eine Buchung zugesagt. Feinkörnig und von {@link Termin} gemeldet, weil
 * mehrere Wege zu derselben Zustandsänderung führen (Eltern-Submit, Organizer-Nachtrag, Umbuchen)
 * und alle dieselbe Bestätigung auslösen sollen.
 */
public record BuchungAngelegt(TerminId termin, BuchungId buchung) implements Ereignis {}
