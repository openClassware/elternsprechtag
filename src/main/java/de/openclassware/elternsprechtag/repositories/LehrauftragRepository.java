package de.openclassware.elternsprechtag.repositories;

import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.ListCrudRepository;

public interface LehrauftragRepository extends ListCrudRepository<Lehrauftrag, UUID> {

  @EntityGraph(attributePaths = {"fach", "lehrer"})
  List<Lehrauftrag> findByKlasseOrderByFach_NameAsc(Klasse klasse);
}
