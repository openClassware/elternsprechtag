package de.openclassware.elternsprechtag.repositories;

import de.openclassware.elternsprechtag.domain.Fach;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface FachRepository extends ListCrudRepository<Fach, UUID> {

  Optional<Fach> findByShortName(String shortName);
}
