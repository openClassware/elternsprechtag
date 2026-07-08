package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.services.SprechtagService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ElternsprechtagPresenter {

  private final SprechtagService sprechtagService;
  private final ElternsprechtagProperties properties;

  public Optional<Sprechtag> findByAccessToken(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return Optional.empty();
    }
    return sprechtagService.findByAccessToken(accessToken);
  }

  public String getSchoolname() {
    return properties.getSchoolname();
  }
}
