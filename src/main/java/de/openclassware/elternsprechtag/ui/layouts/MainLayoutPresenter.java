package de.openclassware.elternsprechtag.ui.layouts;

import com.vaadin.flow.spring.security.AuthenticationContext;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class MainLayoutPresenter {

  private final AuthenticationContext authenticationContext;
  private final ElternsprechtagProperties elternsprechtagProperties;

  String getUsername() {
    return authenticationContext.getPrincipalName().orElse("?");
  }

  String getAvatar() {
    return authenticationContext
            .getPrincipalName()
            .map(name -> name.substring(0, 1))
            .map(String::toUpperCase)
            .orElse("?");
  }

  String getSchoolname() {
    return elternsprechtagProperties.getSchoolname();
  }

  void logout() {
    authenticationContext.logout();
  }
}
