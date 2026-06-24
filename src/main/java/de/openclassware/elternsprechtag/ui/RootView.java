package de.openclassware.elternsprechtag.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@PermitAll
@Route(value = "", autoLayout = false)
public class RootView extends Div implements BeforeEnterObserver {

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    boolean isOrganizer =
        authentication != null
            && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZER"));

    if (isOrganizer) {
      event.forwardTo(OrganizerView.class);
    }
  }
}
