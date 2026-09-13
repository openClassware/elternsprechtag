package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import com.vaadin.flow.spring.security.AuthenticationContext;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht.SprechtagZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class OrganizerPresenter {

  private static final Set<SprechtagStatus> AKTIVE_STATUS =
      EnumSet.of(SprechtagStatus.VEROEFFENTLICHT, SprechtagStatus.ENTWURF);

  private final AuthenticationContext authenticationContext;
  private final Sprechtagsuebersicht uebersicht;

  /** View-Model der Organizer-Übersicht: alles, was der View zum Rendern braucht — ohne Logik im View. */
  record OrganizerModel(String username, List<SprechtagZeile> upcoming) {
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

  private List<SprechtagZeile> activeAndDraftSprechtage() {
    return uebersicht.alle().stream()
        .filter(sprechtag -> AKTIVE_STATUS.contains(sprechtag.status()))
        .toList();
  }
}
