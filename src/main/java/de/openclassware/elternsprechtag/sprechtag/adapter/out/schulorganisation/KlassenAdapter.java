package de.openclassware.elternsprechtag.sprechtag.adapter.out.schulorganisation;

import de.openclassware.elternsprechtag.schulorganisation.application.port.in.Stammdaten;
import de.openclassware.elternsprechtag.schulorganisation.application.port.in.Stammdaten.KlasseOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Klassen;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Der zweite Weg über die Kontextgrenze, neben {@link LehrauftraegeAdapter}: die Klassen der
 * Schulorganisation, gelesen über deren {@code port/in}.
 */
@RequiredArgsConstructor
@Component
class KlassenAdapter implements Klassen {

  private final Stammdaten stammdaten;

  @Override
  public List<KlasseDaten> waehlbare() {
    return zuDaten(stammdaten.waehlbareKlassen());
  }

  @Override
  public List<KlasseDaten> alle() {
    return zuDaten(stammdaten.alleKlassen());
  }

  private static List<KlasseDaten> zuDaten(List<KlasseOption> optionen) {
    return optionen.stream().map(option -> new KlasseDaten(option.id(), option.name())).toList();
  }
}
