package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anlegen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Bearbeiten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl.KlasseOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Veroeffentlichen;
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

  /** Legt einen neuen Sprechtag an und veröffentlicht ihn in einem Zug — der Anlege-Knopf. */
  Veroeffentlichen.Ergebnis legeAnUndVeroeffentliche(SprechtagFormular formular) {
    return anlegen.legeUndVeroeffentliche(formular);
  }
}
