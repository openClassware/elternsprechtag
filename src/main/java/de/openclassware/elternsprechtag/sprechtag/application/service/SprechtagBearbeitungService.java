package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anlegen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Bearbeiten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Duplizieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Veroeffentlichen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.domain.AccessToken;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anlegen, Bearbeiten und Duplizieren eines Sprechtags — die Schreibseite des Bearbeiten-Formulars.
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
class SprechtagBearbeitungService implements Anlegen, Bearbeiten, Duplizieren {

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
            Formularwerte.accessToken(formular),
            Formularwerte.datum(formular),
            Formularwerte.zeitfenster(formular),
            Formularwerte.slotdauer(formular),
            Formularwerte.klassen(formular),
            Formularwerte.erinnerungsVorlauf(formular));
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
        Formularwerte.schulkontakt(formular),
        Formularwerte.accessToken(formular));
    sprechtag.aendereErinnerungsVorlauf(Formularwerte.erinnerungsVorlauf(formular));
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
    Sprechtag kopie = lade(id).dupliziere(AccessToken.neu());
    sprechtage.speichere(kopie);
    return kopie.id().wert();
  }

  private Sprechtag lade(UUID id) {
    return sprechtage
        .lade(SprechtagId.von(id))
        .orElseThrow(() -> new IllegalArgumentException("Sprechtag nicht gefunden: " + id));
  }
}
