package de.openclassware.elternsprechtag.sprechtag.domain;

/** Die Erinnerung einer zugesagten Buchung wurde als versendet markiert (Issue #107). */
public record BuchungErinnert(TerminId termin, BuchungId buchung) implements Ereignis {}
