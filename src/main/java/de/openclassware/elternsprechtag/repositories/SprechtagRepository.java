package de.openclassware.elternsprechtag.repositories;

import de.openclassware.elternsprechtag.domain.Sprechtag;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.ListCrudRepository;

public interface SprechtagRepository extends ListCrudRepository<Sprechtag, UUID> {

  @EntityGraph(attributePaths = "klassen")
  Optional<Sprechtag> findById(UUID id);
}
