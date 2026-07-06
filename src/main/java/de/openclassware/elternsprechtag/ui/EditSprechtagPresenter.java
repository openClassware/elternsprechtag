package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.domain.Klasse;
import java.util.List;

import de.openclassware.elternsprechtag.services.KlassenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EditSprechtagPresenter {

    private final KlassenService klassenService;

  public List<Klasse> findAllKlassen() {
    return klassenService.findAllOrderByName();
  }

}
