package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Klassen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Klassen.KlasseDaten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baut die Sprechtag-Liste des Organizers.
 *
 * <p>Wie die Auswertung entsteht sie aus <b>zwei</b> Ports und wird hier in Java zusammengefügt: die
 * Sprechtage aus dem Query-Port dieses Kontexts, die Klassennamen aus der Schulorganisation. Kein
 * SQL-Statement joint über die Kontextgrenze (ADR 0003) — eine Schule hat Dutzende Klassen, die
 * Kosten sind vernachlässigbar.
 *
 * <p>Eine Klasse, die es nicht mehr gibt, erscheint schlicht nicht im Namen-Register und fällt aus
 * der Liste: Der Sprechtag zeigt, was heute noch da ist.
 */
@RequiredArgsConstructor
@Service
class SprechtagsuebersichtService implements Sprechtagsuebersicht {

  private final SprechtagAnsichten ansichten;
  private final Klassen klassen;

  @Override
  @Transactional(readOnly = true)
  public List<SprechtagZeile> alle() {
    Map<UUID, String> namen = new LinkedHashMap<>();
    for (KlasseDaten klasse : klassen.alle()) {
      namen.put(klasse.id(), klasse.name());
    }

    List<SprechtagZeile> zeilen = new ArrayList<>();
    for (SprechtagAnsichten.SprechtagZeile roh : ansichten.alle()) {
      zeilen.add(
          new SprechtagZeile(
              roh.id(),
              roh.titel(),
              roh.datum(),
              roh.beginn(),
              roh.ende(),
              roh.ort(),
              roh.status(),
              roh.accessToken(),
              // Der Port liefert die Klassen bereits alphabetisch; die Auswahl daraus erhält die
              // Reihenfolge.
              namen.entrySet().stream()
                  .filter(eintrag -> roh.klasseIds().contains(eintrag.getKey()))
                  .map(Map.Entry::getValue)
                  .toList()));
    }
    return zeilen;
  }
}
