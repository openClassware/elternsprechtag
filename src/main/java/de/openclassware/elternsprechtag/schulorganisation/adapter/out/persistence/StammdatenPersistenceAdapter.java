package de.openclassware.elternsprechtag.schulorganisation.adapter.out.persistence;

import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Faecher;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Klassen;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Lehrkraefte;
import de.openclassware.elternsprechtag.schulorganisation.domain.Fach;
import de.openclassware.elternsprechtag.schulorganisation.domain.FachId;
import de.openclassware.elternsprechtag.schulorganisation.domain.Klasse;
import de.openclassware.elternsprechtag.schulorganisation.domain.KlasseId;
import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.schulorganisation.domain.LehrauftragId;
import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrkraft;
import de.openclassware.elternsprechtag.schulorganisation.domain.LehrkraftId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Erfüllt die vier Aggregat-Repositories dieses Kontexts über Spring Data JDBC.
 *
 * <p><b>Ein Adapter für vier Ports</b>, entgegen der sonstigen Eins-zu-eins-Zuordnung: Die vier
 * Stammdaten-Aggregate liegen je in genau einer Tabelle ohne Kindzeilen, und ihre Übersetzung ist
 * viermal dieselbe Bewegung — laden, mappen, speichern. Vier Klassen dafür wären vier Dateien mit
 * identischer Struktur und ohne eigene Entscheidung. Die Ports bleiben getrennt, und genau darauf
 * kommt es an: Jeder Use Case bekommt weiterhin nur das Repository, das er braucht, und wenn ein
 * Aggregat später eine eigene Persistenzfrage aufwirft, zieht es hier allein aus.
 */
@RequiredArgsConstructor
@Component
class StammdatenPersistenceAdapter implements Lehrkraefte, Klassen, Faecher, Lehrauftraege {

  private final LehrkraftZeilen lehrkraefte;
  private final KlasseZeilen klassen;
  private final FachZeilen faecher;
  private final LehrauftragZeilen lehrauftraege;

  @Override
  public Optional<Lehrkraft> lade(LehrkraftId id) {
    return lehrkraefte.findById(id.wert()).map(StammdatenMapper::zuAggregat);
  }

  @Override
  public void speichere(Lehrkraft lehrkraft) {
    lehrkraefte.save(StammdatenMapper.zuZeile(lehrkraft));
  }

  @Override
  public Optional<Klasse> lade(KlasseId id) {
    return klassen.findById(id.wert()).map(StammdatenMapper::zuAggregat);
  }

  @Override
  public void speichere(Klasse klasse) {
    klassen.save(StammdatenMapper.zuZeile(klasse));
  }

  @Override
  public Optional<Fach> lade(FachId id) {
    return faecher.findById(id.wert()).map(StammdatenMapper::zuAggregat);
  }

  @Override
  public void speichere(Fach fach) {
    faecher.save(StammdatenMapper.zuZeile(fach));
  }

  @Override
  public Optional<Lehrauftrag> lade(LehrauftragId id) {
    return lehrauftraege.findById(id.wert()).map(StammdatenMapper::zuAggregat);
  }

  @Override
  public void speichere(Lehrauftrag lehrauftrag) {
    lehrauftraege.save(StammdatenMapper.zuZeile(lehrauftrag));
  }
}
