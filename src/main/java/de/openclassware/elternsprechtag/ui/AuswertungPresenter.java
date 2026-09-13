package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class AuswertungPresenter {

  private final Auswerten auswerten;

  Optional<SprechtagAuswertung> werteAus(UUID sprechtagId) {
    return auswerten.werteAus(sprechtagId);
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
