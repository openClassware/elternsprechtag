package de.openclassware.elternsprechtag.sprechtag.adapter.out.schulorganisation;

import de.openclassware.elternsprechtag.schulorganisation.application.port.in.Stammdaten;
import de.openclassware.elternsprechtag.schulorganisation.application.port.in.Stammdaten.LehrauftragSchnappschuss;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrauftragId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Die Grenze zur Schulorganisation, von dieser Seite aus gesehen: Er erfüllt den
 * {@code Lehrauftraege}-Port dieses Kontexts, indem er den {@code port/in} des anderen ruft.
 *
 * <p>Dass dieser Adapter in einem eigenen Paket neben {@code out/persistence} liegt, sagt genau das
 * aus, was zählt: Hier steht kein SQL. Der Zugriff geht durch einen fremden Use Case, der selbst
 * entscheidet, was er herausgibt — wechselt dessen Persistenz, merkt dieser Kontext nichts davon.
 *
 * <p>Übersetzt wird in beide Richtungen: nackte {@link UUID}s hinaus (der andere Kontext hat seine
 * eigenen typisierten Ids und soll unsere nicht kennen), typisierte Ids dieses Kontexts herein.
 */
@RequiredArgsConstructor
@Component
class LehrauftraegeAdapter implements Lehrauftraege {

  private final Stammdaten stammdaten;

  @Override
  public Optional<LehrauftragDaten> lade(LehrauftragId id) {
    return stammdaten.lehrauftrag(id.wert()).map(LehrauftraegeAdapter::zuDaten);
  }

  @Override
  public List<LehrauftragDaten> fuerKlasse(UUID klasseId) {
    return stammdaten.lehrauftraegeEinerKlasse(klasseId).stream()
        .map(LehrauftraegeAdapter::zuDaten)
        .toList();
  }

  private static LehrauftragDaten zuDaten(LehrauftragSchnappschuss schnappschuss) {
    return new LehrauftragDaten(
        LehrauftragId.von(schnappschuss.id()),
        LehrkraftId.von(schnappschuss.lehrkraftId()),
        schnappschuss.kuerzel(),
        schnappschuss.vorname(),
        schnappschuss.nachname(),
        schnappschuss.klasse(),
        schnappschuss.fach());
  }
}
