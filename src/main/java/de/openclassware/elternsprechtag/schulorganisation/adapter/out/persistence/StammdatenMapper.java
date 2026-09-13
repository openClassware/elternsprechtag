package de.openclassware.elternsprechtag.schulorganisation.adapter.out.persistence;

import de.openclassware.elternsprechtag.schulorganisation.domain.Fach;
import de.openclassware.elternsprechtag.schulorganisation.domain.FachId;
import de.openclassware.elternsprechtag.schulorganisation.domain.Klasse;
import de.openclassware.elternsprechtag.schulorganisation.domain.KlasseId;
import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.schulorganisation.domain.LehrauftragId;
import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrkraft;
import de.openclassware.elternsprechtag.schulorganisation.domain.LehrkraftId;

/**
 * Übersetzt in beide Richtungen zwischen den Stammdaten-Aggregaten und ihren Persistenzmodellen.
 * Die einzige Stelle, an der beide Formen einander sehen — daran hängt, dass die Domäne nichts von
 * Spring Data weiß.
 *
 * <p>Alle vier Aggregate in einem Mapper, weil jede Übersetzung drei Zeilen lang ist und vier
 * Dateien nur denselben Import-Block viermal trügen.
 */
final class StammdatenMapper {

  private StammdatenMapper() {}

  static Lehrkraft zuAggregat(LehrkraftZeile zeile) {
    return Lehrkraft.rekonstruiere(
        LehrkraftId.von(zeile.getId()),
        zeile.getVersion(),
        zeile.getVorname(),
        zeile.getNachname(),
        zeile.getKuerzel(),
        zeile.isStillgelegt());
  }

  static LehrkraftZeile zuZeile(Lehrkraft lehrkraft) {
    return new LehrkraftZeile(
        lehrkraft.id().wert(),
        lehrkraft.vorname(),
        lehrkraft.nachname(),
        lehrkraft.kuerzel(),
        lehrkraft.istStillgelegt(),
        lehrkraft.version());
  }

  static Klasse zuAggregat(KlasseZeile zeile) {
    return Klasse.rekonstruiere(
        KlasseId.von(zeile.getId()),
        zeile.getVersion(),
        zeile.getName(),
        zeile.isStillgelegt());
  }

  static KlasseZeile zuZeile(Klasse klasse) {
    return new KlasseZeile(
        klasse.id().wert(), klasse.name(), klasse.istStillgelegt(), klasse.version());
  }

  static Fach zuAggregat(FachZeile zeile) {
    return Fach.rekonstruiere(
        FachId.von(zeile.getId()),
        zeile.getVersion(),
        zeile.getName(),
        zeile.getShortName(),
        zeile.isStillgelegt());
  }

  static FachZeile zuZeile(Fach fach) {
    return new FachZeile(
        fach.id().wert(), fach.name(), fach.kuerzel(), fach.istStillgelegt(), fach.version());
  }

  static Lehrauftrag zuAggregat(LehrauftragZeile zeile) {
    return Lehrauftrag.rekonstruiere(
        LehrauftragId.von(zeile.getId()),
        zeile.getVersion(),
        LehrkraftId.von(zeile.getLehrerId()),
        KlasseId.von(zeile.getKlasseId()),
        FachId.von(zeile.getFachId()),
        zeile.isStillgelegt());
  }

  static LehrauftragZeile zuZeile(Lehrauftrag lehrauftrag) {
    return new LehrauftragZeile(
        lehrauftrag.id().wert(),
        lehrauftrag.lehrkraft().wert(),
        lehrauftrag.klasse().wert(),
        lehrauftrag.fach().wert(),
        lehrauftrag.istStillgelegt(),
        lehrauftrag.version());
  }
}
