package de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
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

  /**
   * Wie viele Buchungen es an diesem Sprechtag je gab — stornierte eingeschlossen. Die Frage lautet
   * nicht „ist etwas belegt", sondern „hat schon jemand etwas in der Hand"; eine stornierte Buchung
   * ist dafür genauso ein Ja.
   */
  @Query(
      """
      select count(*)
        from buchungen b
        join termin t on t.id = b.termin_id
       where t.sprechtag_id = :sprechtagId
      """)
  long zaehleBuchungenFuerSprechtag(@Param("sprechtagId") UUID sprechtagId);

  /**
   * Verwirft die Termine eines Sprechtags. Bewusst als Statement und nicht als
   * {@code deleteAll(...)} über geladene Aggregate: Es sind rund 600 Zeilen, und es gibt nichts
   * abzuwägen — sie verlieren mit der zurückgenommenen Veröffentlichung ihre Grundlage.
   *
   * <p>Buchungen daran darf es an dieser Stelle nicht geben; der Use Case prüft das vorher über
   * {@link #zaehleBuchungenFuerSprechtag(UUID)}. Gäbe es doch welche, scheiterte das Löschen am
   * Fremdschlüssel — laut und nicht still.
   */
  @Modifying
  @Query("delete from termin where sprechtag_id = :sprechtagId")
  void loescheFuerSprechtag(@Param("sprechtagId") UUID sprechtagId);
}
