package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.services.BuchungService;
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftPlan;
import de.openclassware.elternsprechtag.services.BuchungService.SprechtagAuswertung;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class AuswertungPresenter {

  private final BuchungService buchungService;

  Optional<SprechtagAuswertung> werteAus(UUID sprechtagId) {
    return buchungService.werteAus(sprechtagId);
  }

  /**
   * Filtert die Lehrkraft-Pläne auf genau eine Lehrkraft. {@code lehrerId == null} bedeutet „alle
   * Lehrkräfte". Reine Teilmengen-Bildung ohne Zustand (analog {@code ManageSprechtagPresenter.filter});
   * die aktuelle Auswahl hält der View.
   */
  List<LehrkraftPlan> filter(List<LehrkraftPlan> alle, UUID lehrerId) {
    if (lehrerId == null) {
      return alle;
    }
    return alle.stream().filter(plan -> plan.lehrerId().equals(lehrerId)).toList();
  }
}
