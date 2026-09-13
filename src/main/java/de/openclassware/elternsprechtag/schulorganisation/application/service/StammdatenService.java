package de.openclassware.elternsprechtag.schulorganisation.application.service;

import de.openclassware.elternsprechtag.schulorganisation.application.port.in.Stammdaten;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Stammdatenansichten;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Stammdatenansichten.KlasseDaten;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Stammdatenansichten.LehrauftragDaten;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Beantwortet die Fragen, die der Sprechtag-Kontext an die Schulorganisation stellt — und sonst
 * nichts.
 *
 * <p>Er entscheidet wenig und reicht viel durch, und das ist hier die richtige Menge: Die Abfragen
 * sind reine Leseabfragen, ihre Regeln (welche Datensätze zählen als aktiv, in welcher Reihenfolge)
 * stehen im SQL des Query-Adapters. Was dieser Use Case leistet, ist die <em>Grenze</em> — er
 * übersetzt die Read-Modelle dieses Kontexts in die Records seines Eingangs-Ports, damit von innen
 * nichts nach außen durchschlägt. Fiele er weg, hinge der fremde Kontext an einem Port, der
 * ausdrücklich der Persistenz gehört.
 */
@RequiredArgsConstructor
@Service
class StammdatenService implements Stammdaten {

  private final Stammdatenansichten ansichten;

  @Override
  @Transactional(readOnly = true)
  public List<LehrauftragSchnappschuss> lehrauftraegeEinerKlasse(UUID klasseId) {
    return ansichten.lehrauftraegeEinerKlasse(klasseId).stream()
        .map(StammdatenService::zuSchnappschuss)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<LehrauftragSchnappschuss> lehrauftrag(UUID lehrauftragId) {
    return ansichten.lehrauftrag(lehrauftragId).map(StammdatenService::zuSchnappschuss);
  }

  @Override
  @Transactional(readOnly = true)
  public List<KlasseOption> waehlbareKlassen() {
    return alsOptionen(ansichten.aktiveKlassen());
  }

  @Override
  @Transactional(readOnly = true)
  public List<KlasseOption> alleKlassen() {
    return alsOptionen(ansichten.alleKlassen());
  }

  private static List<KlasseOption> alsOptionen(List<KlasseDaten> klassen) {
    return klassen.stream().map(klasse -> new KlasseOption(klasse.id(), klasse.name())).toList();
  }

  private static LehrauftragSchnappschuss zuSchnappschuss(LehrauftragDaten daten) {
    return new LehrauftragSchnappschuss(
        daten.id(),
        daten.lehrkraftId(),
        daten.kuerzel(),
        daten.vorname(),
        daten.nachname(),
        daten.klasse(),
        daten.fach());
  }
}
