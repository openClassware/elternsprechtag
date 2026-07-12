package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.services.BuchungService;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsAnfrage;
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftOption;
import de.openclassware.elternsprechtag.services.SprechtagService;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagPublic;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class ElternsprechtagPresenter {

  private final SprechtagService sprechtagService;
  private final BuchungService buchungService;
  private final ElternsprechtagProperties properties;

  Optional<SprechtagPublic> findByAccessToken(String accessToken) {
    return sprechtagService.findPublicByAccessToken(accessToken);
  }

  List<LehrkraftOption> ladeLehrkraftOptionen(UUID sprechtagId, UUID klasseId) {
    return buchungService.ladeLehrkraftOptionen(sprechtagId, klasseId);
  }

  /** Persistiert den Eltern-Submit atomar. Wirft {@link BuchungService.TerminBelegtException}. */
  List<Buchung> buchen(BuchungsAnfrage anfrage) {
    return buchungService.buchen(anfrage);
  }

  String getSchoolname() {
    return properties.getSchoolname();
  }
}
