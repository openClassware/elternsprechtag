package de.openclassware.elternsprechtag.sprechtag.domain;

/** Eine zugesagte Buchung wurde zurückgenommen; der Slot ist damit wieder buchbar. */
public record BuchungStorniert(TerminId termin, BuchungId buchung) implements Ereignis {}
