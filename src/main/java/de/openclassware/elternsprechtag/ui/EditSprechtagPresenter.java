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
public class EditSprechtagPresenter {

  private final KlassenService klassenService;
  private final SprechtagService sprechtagService;

  public List<KlasseOption> findAllKlassen() {
    return klassenService.findAllOptions();
  }

  public Optional<SprechtagForm> loadForm(UUID id) {
    return sprechtagService.loadForm(id);
  }

  public UUID save(UUID id, SprechtagForm form, SprechtagStatusEnum status) {
    return sprechtagService.createOrUpdate(id, form, status);
  }
}
