package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Ein Slot fällt aus — Absicht des Organizers, keine Folge einer Buchung (Issue #156). Eine daran
 * hängende aktive Buchung wird in derselben Aggregat-Operation storniert und meldet ihr eigenes
 * {@link BuchungStorniert}.
 */
public record TerminEntfallen(TerminId termin) implements Ereignis {}
