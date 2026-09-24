package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Use Case: Sammelaktion „Lehrkraft fällt aus" (Issue #156) — der Organizer lässt einen oder
 * mehrere Termine einer Lehrkraft an einem veröffentlichten Sprechtag entfallen. Eine daran
 * hängende aktive Buchung wird mit-storniert, ihre Familie in einer Mail benachrichtigt.
 *
 * <p>Der Baustein ist der <b>Termin</b>, nicht die Lehrkraft: {@link #entfallenLassen} nimmt eine
 * Liste von Termin-Ids entgegen und prüft keine Lehrkraft-Zugehörigkeit. „Lehrkraft fällt aus" ist
 * die Erzählung des Ausfall-Dialogs über eine Terminauswahl, nicht der Vertrag dieses Ports.
 */
public interface EntfallenLassen {

  /**
   * Alle Slots dieser Lehrkraft an diesem Sprechtag, chronologisch — das Angebot des
   * Ausfall-Dialogs. Ein Read-Modell wie {@code Buchungsoptionen}: Es darf veraltet sein, die
   * Entscheidung fällt erneut am Aggregat.
   */
  List<SlotZeile> slots(UUID sprechtagId, UUID lehrkraftId);

  /**
   * Lässt die genannten Termine entfallen. Bereits entfallene Termine werden still übersprungen,
   * ein einzelner scheiternder Termin stoppt den Lauf nicht — die übrigen bleiben festgeschrieben.
   * Der Versand der Ausfall-Mail läuft nach Commit und asynchron; dieser Aufruf wartet nicht darauf.
   *
   * @return die Anzahl tatsächlich entfallener Termine und der benachrichtigten Adressen — das
   *     echte Ergebnis, nicht die Größe der Auswahl
   * @throws SprechtagNichtVeroeffentlichtException wenn der Sprechtag der Termine nicht
   *     veröffentlicht ist. Die Prüfung liegt hier und nicht nur in der Oberfläche: Die
   *     Auswertungs-Route ist per URL für jeden Sprechtag-Status erreichbar
   */
  Ergebnis entfallenLassen(List<UUID> terminIds);

  /** Zustand eines Slots aus Sicht der Ausfall-Sammelaktion. */
  enum SlotZustand {
    FREI,
    GEBUCHT,
    ENTFALLEN
  }

  /**
   * Ein Slot im Ausfall-Dialog. {@code schuelerName}, {@code elternName} und
   * {@code familienSchluessel} sind nur bei {@link SlotZustand#GEBUCHT} gesetzt, sonst
   * {@code null}. Der Familien-Schlüssel ist die Eltern-Adresse und wird nicht angezeigt — er
   * dient nur der lokalen Zählung betroffener Familien im Auswahl-Modell des Dialogs.
   */
  record SlotZeile(
      UUID terminId,
      LocalTime zeit,
      SlotZustand zustand,
      String schuelerName,
      String elternName,
      String familienSchluessel) {}

  /** Wie viele Termine tatsächlich entfallen sind und wie viele Adressen benachrichtigt wurden. */
  record Ergebnis(int entfalleneTermine, int benachrichtigteAdressen) {}
}
