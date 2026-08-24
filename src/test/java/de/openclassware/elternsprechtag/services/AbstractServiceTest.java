package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Fach;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.repositories.BuchungRepository;
import de.openclassware.elternsprechtag.repositories.FachRepository;
import de.openclassware.elternsprechtag.repositories.KlassenRepository;
import de.openclassware.elternsprechtag.repositories.LehrauftragRepository;
import de.openclassware.elternsprechtag.repositories.LehrerRepository;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;
import de.openclassware.elternsprechtag.repositories.TerminRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Gemeinsame Fixtures und DB-Reinigung für die Service-Tests. Trägt bewusst keine Spring-Test-
 * Annotationen — die konkreten Testklassen bringen {@link ServiceTest} mit. Ohne umschließende
 * Test-Transaktion (dort konfiguriert) committen die Fixture-Saves sofort, sodass die eigenen
 * Transaktionsgrenzen der Services real beobachtbar sind.
 *
 * <p>Die Reinigung in {@link #cleanDb()} ist deshalb nicht optional: Die Tests laufen gegen die
 * konfigurierte Postgres, die zwischen zwei Läufen bestehen bleibt — anders als bei einer
 * untergeschobenen In-Memory-Datenbank räumt kein Kontextabbau hinter ihnen auf.
 */
abstract class AbstractServiceTest {

  @Autowired protected SprechtagRepository sprechtagRepository;
  @Autowired protected TerminRepository terminRepository;
  @Autowired protected BuchungRepository buchungRepository;
  @Autowired protected LehrauftragRepository lehrauftragRepository;
  @Autowired protected KlassenRepository klassenRepository;
  @Autowired protected FachRepository fachRepository;
  @Autowired protected LehrerRepository lehrerRepository;

  @Autowired protected SprechtagService sprechtagService;
  @Autowired protected BuchungService buchungService;

  @BeforeEach
  void cleanDb() {
    // FK-sichere Reihenfolge.
    buchungRepository.deleteAll();
    terminRepository.deleteAll();
    sprechtagRepository.deleteAll();
    lehrauftragRepository.deleteAll();
    klassenRepository.deleteAll();
    fachRepository.deleteAll();
    lehrerRepository.deleteAll();
  }

  protected Fach persistFach(String name, String shortName) {
    Fach fach = new Fach();
    fach.setId(UUID.randomUUID());
    fach.setName(name);
    fach.setShortName(shortName);
    return fachRepository.save(fach);
  }

  protected Lehrer persistLehrer(String vorname, String nachname, String kuerzel) {
    Lehrer lehrer = new Lehrer();
    lehrer.setVorname(vorname);
    lehrer.setNachname(nachname);
    lehrer.setKuerzel(kuerzel);
    return lehrerRepository.save(lehrer);
  }

  protected Klasse persistKlasse(String name) {
    Klasse klasse = new Klasse();
    klasse.setId(UUID.randomUUID());
    klasse.setName(name);
    return klassenRepository.save(klasse);
  }

  protected Lehrauftrag persistLehrauftrag(Lehrer lehrer, Klasse klasse, Fach fach) {
    Lehrauftrag lehrauftrag = new Lehrauftrag();
    lehrauftrag.setLehrer(lehrer);
    lehrauftrag.setKlasse(klasse);
    lehrauftrag.setFach(fach);
    return lehrauftragRepository.save(lehrauftrag);
  }

  protected Sprechtag persistSprechtag(
      String titel,
      LocalDate date,
      LocalTime start,
      LocalTime end,
      int slotMinutes,
      SprechtagStatusEnum status,
      Klasse... klassen) {
    Sprechtag sprechtag = new Sprechtag();
    sprechtag.setTitel(titel);
    sprechtag.setStartDate(date);
    sprechtag.setStartTime(start);
    sprechtag.setEndTime(end);
    sprechtag.setSlotInMinutes(slotMinutes);
    sprechtag.setAccessToken(UUID.randomUUID().toString());
    sprechtag.setStatus(status);
    sprechtag.setKlassen(new ArrayList<>(List.of(klassen)));
    return sprechtagRepository.save(sprechtag);
  }
}
