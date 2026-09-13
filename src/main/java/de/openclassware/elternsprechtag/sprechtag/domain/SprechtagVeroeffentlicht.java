package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Ein Sprechtag ist veröffentlicht: Ab jetzt trägt der Elternlink, und die Zeitstruktur ist
 * eingefroren.
 */
public record SprechtagVeroeffentlicht(SprechtagId sprechtag) implements Ereignis {}
