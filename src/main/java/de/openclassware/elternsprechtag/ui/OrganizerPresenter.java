package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.spring.security.AuthenticationContext;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.services.SprechtagService;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagRow;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class OrganizerPresenter {

  private static final Set<SprechtagStatusEnum> AKTIVE_STATUS =
      EnumSet.of(SprechtagStatusEnum.VEROEFFENTLICHT, SprechtagStatusEnum.ENTWURF);

  private final AuthenticationContext authenticationContext;
  private final SprechtagService sprechtagService;

  /** View-Model der Organizer-Übersicht: alles, was der View zum Rendern braucht — ohne Logik im View. */
  record OrganizerModel(String username, List<SprechtagRow> upcoming) {
    boolean hasUpcoming() {
      return !upcoming.isEmpty();
    }
  }

  OrganizerModel load() {
    return new OrganizerModel(username(), activeAndDraftSprechtage());
  }

  private String username() {
    return authenticationContext.getPrincipalName().orElse("?");
  }

  private List<SprechtagRow> activeAndDraftSprechtage() {
    return sprechtagService.findAllRows().stream()
        .filter(sprechtag -> AKTIVE_STATUS.contains(sprechtag.status()))
        .toList();
  }
}
