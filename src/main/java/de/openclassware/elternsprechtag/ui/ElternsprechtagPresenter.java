package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.services.BuchungService;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsAnfrage;
import de.openclassware.elternsprechtag.services.BuchungService.FachOption;
import de.openclassware.elternsprechtag.services.SprechtagService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ElternsprechtagPresenter {

  private final SprechtagService sprechtagService;
  private final BuchungService buchungService;
  private final ElternsprechtagProperties properties;

  public Optional<Sprechtag> findByAccessToken(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return Optional.empty();
    }
    return sprechtagService.findByAccessToken(accessToken);
  }

  public List<FachOption> ladeFachOptionen(Sprechtag sprechtag, Klasse klasse) {
    return buchungService.ladeFachOptionen(sprechtag, klasse);
  }

  /** Persistiert den Eltern-Submit atomar. Wirft {@link BuchungService.TerminBelegtException}. */
  public List<Buchung> buchen(BuchungsAnfrage anfrage) {
    return buchungService.buchen(anfrage);
  }

  public String getSchoolname() {
    return properties.getSchoolname();
  }
}
