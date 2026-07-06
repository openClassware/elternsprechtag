package de.openclassware.elternsprechtag.repositories;

import de.openclassware.elternsprechtag.domain.Klasse;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.UUID;

public interface KlassenRepository extends ListCrudRepository<Klasse, UUID> {

    List<Klasse> findAllByOrderByNameAsc();

}
