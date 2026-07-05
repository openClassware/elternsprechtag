package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.spring.security.AuthenticationContext;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.services .SprechtagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
class OrganizerPresenter {

    private final AuthenticationContext authenticationContext;
    private final SprechtagService sprechtagService;

    String getUsername() {
        return authenticationContext.getPrincipalName().orElse("?");
    }

    List<Sprechtag> findAllSprechtage() {
        return sprechtagService.findSprechtageSortedByStartDate();
    }




}
