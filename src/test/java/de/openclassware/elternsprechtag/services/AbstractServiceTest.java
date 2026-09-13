package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.Fach;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.repositories.FachRepository;
import de.openclassware.elternsprechtag.repositories.KlassenRepository;
import de.openclassware.elternsprechtag.repositories.LehrauftragRepository;
import de.openclassware.elternsprechtag.repositories.LehrerRepository;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Gemeinsame Fixtures und DB-Reinigung für die Service-Tests. Trägt bewusst keine Spring-Test-
 * Annotationen — die konkreten Testklassen bringen {@link ServiceTest} mit. Ohne umschließende
 * Test-Transaktion (dort konfiguriert) committen die Fixture-Saves sofort, sodass die eigenen
 * Transaktionsgrenzen der Services real beobachtbar sind.
 *
 * <p>Die Reinigung in {@link #cleanDb()} ist deshalb nicht optional: Die Tests laufen gegen eine
 * echte Postgres, die zwischen zwei Läufen bestehen bleibt — anders als bei einer untergeschobenen
 * In-Memory-Datenbank räumt kein Kontextabbau hinter ihnen auf.
 *
 * <p>Weil hier tatsächlich Tabellen geleert werden, zeigt {@link ServiceTest} auf eine eigene
 * Test-Datenbank und nicht auf die der Anwendung. Wer das ändert, leert damit die
 * Entwicklungsdatenbank bei jedem Testlauf.
 */
abstract class AbstractServiceTest {

  /**
   * Schulkontakt der Fixtures. Nicht kosmetisch: Ohne ihn lässt sich kein Sprechtag veröffentlichen
   * — weder über den Service noch am Check-Constraint der Datenbank vorbei.
   */
  protected static final String SCHULKONTAKT = "Sekretariat, Tel. 0123 456789";

  @Autowired protected SprechtagRepository sprechtagRepository;
  @Autowired protected LehrauftragRepository lehrauftragRepository;
  @Autowired protected KlassenRepository klassenRepository;
  @Autowired protected FachRepository fachRepository;
  @Autowired protected LehrerRepository lehrerRepository;

  @Autowired protected SprechtagService sprechtagService;

  /** Der Schreibweg auf die Termin-Aggregate — dieselben Ports, die die Use Cases benutzen. */
  @Autowired protected Termine termine;

  @Autowired protected Buchen buchen;
  @Autowired protected Auswerten auswerten;
  @Autowired protected Buchungsoptionen buchungsoptionen;

  /** Nur zum Aufräumen und um Aggregate der Reihe nach einzusammeln — nie für Zusicherungen. */
  @Autowired protected JdbcTemplate jdbc;

  // Auch NACH jedem Test, nicht nur davor: Die Datenbank überlebt den Testlauf. Räumten die
  // Service-Tests nur vor sich selbst auf, bliebe die Fixture des jeweils letzten Tests einer
  // Klasse liegen — für jede andere Testklasse der Suite und für jeden weiteren `mvn verify`.
  // DemoSeedSmokeTest ist daran schon einmal gescheitert (eine liegengebliebene Klasse "5a"
  // kollidierte mit dem Seed).
  @BeforeEach
  @AfterEach
  void cleanDb() {
    // FK-sichere Reihenfolge. Termin und Buchung gehören keinem JPA-Repository mehr; sie werden
    // als das geleert, was sie sind — zwei Tabellen eines Aggregats.
    jdbc.update("delete from buchungen");
    jdbc.update("delete from termin");
    sprechtagRepository.deleteAll();
    lehrauftragRepository.deleteAll();
    klassenRepository.deleteAll();
    fachRepository.deleteAll();
    lehrerRepository.deleteAll();
  }

  /** Alle Termin-Aggregate der Datenbank, chronologisch. */
  protected List<Termin> alleTermine() {
    return jdbc.queryForList("select id from termin order by startzeit", UUID.class).stream()
        .map(id -> termine.lade(TerminId.von(id)).orElseThrow())
        .toList();
  }

  /** Die Termine genau dieser Lehrkraft, chronologisch. */
  protected List<Termin> termineVon(Lehrer lehrer) {
    LehrkraftId lehrkraft = LehrkraftId.von(lehrer.getId());
    return alleTermine().stream().filter(termin -> termin.lehrkraft().equals(lehrkraft)).toList();
  }

  /**
   * Storniert die erste aktive Buchung, auf die {@code treffer} zutrifft — über das Aggregat, nicht
   * per UPDATE, damit die Tests denselben Weg gehen wie die Anwendung.
   */
  protected void storniere(Predicate<Buchung> treffer) {
    for (Termin termin : alleTermine()) {
      for (Buchung buchung : termin.buchungen()) {
        if (buchung.istAktiv() && treffer.test(buchung)) {
          termin.storniere(buchung.id());
          termine.speichere(termin);
          return;
        }
      }
    }
    throw new IllegalStateException("Keine passende aktive Buchung gefunden");
  }

  /** Alle Buchungen aller Termine, chronologisch nach Termin. */
  protected List<Buchung> alleBuchungen() {
    return alleTermine().stream().flatMap(termin -> termin.buchungen().stream()).toList();
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
    sprechtag.setSchulkontakt(SCHULKONTAKT);
    sprechtag.setStatus(status);
    sprechtag.setKlassen(new ArrayList<>(List.of(klassen)));
    return sprechtagRepository.save(sprechtag);
  }
}
