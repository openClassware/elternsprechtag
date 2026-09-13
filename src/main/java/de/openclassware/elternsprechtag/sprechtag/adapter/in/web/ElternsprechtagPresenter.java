package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.LehrkraftOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagszugang;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagszugang.OeffentlicherSprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class ElternsprechtagPresenter {

  private final Sprechtagszugang sprechtagszugang;
  private final Buchungsoptionen buchungsoptionen;
  private final Buchen buchenUseCase;
  private final ElternsprechtagProperties properties;

  /** Welcher Screen der Eltern-View aus dem Zugriff folgt. */
  enum Zugang {
    BUCHBAR,
    ABGESAGT,
    NICHT_VERFUEGBAR
  }

  /** Ergebnis der Zugriffsprüfung; {@code sprechtag} ist nur bei {@link Zugang#BUCHBAR} gesetzt. */
  record ZugangsErgebnis(Zugang zugang, OeffentlicherSprechtag sprechtag) {}

  /**
   * Entscheidet aus Token + Status, welcher Screen erscheint: veröffentlicht → buchbar,
   * abgesagt → Absage-Hinweis, sonst (unbekannt/Entwurf/abgeschlossen) → nicht verfügbar.
   */
  ZugangsErgebnis pruefeZugang(String accessToken) {
    Optional<OeffentlicherSprechtag> gefunden = sprechtagszugang.oeffne(accessToken);
    if (gefunden.isEmpty()) {
      return new ZugangsErgebnis(Zugang.NICHT_VERFUEGBAR, null);
    }
    OeffentlicherSprechtag sprechtag = gefunden.get();
    if (sprechtag.buchbar()) {
      return new ZugangsErgebnis(Zugang.BUCHBAR, sprechtag);
    }
    return new ZugangsErgebnis(
        sprechtag.abgesagt() ? Zugang.ABGESAGT : Zugang.NICHT_VERFUEGBAR, null);
  }

  List<LehrkraftOption> ladeLehrkraftOptionen(UUID sprechtagId, UUID klasseId) {
    return buchungsoptionen.ladeLehrkraftOptionen(sprechtagId, klasseId);
  }

  /**
   * Schreibt den Eltern-Submit atomar fest und gibt die Anzahl gebuchter Termine zurück. Wirft
   * {@link TerminBelegtException}.
   */
  int buchen(BuchungsAnfrage anfrage) {
    return buchenUseCase.buchen(anfrage);
  }

  String getSchoolname() {
    return properties.getSchoolname();
  }
}
