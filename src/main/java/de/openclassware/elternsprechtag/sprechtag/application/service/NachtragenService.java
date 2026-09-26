package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen;
import de.openclassware.elternsprechtag.sprechtag.domain.Familie;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schreibt einen Organizer-Nachtrag fest — fachlich derselbe Vorgang wie der Eltern-Submit ({@link
 * de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen}), nur von jemand anderem
 * angestoßen. Die geteilte Mechanik (Aggregate laden, Zeitkonflikt prüfen, sofort speichern,
 * Ereignisse bündeln) liegt in {@link BuchungsVorgangService}; dieser Service trägt nur die eigene
 * Vorbedingung des Nachtragens: Der Sprechtag muss veröffentlicht sein.
 *
 * <p>Die Prüfung sitzt hier und nicht bloß in der Oberfläche, weil die Route per URL für jeden
 * Sprechtag-Status erreichbar ist — genau das Argument aus {@code StornierenService}. Sie läuft
 * <b>vor</b> der geteilten Zeitkonflikt-Prüfung: Ein Nachtrag an einem nicht veröffentlichten
 * Sprechtag ist der grundsätzlichere Fehler, und die Meldung soll nicht vom Zeitkonflikt
 * überdeckt werden, sobald beides zugleich zutrifft.
 */
@RequiredArgsConstructor
@Service
class NachtragenService implements Nachtragen {

  private final BuchungsVorgangService vorgang;

  @Override
  @Transactional
  public int trageNach(NachtragsAnfrage anfrage) {
    Familie familie =
        new Familie(anfrage.elternName(), anfrage.schuelerName(), anfrage.elternEmail());
    List<BuchungsVorgangService.Wunsch> wuensche = wuensche(anfrage.wuensche());
    // Vor jedem Schreiben geprüft — die Ablehnung darf keine halbe Buchung, kein Ereignis und
    // keine Mail hinterlassen.
    pruefeVeroeffentlicht(wuensche);
    List<Termin> geladen = vorgang.ladeUndPruefeZeitkonflikt(wuensche);
    return vorgang.schreibeFest(familie, wuensche, geladen);
  }

  private void pruefeVeroeffentlicht(List<BuchungsVorgangService.Wunsch> wuensche) {
    for (Sprechtag sprechtag : vorgang.sprechtageDer(wuensche)) {
      if (sprechtag.status() != SprechtagStatus.VEROEFFENTLICHT) {
        throw new SprechtagNichtVeroeffentlichtException(
            "Nachtrag nur an einem veröffentlichten Sprechtag, nicht bei " + sprechtag.status());
      }
    }
  }

  private static List<BuchungsVorgangService.Wunsch> wuensche(List<NachtragsWunsch> wuensche) {
    return wuensche.stream()
        .map(
            wunsch ->
                new BuchungsVorgangService.Wunsch(
                    wunsch.lehrauftragId(), wunsch.terminId(), wunsch.notiz()))
        .toList();
  }
}
