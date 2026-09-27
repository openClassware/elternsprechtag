package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anonymisieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht.Datenfrist;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht.SprechtagZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Die Vorwarnung vor der Anonymisierung in der Sprechtag-Liste (Issue #128) gegen eine echte
 * Datenbank, mit der Standardfrist von 30 Tagen. Die Sprechtage enden um 15:00 — der letzte Tag der
 * Frist ist damit derselbe Kalendertag 30 Tage später. Die Grenzfälle der Datumsrechnung stehen ohne
 * Spring in {@code SprechtagTest}.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class SprechtagsuebersichtTest extends AbstractServiceTest {

  @Autowired private Anonymisieren anonymisieren;

  private record Fixture(Sprechtag sprechtag, UUID lehrauftrag) {}

  /** Ein veröffentlichter Sprechtag am gewünschten Datum mit materialisierten Slots. */
  private Fixture veroeffentlichterSprechtag(LocalDate datum) {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling",
            datum,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            15,
            SprechtagStatus.ENTWURF,
            klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, lehrauftrag);
  }

  /** Über den Organizer-Nachtrag: Er bleibt offen bis zum Abschluss, auch nach dem Datum. */
  private void buche(UUID lehrauftrag, Termin termin, String name) {
    nachtragen.trageNach(
        new NachtragsAnfrage(
            "Eltern " + name,
            "Kind " + name,
            name + "@example.com",
            List.of(new NachtragsWunsch(lehrauftrag, termin.id().wert(), "Anliegen " + name))));
  }

  /** Die Zeile eines Sprechtags in der Liste des Organizers. */
  private SprechtagZeile zeileVon(UUID id) {
    return sprechtagsuebersicht.alle().stream()
        .filter(zeile -> zeile.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  /** Solange der Lauf den Sprechtag nicht erfasst hat, nennt die Liste den letzten Tag der Frist. */
  @Test
  void abgeschlossenVorDemLauf_nenntDenLetztenTagDerFrist() {
    LocalDate datum = LocalDate.now().minusDays(10);
    Fixture f = veroeffentlichterSprechtag(datum);
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    schliesseAb(f.sprechtag().id().wert());

    SprechtagZeile zeile = zeileVon(f.sprechtag().id().wert());

    assertThat(zeile.datenfrist())
        .isEqualTo(new Datenfrist(Datenfrist.Art.VERFUEGBAR_BIS, datum.plusDays(30)));
  }

  /** Die Frist gilt auch für den abgesagten Sprechtag — seine Buchungen fallen genauso. */
  @Test
  void abgesagtVorDemLauf_nenntDenLetztenTagDerFrist() {
    LocalDate datum = LocalDate.now().minusDays(10);
    Fixture f = veroeffentlichterSprechtag(datum);
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    absagen.sageAb(f.sprechtag().id().wert());

    SprechtagZeile zeile = zeileVon(f.sprechtag().id().wert());

    assertThat(zeile.datenfrist())
        .isEqualTo(new Datenfrist(Datenfrist.Art.VERFUEGBAR_BIS, datum.plusDays(30)));
  }

  /**
   * Der Lauf fragt nicht nach Buchungen, also auch die Liste nicht: Ein Sprechtag, der abgesagt
   * wurde, bevor jemand buchte, warnt wie jeder andere.
   */
  @Test
  void abgesagtOhneBuchung_warntTrotzdem() {
    LocalDate datum = LocalDate.now().plusDays(5);
    Fixture f = veroeffentlichterSprechtag(datum);
    absagen.sageAb(f.sprechtag().id().wert());

    SprechtagZeile zeile = zeileVon(f.sprechtag().id().wert());

    assertThat(zeile.datenfrist())
        .isEqualTo(new Datenfrist(Datenfrist.Art.VERFUEGBAR_BIS, datum.plusDays(30)));
  }

  /** Nach dem Lauf nennt dieselbe Stelle den Tag, an dem er den Sprechtag erledigt hat. */
  @Test
  void nachDemLauf_nenntDenTagDerAnonymisierung() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().minusDays(31));
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    schliesseAb(f.sprechtag().id().wert());
    anonymisieren.anonymisiere();

    SprechtagZeile zeile = zeileVon(f.sprechtag().id().wert());

    assertThat(zeile.datenfrist())
        .isEqualTo(new Datenfrist(Datenfrist.Art.ENTFERNT_AM, LocalDate.now()));
  }

  /**
   * Ein Lauf, der vor dem Vermerk abbricht, hat die Termine schon anonymisiert — die Liste springt
   * trotzdem erst mit dem Vermerk um und warnt bis dahin weiter.
   */
  @Test
  void nachAbgebrochenemLauf_warntWeiter() {
    LocalDate datum = LocalDate.now().minusDays(31);
    Fixture f = veroeffentlichterSprechtag(datum);
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    schliesseAb(f.sprechtag().id().wert());
    anonymisieren.anonymisiere();
    // Der Abbruch zwischen letztem Termin und Vermerk, nachgestellt wie in AnonymisierenTest.
    jdbc.update("update sprechtage set anonymisiert_am = null");

    SprechtagZeile zeile = zeileVon(f.sprechtag().id().wert());

    assertThat(zeile.datenfrist())
        .isEqualTo(new Datenfrist(Datenfrist.Art.VERFUEGBAR_BIS, datum.plusDays(30)));
  }

  /** Entwürfe und Veröffentlichte erreicht der Lauf nicht — sie tragen keine Vorwarnung. */
  @Test
  void entwurfUndVeroeffentlicht_ohneVorwarnung() {
    UUID klasse = persistKlasse("6b");
    Sprechtag entwurf =
        persistSprechtag(
            "Entwurf", LocalDate.now().minusDays(40), LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    Sprechtag veroeffentlicht =
        persistSprechtag(
            "Veröffentlicht", LocalDate.now().plusDays(3), LocalTime.of(14, 0), LocalTime.of(15, 0),
            15, SprechtagStatus.VEROEFFENTLICHT, klasse);

    assertThat(zeileVon(entwurf.id().wert()).datenfrist()).isNull();
    assertThat(zeileVon(veroeffentlicht.id().wert()).datenfrist()).isNull();
  }
}
