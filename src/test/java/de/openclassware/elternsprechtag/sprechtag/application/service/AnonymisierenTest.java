package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Anonymisieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.BuchungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchungsstatus;
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
import org.springframework.test.context.TestPropertySource;

/**
 * Der tägliche Anonymisierungs-Lauf (Issue #126) gegen eine echte Datenbank, mit der Standardfrist
 * von 30 Tagen: Kandidaten kommen aus den Query-Ports, verbindlich geprüft und geändert wird an
 * jedem Aggregat. Die Grenzfälle der Frist stehen ohne Spring in {@code SprechtagTest}, die der
 * Ersatzwerte in {@code TerminTest}.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
// Eine Adresse abseits des Defaults: So ist belegt, dass sie aus der Konfiguration kommt.
@TestPropertySource(properties = "elternsprechtag.anonymisierung-email=" + AnonymisierenTest.EMAIL)
class AnonymisierenTest extends AbstractServiceTest {

  static final String EMAIL = "anonym@schule.example";

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
              assertThat(buchung.familie().email()).isEqualTo(EMAIL);
              assertThat(buchung.notiz()).isEmpty();
            });
    assertThat(buchungen)
        .extracting(Buchung::status)
        .containsExactly(Buchungsstatus.ZUGESAGT, Buchungsstatus.STORNIERT);
    Sprechtag danach = ladeSprechtag(f.sprechtag().id().wert());
    assertThat(danach.anonymisiertAm()).isPresent();
    // Issue #129: Der Lauf vermerkt auch an jeder Buchung — die Auswertung bietet dort kein
    // „Angaben entfernen" mehr an.
    assertThat(buchungen).allSatisfy(buchung -> assertThat(buchung.anonymisiertAm()).isPresent());
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
        .satisfies(b -> assertThat(b.familie().email()).isEqualTo(EMAIL));
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

  /** Solange die Frist läuft, trägt die Auswertung keinen Vermerk — sie soll keinen Hinweis zeigen. */
  @Test
  void werteAus_vorAblaufDerFrist_ohneAnonymisierungsdatum() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().minusDays(29));
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    schliesseAb(f.sprechtag().id().wert());
    anonymisieren.anonymisiere();

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    assertThat(auswertung.anonymisiertAm()).isNull();
    assertThat(auswertung.plaene().get(0).zeilen())
        .singleElement()
        .satisfies(zeile -> assertThat(zeile.elternName()).isEqualTo("Eltern mueller"));
  }

  /**
   * Nach dem Lauf erklärt das Datum, was die Auswertung zeigt (Issue #127): Pseudonyme statt Namen,
   * leere Notizen — Zähler und Zeilen je Lehrkraft bleiben, denn aus ihnen wird der nächste
   * Sprechtag geplant.
   */
  @Test
  void werteAus_nachDemLauf_traegtDasDatumDesLaufsUndDieUnveraenderteAuslastung() {
    Fixture f = veroeffentlichterSprechtag(LocalDate.now().minusDays(31));
    buche(f.lehrauftrag(), alleTermine().get(0), "mueller");
    buche(f.lehrauftrag(), alleTermine().get(2), "schmidt");
    entfallenLassen.entfallenLassen(List.of(alleTermine().get(3).id().wert()));
    schliesseAb(f.sprechtag().id().wert());
    UUID sprechtagId = f.sprechtag().id().wert();
    SprechtagAuswertung vorher = auswerten.werteAus(sprechtagId).orElseThrow();
    assertThat(vorher.plaene().get(0).entfalleneAnzahl()).isEqualTo(1);

    anonymisieren.anonymisiere();

    SprechtagAuswertung nachher = auswerten.werteAus(sprechtagId).orElseThrow();
    LocalDate tagDesLaufs = ladeSprechtag(sprechtagId).anonymisiertAm().orElseThrow().toLocalDate();
    assertThat(nachher.anonymisiertAm()).isEqualTo(tagDesLaufs);
    assertThat(nachher.plaene())
        .extracting(
            LehrkraftPlan::lehrerId,
            LehrkraftPlan::anzahl,
            LehrkraftPlan::entfalleneAnzahl,
            p -> p.zeilen().size())
        .containsExactlyElementsOf(
            vorher.plaene().stream()
                .map(p -> tuple(p.lehrerId(), p.anzahl(), p.entfalleneAnzahl(), p.zeilen().size()))
                .toList());
    assertThat(nachher.plaene().get(0).zeilen())
        .extracting(BuchungsZeile::startzeit, BuchungsZeile::klasse, BuchungsZeile::fach)
        .containsExactly(
            tuple(LocalTime.of(14, 0), "5a", "Deutsch"), tuple(LocalTime.of(14, 30), "5a", "Deutsch"));
    assertThat(nachher.plaene().get(0).zeilen())
        .allSatisfy(
            zeile -> {
              assertThat(zeile.elternName()).matches("Eltern-[0-9a-f]{8}-00[12]");
              assertThat(zeile.schuelerName()).matches("Schueler-[0-9a-f]{8}-00[12]");
              assertThat(zeile.notiz()).isNullOrEmpty();
            });
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
