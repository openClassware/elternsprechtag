package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Klassen;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reicht die Klassen der Schulorganisation an die Oberfläche durch — ein Use Case ohne eigene Regel.
 *
 * <p>Er existiert trotzdem als eigener Eingang: Die Presenter rufen ausschließlich Use-Case-Ports,
 * und der Weg in einen Fremdkontext soll auch dann über eine benannte Stelle führen, wenn dabei
 * nichts zu entscheiden ist.
 */
@RequiredArgsConstructor
@Service
class KlassenauswahlService implements Klassenauswahl {

  private final Klassen klassen;

  @Override
  @Transactional(readOnly = true)
  public List<KlasseOption> alleKlassen() {
    return klassen.alle().stream().map(k -> new KlasseOption(k.id(), k.name())).toList();
  }
}
