package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl.KlasseOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagszugang;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Klassen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Klassen.KlasseDaten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.domain.AccessToken;
import de.openclassware.elternsprechtag.sprechtag.domain.KlasseId;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schließt einen Sprechtag mit dem Zugangs-Token aus dem Elternlink auf.
 *
 * <p>Gelesen wird hier ausnahmsweise über das Aggregat und nicht über einen Query-Port: Es geht um
 * genau einen Sprechtag, und die Frage „darf hier gebucht werden" ist keine Anzeigefrage — sie
 * gehört an dieselbe Stelle wie die Regel.
 *
 * <p>Ein leeres oder unbekanntes Token ergibt schlicht kein Ergebnis. Der Weg drumherum, wenn Eltern
 * ihren Link verloren haben, ist der Anruf in der Schule (`ABDECKUNG.md` Z. 98).
 */
@RequiredArgsConstructor
@Service
class SprechtagszugangService implements Sprechtagszugang {

  private final Sprechtage sprechtage;
  private final Klassen klassen;

  @Override
  @Transactional(readOnly = true)
  public Optional<OeffentlicherSprechtag> oeffne(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return Optional.empty();
    }
    return sprechtage.ladeNachAccessToken(AccessToken.von(accessToken)).map(this::zuAnsicht);
  }

  private OeffentlicherSprechtag zuAnsicht(Sprechtag sprechtag) {
    Set<KlasseId> teilnehmende = Set.copyOf(sprechtag.klassen());
    // Die Namen kommen aus der Schulorganisation, alphabetisch sortiert; ausgewählt wird daraus in
    // Java statt per Join über die Kontextgrenze.
    List<KlasseOption> auswahl =
        klassen.alle().stream()
            .filter(klasse -> teilnehmende.contains(KlasseId.von(klasse.id())))
            .map(klasse -> new KlasseOption(klasse.id(), klasse.name()))
            .collect(Collectors.toList());

    return new OeffentlicherSprechtag(
        sprechtag.id().wert(),
        sprechtag.titel(),
        sprechtag.datum(),
        sprechtag.zeitfenster().beginn(),
        sprechtag.zeitfenster().ende(),
        sprechtag.ort(),
        sprechtag.beschreibung(),
        sprechtag.slotdauer().minuten(),
        sprechtag.nimmtElternbuchungenAn(LocalDate.now()),
        sprechtag.status() == SprechtagStatus.ABGESAGT,
        auswahl);
  }
}
