package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.StatusuebergangException;
import de.openclassware.elternsprechtag.sprechtag.domain.ZeitstrukturEingefrorenException;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: einen bestehenden Sprechtag bearbeiten — Formular laden und zurückschreiben.
 *
 * <p>Was sich dabei noch ändern lässt, entscheidet das Aggregat: Titel, Ort, Hinweistext,
 * Schulkontakt, Zugangs-Token, Erinnerungsvorlauf und Anmeldefrist bis zum Endzustand, Datum,
 * Zeitfenster, Slot-Dauer und Klassen nur im Entwurf.
 */
public interface Bearbeiten {

  /** Leeres Optional, wenn der Sprechtag nicht existiert. */
  Optional<SprechtagFormular> ladeFormular(UUID id);

  /**
   * Schreibt das Formular zurück. Der Status bleibt, wie er ist — gewechselt wird ausschließlich
   * über die dafür vorgesehenen Use Cases.
   *
   * @throws ZeitstrukturEingefrorenException bei einer Änderung an der Zeitstruktur eines
   *     veröffentlichten Sprechtags
   * @throws StatusuebergangException an einem abgesagten oder abgeschlossenen Sprechtag
   */
  void bearbeite(UUID id, SprechtagFormular formular);
}
