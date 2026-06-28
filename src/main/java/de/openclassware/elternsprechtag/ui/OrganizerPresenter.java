package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class OrganizerPresenter {

    private final AuthenticationContext authenticationContext;

    String getUsername() {
        return authenticationContext.getPrincipalName().orElse("?");
    }


}
