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

  /**
   * Ob der eingegebene Schulkontakt für diesen Zielstatus durchgeht. Die ganze Prüfformel liegt
   * hier, damit der View keine halbe Regel mitträgt: Er fragt nur, ob der Wert gültig ist, und
   * zeigt die Meldung an. Prüfinhalt ist bei Freitext zwangsläufig nur „nicht leer".
   */
  boolean schulkontaktGueltig(SprechtagStatusEnum status, String wert) {
    return !schulkontaktErforderlich(status) || (wert != null && !wert.isBlank());
  }

  UUID save(UUID id, SprechtagForm form, SprechtagStatusEnum status) {
    return sprechtagService.createOrUpdate(id, form, status);
  }
}
