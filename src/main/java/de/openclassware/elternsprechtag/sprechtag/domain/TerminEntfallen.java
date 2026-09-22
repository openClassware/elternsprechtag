package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Ein Slot ist aus dem Angebot genommen: Die Lehrkraft steht zu dieser Zeit nicht bereit, und der
 * Termin ist nie wieder buchbar. Es gibt kein Gegenstück — ein entfallener Termin wird nicht
 * zurückgeholt (#156).
 *
 * <p>Hing eine aktive Buchung daran, meldet dasselbe {@code lassEntfallen} zusätzlich ein
 * {@link BuchungStorniert}. An dem hängt die Benachrichtigung der Familie; dieses Ereignis hier
 * bezeugt nur den Slot.
 */
public record TerminEntfallen(TerminId termin) implements Ereignis {}
