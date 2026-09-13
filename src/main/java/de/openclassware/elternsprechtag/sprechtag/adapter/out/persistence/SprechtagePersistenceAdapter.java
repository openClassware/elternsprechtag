package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.domain.AccessToken;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Erfüllt den {@code Sprechtage}-Port über Spring Data JDBC. */
@RequiredArgsConstructor
@Component
class SprechtagePersistenceAdapter implements Sprechtage {

  private final SprechtagZeilen zeilen;

  @Override
  public Optional<Sprechtag> lade(SprechtagId id) {
    return zeilen.findById(id.wert()).map(SprechtagMapper::zuAggregat);
  }

  @Override
  public Optional<Sprechtag> ladeNachAccessToken(AccessToken token) {
    return zeilen.findByAccessToken(token.wert()).map(SprechtagMapper::zuAggregat);
  }

  @Override
  public void speichere(Sprechtag sprechtag) {
    // Wirft OptimisticLockingFailureException, wenn die Version am Root nicht mehr passt — zwei
    // Organizer-Fenster, die denselben Sprechtag gleichzeitig geändert haben.
    zeilen.save(SprechtagMapper.zuZeile(sprechtag));
  }
}
