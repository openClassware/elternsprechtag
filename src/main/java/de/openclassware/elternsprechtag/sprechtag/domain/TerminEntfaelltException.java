package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Der Slot ist vom Organizer aus dem Angebot genommen ({@link Verfuegbarkeit#ENTFAELLT}). Bewusst
 * nicht derselbe Fall wie „belegt": Hier wird auch später niemand buchen können.
 */
public class TerminEntfaelltException extends RuntimeException {

  public TerminEntfaelltException(String message) {
    super(message);
  }
}
