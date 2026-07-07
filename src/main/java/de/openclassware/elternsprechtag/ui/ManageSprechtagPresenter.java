package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.services.SprechtagService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ManageSprechtagPresenter {

  private final SprechtagService sprechtagService;

  public List<Sprechtag> findAllSprechtage() {
    return sprechtagService.findSprechtageSortedByStartDate();
  }
}
