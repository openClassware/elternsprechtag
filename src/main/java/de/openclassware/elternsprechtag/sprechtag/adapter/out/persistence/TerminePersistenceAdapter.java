package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Erfüllt den {@code Termine}-Port über Spring Data JDBC. */
@RequiredArgsConstructor
@Component
class TerminePersistenceAdapter implements Termine {

  private final TerminZeilen zeilen;

  @Override
  public Optional<Termin> lade(TerminId id) {
    return zeilen.findById(id.wert()).map(TerminMapper::zuAggregat);
  }

  @Override
  public void speichere(Termin termin) {
    // Wirft OptimisticLockingFailureException, wenn die Version am Root nicht mehr passt — der
    // Konfliktfall, den der Buchungs-Use-Case in TerminBelegtException übersetzt.
    zeilen.save(TerminMapper.zuZeile(termin));
  }

  @Override
  public void speichereAlle(List<Termin> termine) {
    List<TerminZeile> alle = new ArrayList<>(termine.size());
    for (Termin termin : termine) {
      alle.add(TerminMapper.zuZeile(termin));
    }
    zeilen.saveAll(alle);
  }

  @Override
  public boolean existierenFuer(SprechtagId sprechtag) {
    return zeilen.zaehleFuerSprechtag(sprechtag.wert()) > 0;
  }
}
