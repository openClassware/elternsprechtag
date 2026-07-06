package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import java.util.List;

import de.openclassware.elternsprechtag.services.KlassenService;
import de.openclassware.elternsprechtag.services.SprechtagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EditSprechtagPresenter {

    private final KlassenService klassenService;
    private final SprechtagService sprechtagService;

  public List<Klasse> findAllKlassen() {
    return klassenService.findAllOrderByName();
  }

  public Sprechtag save(Sprechtag sprechtag) {
    return sprechtagService.save(sprechtag);
  }

}
