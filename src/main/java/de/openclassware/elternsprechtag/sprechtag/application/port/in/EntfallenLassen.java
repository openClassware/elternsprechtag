package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import java.util.List;
import java.util.UUID;

/**
 * Use Case: Der Organizer lässt eine Auswahl von Terminen entfallen — der Kern der Sammelaktion
 * „Lehrkraft fällt aus" (#156). Danach gilt für jeden dieser Termine: Er ist nicht mehr buchbar,
 * und eine daran hängende Buchung ist storniert.
 *
 * <p>Der Port ist <b>termin-zentriert</b>: Er nimmt Termin-Ids und prüft <em>keine</em>
 * Lehrkraft-Zugehörigkeit. Der Ausfall ist real selten ein ganzer Tag — wer erst ab 16 Uhr kommt,
 * fällt für die frühen Slots aus —, und die Lehrkraft bleibt Stammdatum ohne Tageszustand.
 * „Lehrkraft fällt aus" ist die Erzählung des Dialogs über eine Terminauswahl, nicht der Vertrag
 * hier.
 *
 * <p>Es gibt keinen Gegen-Use-Case: Ein entfallener Termin wird nie wieder angeboten.
 */
public interface EntfallenLassen {

  /**
   * Lässt die genannten Termine entfallen und storniert dabei ihre Buchungen.
   *
   * <p><b>Eine Transaktion je Termin</b>, nicht eine über alle: Scheitert ein einzelner, wird er
   * protokolliert und übersprungen, während die übrigen durchlaufen und festgeschrieben bleiben.
   * Ein Rollback über alles wäre hier die falsche Semantik — er machte die bereits abgesagten
   * Termine wieder buchbar, obwohl die Lehrkraft fehlt. Vorwärtskommen schlägt Atomarität; beim
   * Eltern-Submit (ADR 0005) ist es genau andersherum.
   *
   * <p>Termin-Ids, die es nicht gibt oder die bereits entfallen sind, werden <b>still</b>
   * übersprungen: Der zweite Klick aus einem alten Browser-Tab ist kein Fehler und löst keine
   * zweite Benachrichtigung aus.
   *
   * @param terminIds die Auswahl des Organizers; Dubletten und Unbekanntes schaden nicht
   * @return was tatsächlich geschehen ist, nicht die Größe der Auswahl
   * @throws SprechtagNichtVeroeffentlichtException wenn ein Sprechtag der Auswahl nicht
   *     veröffentlicht ist. Die Prüfung liegt hier und nicht nur in der Oberfläche: Die
   *     Auswertungs-Route ist per URL für jeden Sprechtag-Status erreichbar. Sie geht dem stillen
   *     Überspringen <b>vor</b> und greift, bevor ein einziger Termin geschrieben ist — wird
   *     abgewiesen, hat der Vorgang nichts hinterlassen
   */
  Ausfallergebnis lassEntfallen(List<UUID> terminIds);

  /**
   * Das Ergebnis eines Ausfall-Vorgangs — die Zahlen, die die Oberfläche danach meldet.
   *
   * @param entfalleneTermine wie viele Termine durch diesen Aufruf entfallen sind
   * @param betroffeneAdressen wie viele <em>verschiedene</em> Eltern-Adressen eine stornierte
   *     Buchung hatten. Die Adresse ist der Bündelungsschlüssel, nicht ein Familien-Objekt: Eine
   *     Familie mit zwei entfallenden Terminen zählt einmal
   */
  record Ausfallergebnis(int entfalleneTermine, int betroffeneAdressen) {}
}
