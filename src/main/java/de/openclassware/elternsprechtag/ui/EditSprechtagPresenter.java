package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anlegen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Bearbeiten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl.KlasseOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Veroeffentlichen;
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

  List<KlasseOption> findAllKlassen() {
    return klassenauswahl.alleKlassen();
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

  /** Legt einen neuen Sprechtag an und veröffentlicht ihn in einem Zug — der Anlege-Knopf. */
  Veroeffentlichen.Ergebnis legeAnUndVeroeffentliche(SprechtagFormular formular) {
    return anlegen.legeUndVeroeffentliche(formular);
  }
}
