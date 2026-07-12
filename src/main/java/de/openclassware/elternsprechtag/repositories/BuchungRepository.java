package de.openclassware.elternsprechtag.repositories;

import de.openclassware.elternsprechtag.domain.Buchung;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface BuchungRepository extends ListCrudRepository<Buchung, UUID> {}
