package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.services.KlassenService;
import de.openclassware.elternsprechtag.services.KlassenService.KlasseOption;
import de.openclassware.elternsprechtag.services.SprechtagService;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagForm;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class EditSprechtagPresenter {

  private final KlassenService klassenService;
  private final SprechtagService sprechtagService;

  List<KlasseOption> findAllKlassen() {
    return klassenService.findAllOptions();
  }

  Optional<SprechtagForm> loadForm(UUID id) {
    return sprechtagService.loadForm(id);
  }

  /**
   * Ob der Schulkontakt für diesen Zielstatus ausgefüllt sein muss. Nur beim Veröffentlichen — ein
   * Entwurf darf ohne ihn gespeichert werden. Die Entscheidung gehört hierher und nicht in den
   * View; die Wahrheit steht im {@link SprechtagService}, hier hängt nur die Nutzerführung dran.
   */
  boolean schulkontaktErforderlich(SprechtagStatusEnum status) {
    return status == SprechtagStatusEnum.VEROEFFENTLICHT;
  }

  UUID save(UUID id, SprechtagForm form, SprechtagStatusEnum status) {
    return sprechtagService.createOrUpdate(id, form, status);
  }
}
