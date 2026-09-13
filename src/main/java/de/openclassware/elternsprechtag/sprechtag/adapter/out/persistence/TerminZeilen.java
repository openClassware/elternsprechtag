package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Spring-Data-JDBC-Repository des Termin-Aggregats: ein Repository je Aggregat-Root, Speichern
 * schreibt das ganze Aggregat, Laden lädt es ganz. Nur innerhalb des Persistenz-Adapters sichtbar —
 * nach außen gilt der Port {@code Termine}.
 */
interface TerminZeilen extends CrudRepository<TerminZeile, UUID> {

  /**
   * Ob für diesen Sprechtag schon materialisiert wurde. Bewusst als Zählung statt als abgeleiteter
   * {@code existsBy}-Name: Der Adapter soll das eine Statement zeigen, das er ausführt.
   */
  @Query("select count(*) from termin where sprechtag_id = :sprechtagId")
  long zaehleFuerSprechtag(@Param("sprechtagId") UUID sprechtagId);
}
