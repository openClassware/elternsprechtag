package de.openclassware.elternsprechtag.sprechtag.application.service;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import de.openclassware.elternsprechtag.sprechtag.domain.AccessToken;
import de.openclassware.elternsprechtag.sprechtag.domain.Anmeldefrist;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungsVorlauf;
import de.openclassware.elternsprechtag.sprechtag.domain.KlasseId;
import de.openclassware.elternsprechtag.sprechtag.domain.Schulkontakt;
import de.openclassware.elternsprechtag.sprechtag.domain.Slotdauer;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Zeitfenster;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Übersetzt zwischen dem rohen {@link SprechtagFormular} der Oberfläche und den Werten der Domäne.
 *
 * <p>Hier liegt die Grenze, an der aus „irgendein {@code Integer}" eine {@link Slotdauer} wird — und
 * damit die Stelle, an der eine leere Eingabe scheitert statt später als
 * {@code NullPointerException} aufzutauchen (`ABDECKUNG.md` Z. 95). Die Meldungen sind Fallback für
 * den Weg an der Formularvalidierung vorbei; im Regelfall fängt der Binder diese Fälle ab.
 */
final class Formularwerte {

  private Formularwerte() {}

  static LocalDate datum(SprechtagFormular formular) {
    return verlange(formular.getDatum(), "Ein Sprechtag ohne Datum findet nicht statt");
  }

  static Zeitfenster zeitfenster(SprechtagFormular formular) {
    return new Zeitfenster(
        verlange(formular.getBeginn(), "Ein Sprechtag ohne Beginn findet nicht statt"),
        verlange(formular.getEnde(), "Ein Sprechtag ohne Ende findet nicht statt"));
  }

  static Slotdauer slotdauer(SprechtagFormular formular) {
    return Slotdauer.vonMinuten(
        verlange(formular.getSlotInMinuten(), "Ohne Slot-Dauer lassen sich keine Termine bilden"));
  }

  static Schulkontakt schulkontakt(SprechtagFormular formular) {
    String text = formular.getSchulkontakt();
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Ein Sprechtag ohne Schulkontakt ist unvollständig");
    }
    return Schulkontakt.von(text);
  }

  static AccessToken accessToken(SprechtagFormular formular) {
    return formular.getAccessToken() == null || formular.getAccessToken().isBlank()
        ? AccessToken.neu()
        : AccessToken.von(formular.getAccessToken());
  }

  static List<KlasseId> klassen(SprechtagFormular formular) {
    return formular.getKlasseIds().stream().map(KlasseId::von).toList();
  }

  static ErinnerungsVorlauf erinnerungsVorlauf(SprechtagFormular formular) {
    ErinnerungsVorlauf vorlauf = formular.getErinnerungsVorlauf();
    return vorlauf == null ? ErinnerungsVorlauf.KEINE : vorlauf;
  }

  static Anmeldefrist anmeldefrist(SprechtagFormular formular) {
    return Anmeldefrist.vonTagen(
        verlange(
            formular.getAnmeldefristTage(), "Ein Sprechtag ohne Anmeldefrist ist unvollständig"));
  }

  /** Der Rückweg: ein gespeicherter Sprechtag als Formular. */
  static SprechtagFormular zuFormular(Sprechtag sprechtag) {
    SprechtagFormular formular = new SprechtagFormular();
    formular.setTitel(sprechtag.titel());
    formular.setOrt(sprechtag.ort());
    formular.setBeschreibung(sprechtag.beschreibung());
    formular.setSchulkontakt(sprechtag.schulkontakt().text());
    formular.setDatum(sprechtag.datum());
    formular.setBeginn(sprechtag.zeitfenster().beginn());
    formular.setEnde(sprechtag.zeitfenster().ende());
    formular.setSlotInMinuten(sprechtag.slotdauer().minuten());
    formular.setAccessToken(sprechtag.accessToken().wert());
    formular.setErinnerungsVorlauf(sprechtag.erinnerungsVorlauf());
    formular.setAnmeldefristTage(sprechtag.anmeldefrist().tageVorher());
    // Dieselbe Bedingung, die das Aggregat beim Schreiben prüft — hier nur vorgezogen, damit die
    // Oberfläche gar nicht erst zur Eingabe einlädt.
    formular.setZeitstrukturEingefroren(sprechtag.status() != SprechtagStatus.ENTWURF);
    formular.setKlasseIds(
        sprechtag.klassen().stream()
            .map(KlasseId::wert)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
    return formular;
  }

  private static <T> T verlange(T wert, String meldung) {
    if (wert == null) {
      throw new IllegalArgumentException(meldung);
    }
    return wert;
  }
}
