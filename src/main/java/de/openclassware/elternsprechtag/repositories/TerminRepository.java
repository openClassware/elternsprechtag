package de.openclassware.elternsprechtag.repositories;

import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.Termin;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminRepository extends JpaRepository<Termin, UUID> {

  @EntityGraph(attributePaths = "lehrer")
  List<Termin> findBySprechtagOrderByLehrer_NachnameAscStartzeitAsc(Sprechtag sprechtag);

  boolean existsBySprechtag(Sprechtag sprechtag);
}
