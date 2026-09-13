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
   * Was die Eltern-Ansicht zeigt.
   *
   * <p>Der Status steht hier bewusst nicht als Enum, sondern als die zwei Fragen, die die Ansicht
   * stellt: {@code buchbar} — darf gebucht werden; {@code abgesagt} — gibt es den eigenen
   * Absage-Hinweis. Entwurf und abgeschlossen sind für die Eltern schlicht nicht verfügbar und
   * brauchen keinen eigenen Namen.
   */
  record OeffentlicherSprechtag(
      UUID id,
      String titel,
      LocalDate datum,
      LocalTime beginn,
      LocalTime ende,
      String ort,
      String beschreibung,
      int slotInMinuten,
      boolean buchbar,
      boolean abgesagt,
      List<KlasseOption> klassen) {}
}
