package de.openclassware.elternsprechtag.repositories;

import de.openclassware.elternsprechtag.domain.Lehrer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface LehrerRepository extends ListCrudRepository<Lehrer, UUID> {

  Optional<Lehrer> findByKuerzel(String kuerzel);
}
