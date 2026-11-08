package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts;

import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts.Rechtslinks.Rechtslink;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class SeitenLayoutPresenter {

  private final ElternsprechtagProperties elternsprechtagProperties;

  List<Rechtslink> getRechtslinks() {
    return Rechtslinks.aus(
        elternsprechtagProperties.getImpressumUrl(),
        elternsprechtagProperties.getDatenschutzerklaerungUrl());
  }
}
