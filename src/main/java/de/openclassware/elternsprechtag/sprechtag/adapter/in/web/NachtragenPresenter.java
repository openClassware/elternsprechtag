package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Bearbeiten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.LehrkraftOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl.KlasseOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class NachtragenPresenter {

  private final Bearbeiten bearbeiten;
  private final Klassenauswahl klassenauswahl;
  private final Buchungsoptionen buchungsoptionen;
  private final Nachtragen nachtragenUseCase;
  private final ElternsprechtagProperties properties;

  /**
   * Kopfdaten des Sprechtags, wie sie die Nachtrag-Ansicht zeigt — dieselben Felder wie
   * {@code Sprechtagszugang.OeffentlicherSprechtag}, nur über das Bearbeiten-Formular gelesen: Die
   * Organizer-Strecke kennt keinen Zugangs-Token, sondern die Sprechtag-Id aus der Route.
   */
  record SprechtagKopf(
      String titel,
      LocalDate datum,
      LocalTime beginn,
      LocalTime ende,
      String ort,
      String beschreibung,
      int slotInMinuten,
      List<KlasseOption> klassen) {}

  /** Leeres Optional, wenn es zu dieser Id keinen Sprechtag gibt. */
  Optional<SprechtagKopf> ladeSprechtag(UUID sprechtagId) {
    return bearbeiten.ladeFormular(sprechtagId).map(this::zuKopf);
  }

  private SprechtagKopf zuKopf(SprechtagFormular formular) {
    List<KlasseOption> klassen =
        klassenauswahl.waehlbareKlassen(formular.getKlasseIds()).stream()
            .filter(option -> formular.getKlasseIds().contains(option.id()))
            .toList();
    return new SprechtagKopf(
        formular.getTitel(),
        formular.getDatum(),
        formular.getBeginn(),
        formular.getEnde(),
        formular.getOrt(),
        formular.getBeschreibung(),
        formular.getSlotInMinuten(),
        klassen);
  }

  List<LehrkraftOption> ladeLehrkraftOptionen(UUID sprechtagId, UUID klasseId) {
    return buchungsoptionen.ladeLehrkraftOptionen(sprechtagId, klasseId);
  }

  /**
   * Schreibt den Nachtrag atomar fest und gibt die Anzahl gebuchter Termine zurück. Wirft
   * {@link de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException}, {@link
   * de.openclassware.elternsprechtag.sprechtag.domain.ZeitkonfliktException} und {@link
   * de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException}.
   */
  int trageNach(NachtragsAnfrage anfrage) {
    return nachtragenUseCase.trageNach(anfrage);
  }

  /**
   * Ob die Ansicht den Stellvertreteradresse-Schalter überhaupt anbietet. Ohne konfigurierte
   * Adresse gäbe es nichts einzusetzen — der Schalter bliebe eine leere Behauptung.
   */
  boolean stellvertreteradresseVerfuegbar() {
    String adresse = properties.getStellvertreteradresse();
    return adresse != null && !adresse.isBlank();
  }

  String stellvertreteradresse() {
    return properties.getStellvertreteradresse();
  }
}
