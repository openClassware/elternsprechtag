package de.openclassware.elternsprechtag.repositories;

import de.openclassware.elternsprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.domain.BuchungStatusEnum;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.ListCrudRepository;

public interface BuchungRepository extends ListCrudRepository<Buchung, UUID> {

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
