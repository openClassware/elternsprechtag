package de.openclassware.elternsprechtag.schulorganisation.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring-Data-JDBC-Repository des Lehrkraft-Aggregats: ein Repository je Aggregat-Wurzel. Nur
 * innerhalb des Persistenz-Adapters sichtbar — nach außen gilt der Port.
 */
interface LehrkraftZeilen extends CrudRepository<LehrkraftZeile, UUID> {}
