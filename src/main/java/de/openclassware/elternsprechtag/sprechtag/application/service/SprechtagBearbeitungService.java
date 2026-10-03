package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anlegen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Bearbeiten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Duplizieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Loeschen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Veroeffentlichen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtLoeschbarException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anlegen, Bearbeiten und Duplizieren eines Sprechtags — die Schreibseite des Bearbeiten-Formulars
 * —, dazu das Löschen, mit dem ein versehentlich angelegter Entwurf wieder verschwindet (#132).
 *
 * <p>Hier steht <b>keine</b> Regel darüber, was sich noch ändern lässt: Der Service reicht das
 * Formular an das Aggregat weiter und lässt es entscheiden. Genau daran lag es, dass Zeitfenster,
 * Slot-Dauer und Klassenliste bisher auch nach dem Veröffentlichen überschrieben wurden — der alte
 * Service setzte sie ungeprüft.
 *
 * <p>Der Status wird hier nie gewechselt. Das Speichern eines Formulars hat mit dem Lebenszyklus
 * nichts zu tun; dass es ihn früher mitgesetzt hat, war der Weg, auf dem sich ein abgesagter
 * Sprechtag wiederbeleben ließ (`ABDECKUNG.md` Z. 93).
 */
@RequiredArgsConstructor
@Service
class SprechtagBearbeitungService implements Anlegen, Bearbeiten, Duplizieren, Loeschen {

  private final Sprechtage sprechtage;
  private final Veroeffentlichen veroeffentlichen;

  @Override
  @Transactional
  public UUID lege(SprechtagFormular formular) {
    Sprechtag sprechtag =
        Sprechtag.entwirf(
            formular.getTitel(),
            formular.getOrt(),
            formular.getBeschreibung(),
            Formularwerte.schulkontakt(formular),
            Formularwerte.datum(formular),
            Formularwerte.zeitfenster(formular),
            Formularwerte.slotdauer(formular),
            Formularwerte.klassen(formular),
            Formularwerte.erinnerungsVorlauf(formular),
            Formularwerte.anmeldefrist(formular));
    sprechtage.speichere(sprechtag);
    return sprechtag.id().wert();
  }

  @Override
  @Transactional
  public Veroeffentlichen.Ergebnis legeUndVeroeffentliche(SprechtagFormular formular) {
    return veroeffentlichen.veroeffentliche(lege(formular));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SprechtagFormular> ladeFormular(UUID id) {
    return sprechtage.lade(SprechtagId.von(id)).map(Formularwerte::zuFormular);
  }

  @Override
  @Transactional
  public void bearbeite(UUID id, SprechtagFormular formular) {
    Sprechtag sprechtag = lade(id);
    sprechtag.beschreibeNeu(
        formular.getTitel(),
        formular.getOrt(),
        formular.getBeschreibung(),
        Formularwerte.schulkontakt(formular));
    sprechtag.aendereErinnerungsVorlauf(Formularwerte.erinnerungsVorlauf(formular));
    sprechtag.aendereAnmeldefrist(Formularwerte.anmeldefrist(formular));
    sprechtag.legeZeitstrukturFest(
        Formularwerte.datum(formular),
        Formularwerte.zeitfenster(formular),
        Formularwerte.slotdauer(formular),
        Formularwerte.klassen(formular));
    sprechtage.speichere(sprechtag);
  }

  @Override
  @Transactional
  public UUID dupliziere(UUID id) {
    Sprechtag kopie = lade(id).dupliziere();
    sprechtage.speichere(kopie);
    return kopie.id().wert();
  }

  @Override
  @Transactional
  public void loesche(UUID id) {
    Optional<Sprechtag> geladen = sprechtage.lade(SprechtagId.von(id));
    if (geladen.isEmpty()) {
      // Ein zweiter Tab oder ein Doppelklick war schneller — der Entwurf ist weg, wie gewollt.
      return;
    }
    Sprechtag sprechtag = geladen.get();
    sprechtag.verlangeLoeschbar();
    try {
      sprechtage.entferne(sprechtag);
    } catch (OptimisticLockingFailureException konflikt) {
      // Zwischen Laden und Entfernen hat ein anderes Fenster den Sprechtag verändert — vielleicht
      // veröffentlicht. Fachlich derselbe Fall wie der veraltete Menüeintrag, nur einen Takt später
      // bemerkt; gelöscht wird nichts, was der Organizer so nicht vor Augen hatte.
      throw new SprechtagNichtLoeschbarException(
          "Sprechtag wurde soeben von anderer Stelle verändert: " + id);
    }
  }

  private Sprechtag lade(UUID id) {
    return sprechtage
        .lade(SprechtagId.von(id))
        .orElseThrow(() -> new IllegalArgumentException("Sprechtag nicht gefunden: " + id));
  }
}
