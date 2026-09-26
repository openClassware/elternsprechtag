package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anonymisieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsstatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Pseudonymisierung;
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
 * Der tägliche Anonymisierungs-Lauf (Issue #126) gegen eine echte Datenbank, mit der Standardfrist
 * von 30 Tagen: Kandidaten kommen aus den Query-Ports, verbindlich geprüft und geändert wird an
 * jedem Aggregat. Die Grenzfälle der Frist stehen ohne Spring in {@code SprechtagTest}, die der
 * Ersatzwerte in {@code TerminTest}.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class AnonymisierenTest extends AbstractServiceTest {

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

  @Test
  void anonymisiere_nachAblauf_ersetztAlleBuchungenUndVermerktDenSprechtag() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().minusDays(31));
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    buche(f.lehrauftrag(), alleTermine().get(1), "schmidt");
    storniere(buchung -> buchung.familie().elternName().equals("Eltern schmidt"));
    schliesseAb(f.sprechtag().id().wert());

    int anzahl = anonymisieren.anonymisiere();

    assertThat(anzahl).isEqualTo(1);
    List<Buchung> buchungen = alleBuchungen();
    assertThat(buchungen).hasSize(2);
    assertThat(buchungen)
        .allSatisfy(
            buchung -> {
              assertThat(buchung.familie().elternName()).matches("Eltern-[0-9a-f]{8}-00[12]");
              assertThat(buchung.familie().schuelerName()).matches("Schueler-[0-9a-f]{8}-00[12]");
              assertThat(buchung.familie().email()).isEqualTo(Pseudonymisierung.EMAIL);
              assertThat(buchung.notiz()).isEmpty();
            });
    assertThat(buchungen)
        .extracting(Buchung::status)
        .containsExactly(Buchungsstatus.ZUGESAGT, Buchungsstatus.STORNIERT);
    Sprechtag danach = ladeSprechtag(f.sprechtag().id().wert());
    assertThat(danach.anonymisiertAm()).isPresent();
    assertThat(danach.status()).isEqualTo(SprechtagStatus.ABGESCHLOSSEN);
  }

  /** Die Frist gilt auch für den abgesagten Sprechtag — seine Buchungen sind genauso personenbezogen. */
  @Test
  void anonymisiere_auchDenAbgesagtenSprechtag() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().minusDays(31));
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    absagen.sageAb(f.sprechtag().id().wert());

    int anzahl = anonymisieren.anonymisiere();

    assertThat(anzahl).isEqualTo(1);
    assertThat(alleBuchungen())
        .singleElement()
        .satisfies(b -> assertThat(b.familie().email()).isEqualTo(Pseudonymisierung.EMAIL));
    assertThat(ladeSprechtag(f.sprechtag().id().wert()).anonymisiertAm()).isPresent();
  }

  /**
   * Ein Lauf, der nach den Terminen, aber vor dem Vermerk abbricht, lässt den Sprechtag fällig — der
   * nächste wiederholt ihn und überschreibt die Pseudonyme nur ein weiteres Mal.
   */
  @Test
  void anonymisiere_nachAbbruchVorDemVermerk_wiederholtDerNaechsteLaufDenSprechtag() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().minusDays(31));
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    schliesseAb(f.sprechtag().id().wert());
    anonymisieren.anonymisiere();
    // Der Abbruch zwischen letztem Termin und Vermerk, nachgestellt: Termine anonymisiert, kein
    // Vermerk. Der Stand ist anders nicht herzustellen, ohne einen Fehler einzuschleusen.
    jdbc.update("update sprechtage set anonymisiert_am = null");

    int anzahl = anonymisieren.anonymisiere();

    assertThat(anzahl).isEqualTo(1);
    assertThat(alleBuchungen())
        .singleElement()
        .satisfies(b -> assertThat(b.familie().elternName()).matches("Eltern-[0-9a-f]{8}-001"));
    assertThat(ladeSprechtag(f.sprechtag().id().wert()).anonymisiertAm()).isPresent();
  }

  @Test
  void anonymisiere_zweiterLauf_laesstDenErledigtenSprechtagStehen() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().minusDays(31));
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    schliesseAb(f.sprechtag().id().wert());
    anonymisieren.anonymisiere();
    String pseudonym = alleBuchungen().get(0).familie().elternName();

    int anzahl = anonymisieren.anonymisiere();

    assertThat(anzahl).isZero();
    assertThat(alleBuchungen().get(0).familie().elternName()).isEqualTo(pseudonym);
  }
}
