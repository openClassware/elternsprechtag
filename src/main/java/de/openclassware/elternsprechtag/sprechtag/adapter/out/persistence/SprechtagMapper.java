package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.domain.AccessToken;
import de.openclassware.elternsprechtag.sprechtag.domain.Anmeldefrist;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungsVorlauf;
import de.openclassware.elternsprechtag.sprechtag.domain.KlasseId;
import de.openclassware.elternsprechtag.sprechtag.domain.Schulkontakt;
import de.openclassware.elternsprechtag.sprechtag.domain.Slotdauer;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Zeitfenster;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Übersetzt in beide Richtungen zwischen Sprechtag-Aggregat und Persistenzmodell. Die einzige
 * Stelle, an der beide Formen einander sehen — daran hängt, dass die Domäne nichts von Spring Data
 * weiß.
 */
final class SprechtagMapper {

  private SprechtagMapper() {}

  static Sprechtag zuAggregat(SprechtagZeile zeile) {
    List<KlasseId> klassen =
        zeile.getKlassen().stream().map(k -> KlasseId.von(k.klasseId())).toList();
    return Sprechtag.rekonstruiere(
        SprechtagId.von(zeile.getId()),
        zeile.getVersion(),
        zeile.getTitel(),
        zeile.getLocation(),
        zeile.getDescription(),
        Schulkontakt.von(zeile.getSchulkontakt()),
        AccessToken.von(zeile.getAccessToken()),
        zeile.getStartDate(),
        new Zeitfenster(zeile.getStartTime(), zeile.getEndTime()),
        Slotdauer.vonMinuten(zeile.getSlotInMinutes()),
        klassen,
        zeile.getStatus(),
        zeile.getErinnerungVorlauf(),
        Anmeldefrist.vonTagen(zeile.getAnmeldefristTage()));
  }

  static SprechtagZeile zuZeile(Sprechtag sprechtag) {
    Set<SprechtagKlasseZeile> klassen = new LinkedHashSet<>();
    for (KlasseId klasse : sprechtag.klassen()) {
      klassen.add(new SprechtagKlasseZeile(klasse.wert()));
    }
    return new SprechtagZeile(
        sprechtag.id().wert(),
        sprechtag.titel(),
        sprechtag.ort(),
        sprechtag.beschreibung(),
        sprechtag.schulkontakt().text(),
        sprechtag.datum(),
        sprechtag.zeitfenster().beginn(),
        sprechtag.zeitfenster().ende(),
        sprechtag.slotdauer().minuten(),
        sprechtag.accessToken().wert(),
        sprechtag.status(),
        sprechtag.erinnerungsVorlauf(),
        sprechtag.anmeldefrist().tageVorher(),
        sprechtag.version(),
        klassen);
  }
}
