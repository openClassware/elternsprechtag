package de.openclassware.elternsprechtag.schulorganisation.application.port.out;

import de.openclassware.elternsprechtag.schulorganisation.domain.Klasse;
import de.openclassware.elternsprechtag.schulorganisation.domain.KlasseId;
import java.util.Optional;

/** Aggregat-Repository der Klassen. Siehe {@link Lehrkraefte} für die gemeinsamen Zusagen. */
public interface Klassen {

  Optional<Klasse> lade(KlasseId id);

  void speichere(Klasse klasse);
}
