package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Klassen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Klassen.KlasseDaten;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stellt zusammen, was das Bearbeiten-Formular zur Wahl stellen darf.
 *
 * <p>Er existiert auch deshalb als eigener Eingang, weil die Presenter ausschließlich
 * Use-Case-Ports rufen und der Weg in einen Fremdkontext über eine benannte Stelle führen soll.
 * Seine Regel hat er aber selbst: aktive Klassen <em>plus</em> die bereits gewählten. Sie steht
 * hier und nicht im View, weil eine falsche Antwort darauf einem Entwurf stillschweigend eine
 * Klasse nimmt.
 */
@RequiredArgsConstructor
@Service
class KlassenauswahlService implements Klassenauswahl {

  private final Klassen klassen;

  @Override
  @Transactional(readOnly = true)
  public List<KlasseOption> waehlbareKlassen(Collection<UUID> bereitsGewaehlt) {
    List<KlasseDaten> waehlbar = klassen.waehlbare();
    if (bereitsGewaehlt.isEmpty()) {
      return waehlbar.stream().map(KlassenauswahlService::zuOption).toList();
    }

    // Zwei Abfragen statt einer: Der Fremdkontext beantwortet „aktiv?" und „wie heißt sie?"
    // getrennt, und über die Grenze joint nichts (ADR 0003). Eine Schule hat Dutzende Klassen.
    Set<UUID> aktiv = new HashSet<>();
    for (KlasseDaten klasse : waehlbar) {
      aktiv.add(klasse.id());
    }
    List<KlasseOption> optionen = new ArrayList<>();
    // `alle()` kommt bereits nach Namen sortiert; die Auswahl daraus erhält die Reihenfolge.
    for (KlasseDaten klasse : klassen.alle()) {
      if (aktiv.contains(klasse.id()) || bereitsGewaehlt.contains(klasse.id())) {
        optionen.add(zuOption(klasse));
      }
    }
    return optionen;
  }

  private static KlasseOption zuOption(KlasseDaten klasse) {
    return new KlasseOption(klasse.id(), klasse.name());
  }
}
