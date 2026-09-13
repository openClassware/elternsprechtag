package de.openclassware.elternsprechtag.schulorganisation.application.port.out;

import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrkraft;
import de.openclassware.elternsprechtag.schulorganisation.domain.LehrkraftId;
import java.util.Optional;

/**
 * Aggregat-Repository der Lehrkräfte: lädt und speichert ganze Aggregate — der Schreibweg in die
 * Datenbank. Die Leseseite geht den anderen Weg, über {@link Stammdatenansichten} (ADR 0003).
 */
public interface Lehrkraefte {

  Optional<Lehrkraft> lade(LehrkraftId id);

  /**
   * Schreibt das Aggregat. Wie überall in diesem Projekt ist es danach <b>verbraucht</b>: Der
   * Adapter hebt die Version in der Datenbank, das Aggregat trägt sie unveränderlich. Wer
   * weiterarbeiten will, lädt neu.
   *
   * @throws org.springframework.dao.OptimisticLockingFailureException wenn die Lehrkraft seit dem
   *     Laden verändert wurde
   */
  void speichere(Lehrkraft lehrkraft);
}
