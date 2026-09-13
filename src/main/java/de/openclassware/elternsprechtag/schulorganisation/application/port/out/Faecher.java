package de.openclassware.elternsprechtag.schulorganisation.application.port.out;

import de.openclassware.elternsprechtag.schulorganisation.domain.Fach;
import de.openclassware.elternsprechtag.schulorganisation.domain.FachId;
import java.util.Optional;

/** Aggregat-Repository der Fächer. Siehe {@link Lehrkraefte} für die gemeinsamen Zusagen. */
public interface Faecher {

  Optional<Fach> lade(FachId id);

  void speichere(Fach fach);
}
