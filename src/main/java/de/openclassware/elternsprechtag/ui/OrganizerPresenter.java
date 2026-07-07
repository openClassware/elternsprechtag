package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.spring.security.AuthenticationContext;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.services .SprechtagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    List<Sprechtag> findActiveAndDraftSprechtage() {
        return sprechtagService.findSprechtageSortedByStartDate().stream()
                .filter(sprechtag -> AKTIVE_STATUS.contains(sprechtag.getStatus()))
                .toList();
    }




}
