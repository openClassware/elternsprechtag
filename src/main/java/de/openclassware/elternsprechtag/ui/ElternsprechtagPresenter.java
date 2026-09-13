package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.services.SprechtagService;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagPublic;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.LehrkraftOption;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class ElternsprechtagPresenter {

  private final SprechtagService sprechtagService;
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
  record ZugangsErgebnis(Zugang zugang, SprechtagPublic sprechtag) {}

  /**
   * Entscheidet aus Token + Status, welcher Screen erscheint: veröffentlicht → buchbar,
   * abgesagt → Absage-Hinweis, sonst (unbekannt/Entwurf/abgeschlossen) → nicht verfügbar.
   */
  ZugangsErgebnis pruefeZugang(String accessToken) {
    Optional<SprechtagPublic> gefunden = sprechtagService.findPublicByAccessToken(accessToken);
    if (gefunden.isEmpty()) {
      return new ZugangsErgebnis(Zugang.NICHT_VERFUEGBAR, null);
    }
    SprechtagPublic sprechtag = gefunden.get();
    return switch (sprechtag.status()) {
      case VEROEFFENTLICHT -> new ZugangsErgebnis(Zugang.BUCHBAR, sprechtag);
      case ABGESAGT -> new ZugangsErgebnis(Zugang.ABGESAGT, null);
      case ENTWURF, ABGESCHLOSSEN -> new ZugangsErgebnis(Zugang.NICHT_VERFUEGBAR, null);
    };
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
