package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.domain.Pseudonymisierung;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Die eine Quelle für {@link Pseudonymisierung}en des Betriebs — mit der Ersatz-E-Mail aus
 * {@code elternsprechtag.anonymisierung-email}.
 *
 * <p>Vier Use Cases lassen Angaben fallen: der nächtliche Lauf (#126), das Storno mit entfernten
 * Angaben, das Umbuchen für die alte Buchung und die Einzelaktion der Auswertung (#129). Läsen alle
 * die Property selbst, könnten die Pseudonyme auseinanderlaufen.
 */
@Component
class Pseudonymgeber {

  private final String ersatzEmail;

  Pseudonymgeber(@Value("${elternsprechtag.anonymisierung-email}") String ersatzEmail) {
    // Einmal zur Probe: Eine leer eingestellte Adresse soll den Start scheitern lassen, nicht erst
    // den nächtlichen Lauf.
    Pseudonymisierung.neu(ersatzEmail);
    this.ersatzEmail = ersatzEmail;
  }

  /** Ein neuer Lauf mit eigenem Seed. Ein Objekt je Vorgang — es zählt seine Nummern selbst. */
  Pseudonymisierung neuerLauf() {
    return Pseudonymisierung.neu(ersatzEmail);
  }
}
