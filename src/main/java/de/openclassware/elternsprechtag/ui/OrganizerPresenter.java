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

  String getUsername() {
    return authenticationContext.getPrincipalName().orElse("?");
  }

  List<SprechtagRow> findActiveAndDraftSprechtage() {
    return sprechtagService.findAllRows().stream()
        .filter(sprechtag -> AKTIVE_STATUS.contains(sprechtag.status()))
        .toList();
  }
}
