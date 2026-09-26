package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl.KlasseOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: der Blick der Eltern auf einen Sprechtag, aufgeschlossen mit dem Zugangs-Token aus ihrem
 * Link. Es gibt bewusst keine Eltern-Accounts — dieses Token ist die ganze Zugangsprüfung.
 */
public interface Sprechtagszugang {

  /** Leeres Optional, wenn kein Sprechtag zu diesem Token gehört. */
  Optional<OeffentlicherSprechtag> oeffne(String accessToken);

  /**
   * Was der Elternlink heute zeigt.
   *
   * <p>Ein Enum und nicht zwei Fragen als Booleans: Mit „Anmeldung beendet" (Issue #123) und später
   * „vorbei" (#131) wären das sich ausschließende Flags, deren Reihenfolge der Presenter kennen
   * müsste. Der Stand liegt bewusst im Port und nicht in der Domäne — er ist die Sicht der Eltern,
   * kein Status des Sprechtags. Entwurf und abgeschlossen sind für sie schlicht {@link
   * #NICHT_VERFUEGBAR}.
   */
  enum Zugangsstand {
    BUCHBAR,
    ANMELDUNG_BEENDET,
    ABGESAGT,
    NICHT_VERFUEGBAR
  }

  /** Was die Eltern-Ansicht zeigt. {@code schulkontakt} ist der Weg zur Schule, als Text. */
  record OeffentlicherSprechtag(
      UUID id,
      String titel,
      LocalDate datum,
      LocalTime beginn,
      LocalTime ende,
      String ort,
      String beschreibung,
      int slotInMinuten,
      Zugangsstand stand,
      String schulkontakt,
      List<KlasseOption> klassen) {}
}
