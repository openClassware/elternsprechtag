package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen;
import de.openclassware.elternsprechtag.sprechtag.domain.Familie;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schreibt einen Eltern-Submit fest — alles oder nichts. Die eigentliche Mechanik (Aggregate laden,
 * Zeitkonflikt prüfen, sofort speichern, Ereignisse bündeln) liegt in {@link
 * BuchungsVorgangService} und wird sich mit dem Organizer-Nachtrag geteilt; dieser Service trägt
 * nur die Vorbedingungen des Eltern-Submits — hier: keine weiteren, die Route ist der Elternlink
 * eines veröffentlichten Sprechtags.
 */
@RequiredArgsConstructor
@Service
class BuchenService implements Buchen {

  private final BuchungsVorgangService vorgang;

  @Override
  @Transactional
  public int buchen(BuchungsAnfrage anfrage) {
    Familie familie =
        new Familie(anfrage.elternName(), anfrage.schuelerName(), anfrage.elternEmail());
    List<BuchungsVorgangService.Wunsch> wuensche = wuensche(anfrage.wuensche());
    List<Termin> geladen = vorgang.ladeUndPruefeZeitkonflikt(wuensche);
    return vorgang.schreibeFest(familie, wuensche, geladen);
  }

  private static List<BuchungsVorgangService.Wunsch> wuensche(List<BuchungsWunsch> wuensche) {
    return wuensche.stream()
        .map(
            wunsch ->
                new BuchungsVorgangService.Wunsch(
                    wunsch.lehrauftragId(), wunsch.terminId(), wunsch.notiz()))
        .toList();
  }
}
