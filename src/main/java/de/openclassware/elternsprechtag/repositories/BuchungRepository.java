package de.openclassware.elternsprechtag.repositories;

import de.openclassware.elternsprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.domain.BuchungStatusEnum;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface BuchungRepository extends ListCrudRepository<Buchung, UUID> {

  /**
   * Die (bereits per {@code distinct} deduplizierten) Eltern-E-Mail-Adressen mit einer Buchung im
   * gegebenen Status an diesem Sprechtag. Projiziert direkt auf die Adress-Spalte — kein
   * {@link EntityGraph} nötig, da keine Beziehung gelesen wird.
   */
  @Query(
      "select distinct b.elternEmail from Buchung b "
          + "where b.termin.sprechtag = :sprechtag and b.status = :status")
  List<String> findDistinctElternEmailByTermin_SprechtagAndStatus(
      @Param("sprechtag") Sprechtag sprechtag, @Param("status") BuchungStatusEnum status);

  /**
   * Anzahl der (per E-Mail-Adresse deduplizierten) betroffenen Eltern-Kontakte mit einer Buchung im
   * gegebenen Status an diesem Sprechtag — für die Betroffenen-Anzeige im Absage-Bestätigungsdialog.
   * Nimmt die Sprechtag-Id direkt, damit der Presenter-Pfad die Entity nicht laden muss.
   */
  @Query(
      "select count(distinct b.elternEmail) from Buchung b "
          + "where b.termin.sprechtag.id = :sprechtagId and b.status = :status")
  long countDistinctElternEmailByTermin_SprechtagIdAndStatus(
      @Param("sprechtagId") UUID sprechtagId, @Param("status") BuchungStatusEnum status);

  /**
   * Buchungen eines Sprechtags im gegebenen Status, chronologisch nach Startzeit. Lädt Termin und
   * Lehrauftrag (samt Lehrer/Klasse/Fach) per {@link EntityGraph} mit, damit das Record-Mapping im
   * Service unter {@code open-in-view=false} ohne Lazy-Zugriff auskommt.
   */
  @EntityGraph(
      attributePaths = {
        "termin",
        "lehrauftrag",
        "lehrauftrag.lehrer",
        "lehrauftrag.klasse",
        "lehrauftrag.fach"
      })
  List<Buchung> findByTermin_SprechtagAndStatusOrderByTermin_StartzeitAsc(
      Sprechtag sprechtag, BuchungStatusEnum status);
}
