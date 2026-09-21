package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsziel;
import de.openclassware.elternsprechtag.sprechtag.domain.Familie;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrauftragId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.Notiz;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import de.openclassware.elternsprechtag.sprechtag.domain.Zeitraum;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Übersetzt in beide Richtungen zwischen Aggregat und Persistenzmodell. Die einzige Stelle, an der
 * beide Formen einander sehen — daran hängt, dass die Domäne nichts von Spring Data weiß.
 */
final class TerminMapper {

  private TerminMapper() {}

  static Termin zuAggregat(TerminZeile zeile) {
    // Chronologisch, damit „die erste Buchung" im Aggregat dasselbe heißt wie auf der Leseseite;
    // die Set-Spalte gibt keine Reihenfolge her.
    List<Buchung> buchungen =
        zeile.getBuchungen().stream()
            .sorted(Comparator.comparing(BuchungZeile::getErstelltAm))
            .map(TerminMapper::zuBuchung)
            .toList();
    return Termin.rekonstruiere(
        TerminId.von(zeile.getId()),
        SprechtagId.von(zeile.getSprechtagId()),
        LehrkraftId.von(zeile.getLehrerId()),
        new Zeitraum(zeile.getStartzeit(), zeile.getEndzeit()),
        zeile.getVerfuegbarkeit(),
        zeile.getVersion(),
        buchungen);
  }

  private static Buchung zuBuchung(BuchungZeile zeile) {
    return Buchung.rekonstruiere(
        BuchungId.von(zeile.getId()),
        zeile.getErstelltAm(),
        new Familie(zeile.getElternName(), zeile.getSchuelerName(), zeile.getElternEmail()),
        new Buchungsziel(
            LehrauftragId.von(zeile.getLehrauftragId()),
            LehrkraftId.von(zeile.getLehrkraftId()),
            zeile.getLehrkraftName(),
            zeile.getLehrkraftKuerzel(),
            zeile.getKlasseName(),
            zeile.getFachName()),
        Notiz.vielleicht(zeile.getNotiz()).orElse(null),
        zeile.getStatus(),
        zeile.getErinnerungVersendetAm());
  }

  static TerminZeile zuZeile(Termin termin) {
    Set<BuchungZeile> buchungen = new LinkedHashSet<>();
    for (Buchung buchung : termin.buchungen()) {
      buchungen.add(zuZeile(buchung));
    }
    return new TerminZeile(
        termin.id().wert(),
        termin.zeitraum().beginn(),
        termin.zeitraum().ende(),
        termin.verfuegbarkeit(),
        termin.version(),
        termin.lehrkraft().wert(),
        termin.sprechtag().wert(),
        buchungen);
  }

  private static BuchungZeile zuZeile(Buchung buchung) {
    Buchungsziel ziel = buchung.ziel();
    return new BuchungZeile(
        buchung.id().wert(),
        buchung.erstelltAm(),
        buchung.status(),
        buchung.familie().schuelerName(),
        buchung.familie().elternName(),
        buchung.familie().email(),
        buchung.notiz().map(Notiz::text).orElse(null),
        ziel.herkunft().wert(),
        ziel.lehrkraft().wert(),
        ziel.lehrkraftName(),
        ziel.lehrkraftKuerzel(),
        ziel.klasse(),
        ziel.fach(),
        buchung.erinnerungVersendetAm().orElse(null));
  }
}
