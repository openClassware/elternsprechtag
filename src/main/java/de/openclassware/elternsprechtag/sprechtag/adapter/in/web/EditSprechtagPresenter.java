package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.SprechtagMeldungen.Meldung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anlegen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Bearbeiten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl.KlasseOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import de.openclassware.elternsprechtag.sprechtag.domain.Anmeldefrist;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungsVorlauf;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class EditSprechtagPresenter {

  private final Klassenauswahl klassenauswahl;
  private final Anlegen anlegen;
  private final Bearbeiten bearbeiten;

  /**
   * Was die Klassen-Auswahl anbieten darf: die aktiven Klassen plus die, die dieser Sprechtag schon
   * gewählt hat. Beim Anlegen ist {@code bereitsGewaehlt} leer.
   */
  List<KlasseOption> waehlbareKlassen(Collection<UUID> bereitsGewaehlt) {
    return klassenauswahl.waehlbareKlassen(bereitsGewaehlt);
  }

  Optional<SprechtagFormular> loadForm(UUID id) {
    return bearbeiten.ladeFormular(id);
  }

  /**
   * Legt an ({@code id == null}) oder schreibt zurück. Der Status bleibt unberührt — Speichern ist
   * kein Statuswechsel; dass es früher einer war, war der Weg, auf dem sich ein abgesagter
   * Sprechtag wiederbeleben ließ.
   */
  UUID speichere(UUID id, SprechtagFormular formular) {
    if (id == null) {
      return anlegen.lege(formular);
    }
    bearbeiten.bearbeite(id, formular);
    return id;
  }

  /**
   * Legt einen neuen Sprechtag an und veröffentlicht ihn in einem Zug — der Anlege-Knopf.
   *
   * @return die Hinweise, die der View danach zeigt; leer, wenn es nichts zu sagen gibt
   */
  List<Meldung> legeAnUndVeroeffentliche(SprechtagFormular formular) {
    return SprechtagMeldungen.hinweiseZu(anlegen.legeUndVeroeffentliche(formular));
  }

  /** Ein Hilfetext als i18n-Schlüssel samt Parametern — übersetzt wird am View. */
  record Hilfetext(String schluessel, Object... parameter) {}

  /**
   * Der Hilfetext unter der Anmeldefrist: der errechnete Anmeldeschluss, sobald ein Datum gewählt
   * ist; ohne Datum eine Erklärung der Frist; bei einer Frist außerhalb ihres Bereichs nichts — dort
   * spricht die Validierung. Gerechnet wird mit derselben {@link Anmeldefrist}, die das Aggregat
   * hält: Hilfetext und Elternlink sollen nie verschiedene Tage nennen.
   */
  Optional<Hilfetext> anmeldefristHilfetext(LocalDate datum, Integer anmeldefristTage) {
    if (datum == null) {
      return Optional.of(new Hilfetext("edit-sprechtag.field.anmeldefrist.helper-ohne-datum"));
    }
    if (anmeldefristTage == null || !Anmeldefrist.istZulaessig(anmeldefristTage)) {
      return Optional.empty();
    }
    LocalDate schluss = Anmeldefrist.vonTagen(anmeldefristTage).anmeldeschlussFuer(datum);
    return Optional.of(
        new Hilfetext(
            "edit-sprechtag.field.anmeldefrist.helper", Formats.weekdayDateShort(schluss)));
  }

  /**
   * Der Hilfetext unter der Erinnerung — nach demselben Muster wie bei der Anmeldefrist: der Tag
   * des Versands, sobald ein Datum gewählt ist; bei 0 der Hinweis, dass keine Erinnerung läuft;
   * außerhalb der festen Optionen nichts, dort spricht die Validierung.
   */
  Optional<Hilfetext> erinnerungHilfetext(LocalDate datum, Integer erinnerungTage) {
    if (erinnerungTage == null || !ErinnerungsVorlauf.istZulaessig(erinnerungTage)) {
      return Optional.empty();
    }
    if (ErinnerungsVorlauf.vonTagen(erinnerungTage) == ErinnerungsVorlauf.KEINE) {
      return Optional.of(new Hilfetext("edit-sprechtag.field.erinnerung.helper-keine"));
    }
    if (datum == null) {
      return Optional.of(new Hilfetext("edit-sprechtag.field.erinnerung.helper-ohne-datum"));
    }
    return Optional.of(
        new Hilfetext(
            "edit-sprechtag.field.erinnerung.helper",
            Formats.weekdayDateShort(datum.minusDays(erinnerungTage))));
  }
}
