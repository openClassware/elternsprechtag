package de.openclassware.elternsprechtag.sprechtag;

import de.openclassware.elternsprechtag.schulorganisation.application.port.in.Stammdatenpflege;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Absagen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Abschliessen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anlegen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Bearbeiten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Duplizieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Klassenauswahl;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagszugang;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Veroeffentlichen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.ZurueckAufEntwurf;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Sprechtage;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine;
import de.openclassware.elternsprechtag.sprechtag.domain.AccessToken;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.KlasseId;
import de.openclassware.elternsprechtag.sprechtag.domain.LehrkraftId;
import de.openclassware.elternsprechtag.sprechtag.domain.Schulkontakt;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Slotdauer;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminId;
import de.openclassware.elternsprechtag.sprechtag.domain.Zeitfenster;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
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
public abstract class AbstractServiceTest {

  /**
   * Schulkontakt der Fixtures. Nicht kosmetisch: Ohne ihn lässt sich kein Sprechtag anlegen — weder
   * über das Aggregat noch am Check-Constraint der Datenbank vorbei.
   */
  protected static final String SCHULKONTAKT = "Sekretariat, Tel. 0123 456789";

  /**
   * Die Stammdaten der Fixtures entstehen über den Use-Case-Port der Schulorganisation — denselben
   * Weg, den später der Import geht. Das ist kein Umweg, sondern der Grund, warum die Schreibseite
   * dieses Kontexts nicht ungeprüft bleibt, solange es den Import noch nicht gibt.
   */
  @Autowired protected Stammdatenpflege stammdatenpflege;

  /** Die Schreibwege auf die Aggregate — dieselben Ports, die die Use Cases benutzen. */
  @Autowired protected Sprechtage sprechtage;

  @Autowired protected Termine termine;

  @Autowired protected Anlegen anlegen;
  @Autowired protected Bearbeiten bearbeiten;
  @Autowired protected Duplizieren duplizieren;
  @Autowired protected Veroeffentlichen veroeffentlichen;
  @Autowired protected Absagen absagen;
  @Autowired protected Abschliessen abschliessen;
  @Autowired protected ZurueckAufEntwurf zurueckAufEntwurf;
  @Autowired protected Klassenauswahl klassenauswahl;
  @Autowired protected Sprechtagsuebersicht sprechtagsuebersicht;
  @Autowired protected Sprechtagszugang sprechtagszugang;

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
    // FK-sichere Reihenfolge. Kein Aggregat-Port hat ein `deleteAll` und keiner soll eins bekommen:
    // Löschen ist im Betrieb kein Vorgang — Stammdaten werden stillgelegt, Sprechtage abgesagt.
    // Die Tabellen werden hier als das geleert, was sie sind, und das bleibt auf den Testaufbau
    // beschränkt.
    jdbc.update("delete from buchungen");
    jdbc.update("delete from termin");
    jdbc.update("delete from sprechtage_klassen");
    jdbc.update("delete from sprechtage");
    jdbc.update("delete from lehrauftrag");
    jdbc.update("delete from klassen");
    jdbc.update("delete from faecher");
    jdbc.update("delete from lehrer");
  }

  /** Alle Termin-Aggregate der Datenbank, chronologisch. */
  protected List<Termin> alleTermine() {
    return jdbc.queryForList("select id from termin order by startzeit", UUID.class).stream()
        .map(id -> termine.lade(TerminId.von(id)).orElseThrow())
        .toList();
  }

  /** Die Termine genau dieser Lehrkraft, chronologisch. */
  protected List<Termin> termineVon(UUID lehrkraftId) {
    LehrkraftId lehrkraft = LehrkraftId.von(lehrkraftId);
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

  // Die Stammdaten-Fixtures geben Ids zurück, keine Aggregate: Die Aggregate der Schulorganisation
  // gehören ihrem Kontext, und was dieser hier braucht, ist genau der Verweis, den auch die
  // Anwendung über die Grenze reicht.

  protected UUID persistFach(String name, String kuerzel) {
    return stammdatenpflege.legeFachAn(name, kuerzel);
  }

  protected UUID persistLehrkraft(String vorname, String nachname, String kuerzel) {
    return stammdatenpflege.legeLehrkraftAn(vorname, nachname, kuerzel);
  }

  protected UUID persistKlasse(String name) {
    return stammdatenpflege.legeKlasseAn(name);
  }

  protected UUID persistLehrauftrag(UUID lehrkraft, UUID klasse, UUID fach) {
    return stammdatenpflege.erteileLehrauftrag(lehrkraft, klasse, fach);
  }

  /**
   * Ein Sprechtag im gewünschten Status, direkt über das Aggregat-Repository — <b>ohne</b>
   * Materialisierung. Der Weg über die Use Cases steht den Tests offen, wo er geprüft werden soll;
   * als Fixture wäre er nur Umweg und brächte Termine mit, die der Test nicht bestellt hat.
   */
  protected Sprechtag persistSprechtag(
      String titel,
      LocalDate datum,
      LocalTime beginn,
      LocalTime ende,
      int slotMinuten,
      SprechtagStatus status,
      UUID... klassen) {
    return persistSprechtag(titel, null, datum, beginn, ende, slotMinuten, status, klassen);
  }

  /** Wie oben, mit Ort — den braucht nur der Bestätigungsversand. */
  protected Sprechtag persistSprechtag(
      String titel,
      String ort,
      LocalDate datum,
      LocalTime beginn,
      LocalTime ende,
      int slotMinuten,
      SprechtagStatus status,
      UUID... klassen) {
    Sprechtag sprechtag =
        Sprechtag.entwirf(
            titel,
            ort,
            null,
            Schulkontakt.von(SCHULKONTAKT),
            AccessToken.neu(),
            datum,
            new Zeitfenster(beginn, ende),
            Slotdauer.vonMinuten(slotMinuten),
            Arrays.stream(klassen).map(KlasseId::von).toList());
    switch (status) {
      case ENTWURF -> {}
      case VEROEFFENTLICHT -> sprechtag.veroeffentliche();
      case ABGESAGT -> {
        sprechtag.veroeffentliche();
        sprechtag.sageAb();
      }
      case ABGESCHLOSSEN -> {
        sprechtag.veroeffentliche();
        sprechtag.schliesseAb();
      }
    }
    // Die Ereignisse der Fixture gehören niemandem: Sie sind nicht der Vorgang, den der Test prüft.
    sprechtag.ereignisseAbholen();
    sprechtage.speichere(sprechtag);
    return sprechtag;
  }

  /** Der gespeicherte Stand eines Sprechtags — nach jedem Schreiben neu zu laden. */
  protected Sprechtag ladeSprechtag(UUID id) {
    return sprechtage
        .lade(SprechtagId.von(id))
        .orElseThrow();
  }
}
