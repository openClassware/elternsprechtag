package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.services.BuchungService;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsAnfrage;
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftOption;
import de.openclassware.elternsprechtag.services.SprechtagService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class ElternsprechtagPresenter {

  private final SprechtagService sprechtagService;
  private final BuchungService buchungService;
  private final ElternsprechtagProperties properties;

  Optional<Sprechtag> findByAccessToken(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return Optional.empty();
    }
    return sprechtagService.findByAccessToken(accessToken);
  }

  List<LehrkraftOption> ladeLehrkraftOptionen(Sprechtag sprechtag, Klasse klasse) {
    return buchungService.ladeLehrkraftOptionen(sprechtag, klasse);
  }

  /** Persistiert den Eltern-Submit atomar. Wirft {@link BuchungService.TerminBelegtException}. */
  List<Buchung> buchen(BuchungsAnfrage anfrage) {
    return buchungService.buchen(anfrage);
  }

  String getSchoolname() {
    return properties.getSchoolname();
  }
}
