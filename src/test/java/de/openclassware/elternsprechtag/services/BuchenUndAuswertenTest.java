package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.domain.Fach;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.BuchungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchungsoptionen.LehrkraftOption;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.Notiz;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Die Use Cases Buchen, Auswerten und Buchungsoptionen gegen eine echte Datenbank — Nachfolger des
 * alten {@code BuchungServiceTest}.
 *
 * <p>Was hier geprüft wird, braucht die Datenbank: Rollback, optimistisches Sperren, die
 * Zusammenführung zweier Ports zu einem Read-Modell. Die Kernregel <em>ein Slot, eine Buchung</em>
 * steht dagegen ohne Spring und ohne Datenbank in {@code TerminTest} — sie gehört ins Aggregat.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class BuchenUndAuswertenTest extends AbstractServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 7, 20);

  /** Ein veröffentlichter Sprechtag mit einer Lehrkraft (4 materialisierte Slots). */
  private record Fixture(Sprechtag sprechtag, Klasse klasse, Lehrauftrag lehrauftrag, Lehrer lehrer) {}

  private Fixture publishedSprechtag() {
    Klasse klasse = persistKlasse("5a");
    Lehrer lehrer = persistLehrer("Anna", "Berg", "BER");
    Fach fach = persistFach("Deutsch", "D");
    Lehrauftrag lehrauftrag = persistLehrauftrag(lehrer, klasse, fach);
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, klasse, lehrauftrag, lehrer);
  }

  private BuchungsAnfrage anfrage(Lehrauftrag lehrauftrag, Termin... termine) {
    List<BuchungsWunsch> wuensche =
        Arrays.stream(termine)
            .map(t -> new BuchungsWunsch(lehrauftrag.getId(), t.id().wert(), "Bitte pünktlich"))
            .toList();
    return new BuchungsAnfrage(
        "Eltern Müller", "Kind Müller", "eltern.mueller@example.com", wuensche);
  }

  @Test
  void ladeLehrkraftOptionen_returnsTeacherWithSlotsAndSubjects() {
    Fixture f = publishedSprechtag();

    List<LehrkraftOption> optionen =
        buchungsoptionen.ladeLehrkraftOptionen(f.sprechtag().id().wert(), f.klasse().getId());

    assertThat(optionen).hasSize(1);
    LehrkraftOption option = optionen.get(0);
    assertThat(option.lehrauftragId()).isEqualTo(f.lehrauftrag().getId());
    assertThat(option.faecher()).containsExactly("Deutsch");
    assertThat(option.slots()).hasSize(4);
    assertThat(option.slots()).allSatisfy(slot -> assertThat(slot.buchbar()).isTrue());
  }

  @Test
  void ladeLehrkraftOptionen_gebuchterSlot_istNichtMehrWaehlbar() {
    Fixture f = publishedSprechtag();
    Termin termin = alleTermine().get(0);
    buchen.buchen(anfrage(f.lehrauftrag(), termin));

    List<LehrkraftOption> optionen =
        buchungsoptionen.ladeLehrkraftOptionen(f.sprechtag().id().wert(), f.klasse().getId());

    assertThat(optionen.get(0).slots())
        .filteredOn(slot -> slot.terminId().equals(termin.id().wert()))
        .singleElement()
        .satisfies(slot -> assertThat(slot.buchbar()).isFalse());
  }

  @Test
  void ladeLehrkraftOptionen_entfallenerSlot_istNichtWaehlbar() {
    Fixture f = publishedSprechtag();
    Termin termin = alleTermine().get(0);
    termin.lassEntfallen();
    termine.speichere(termin);

    List<LehrkraftOption> optionen =
        buchungsoptionen.ladeLehrkraftOptionen(f.sprechtag().id().wert(), f.klasse().getId());

    assertThat(optionen.get(0).slots())
        .filteredOn(slot -> slot.terminId().equals(termin.id().wert()))
        .singleElement()
        .satisfies(slot -> assertThat(slot.buchbar()).isFalse());
  }

  @Test
  void buchen_happyPath_createsBuchungAndMarksTerminBelegt() {
    Fixture f = publishedSprechtag();
    Termin termin = alleTermine().get(0);

    int gebucht = buchen.buchen(anfrage(f.lehrauftrag(), termin));

    assertThat(gebucht).isEqualTo(1);
    assertThat(alleBuchungen()).hasSize(1);
    Buchung persisted = alleBuchungen().get(0);
    assertThat(persisted.istAktiv()).isTrue();
    assertThat(persisted.familie().elternName()).isEqualTo("Eltern Müller");
    assertThat(persisted.familie().email()).isEqualTo("eltern.mueller@example.com");
    assertThat(termine.lade(termin.id()).orElseThrow().istBuchbar()).isFalse();
  }

  @Test
  void buchen_friertBuchungszielEin() {
    Fixture f = publishedSprechtag();

    buchen.buchen(anfrage(f.lehrauftrag(), alleTermine().get(0)));

    assertThat(alleBuchungen().get(0).ziel())
        .satisfies(
            ziel -> {
              assertThat(ziel.herkunft().wert()).isEqualTo(f.lehrauftrag().getId());
              assertThat(ziel.lehrkraftName()).isEqualTo("Anna Berg");
              assertThat(ziel.lehrkraftKuerzel()).isEqualTo("BER");
              assertThat(ziel.klasse()).isEqualTo("5a");
              assertThat(ziel.fach()).isEqualTo("Deutsch");
            });
  }

  @Test
  void buchen_persistsElternEmail_onEveryBuchung() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = alleTermine();

    buchen.buchen(anfrage(f.lehrauftrag(), frei.get(0), frei.get(1)));

    assertThat(alleBuchungen())
        .hasSize(2)
        .allSatisfy(
            b -> assertThat(b.familie().email()).isEqualTo("eltern.mueller@example.com"));
  }

  @Test
  void buchen_notizJeWunsch_persistedOnItsOwnBuchung() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = alleTermine();

    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern Müller",
            "Kind Müller",
            "eltern.mueller@example.com",
            List.of(
                new BuchungsWunsch(
                    f.lehrauftrag().getId(), frei.get(0).id().wert(), "Erstes Anliegen"),
                new BuchungsWunsch(
                    f.lehrauftrag().getId(), frei.get(1).id().wert(), "Zweites Anliegen"))));

    assertThat(alleTermine())
        .filteredOn(termin -> termin.aktiveBuchung().isPresent())
        .extracting(
            termin -> termin.id().wert(),
            termin -> termin.aktiveBuchung().orElseThrow().notiz().map(Notiz::text).orElse(null))
        .containsExactlyInAnyOrder(
            tuple(frei.get(0).id().wert(), "Erstes Anliegen"),
            tuple(frei.get(1).id().wert(), "Zweites Anliegen"));
  }

  @Test
  void buchen_wunschOhneNotiz_persistsBuchungOhneNotiz() {
    Fixture f = publishedSprechtag();
    Termin termin = alleTermine().get(0);

    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern Müller",
            "Kind Müller",
            "eltern.mueller@example.com",
            List.of(new BuchungsWunsch(f.lehrauftrag().getId(), termin.id().wert(), null))));

    assertThat(alleBuchungen().get(0).notiz()).isEmpty();
  }

  @Test
  void buchen_multipleWuensche_allBooked() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = alleTermine();

    int gebucht = buchen.buchen(anfrage(f.lehrauftrag(), frei.get(0), frei.get(1)));

    assertThat(gebucht).isEqualTo(2);
    assertThat(alleBuchungen()).hasSize(2);
    assertThat(termine.lade(frei.get(0).id()).orElseThrow().istBuchbar()).isFalse();
    assertThat(termine.lade(frei.get(1).id()).orElseThrow().istBuchbar()).isFalse();
  }

  @Test
  void buchen_slotAlreadyBelegt_throwsAndPersistsNothing() {
    Fixture f = publishedSprechtag();
    Termin termin = alleTermine().get(0);
    buchen.buchen(anfrage(f.lehrauftrag(), termin));

    Termin veraltet = termine.lade(termin.id()).orElseThrow();
    assertThatThrownBy(() -> buchen.buchen(anfrage(f.lehrauftrag(), veraltet)))
        .isInstanceOf(TerminBelegtException.class);
    assertThat(alleBuchungen()).hasSize(1);
  }

  @Test
  void buchen_allOrNothing_rollsBackEarlierSlotWhenLaterSlotTaken() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = alleTermine();
    Termin first = frei.get(0);
    Termin second = frei.get(1);
    // Zweiter Slot wird zwischenzeitlich vergeben — von einer anderen Familie.
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern Schmidt",
            "Kind Schmidt",
            "schmidt@example.com",
            List.of(new BuchungsWunsch(f.lehrauftrag().getId(), second.id().wert(), null))));

    assertThatThrownBy(() -> buchen.buchen(anfrage(f.lehrauftrag(), first, second)))
        .isInstanceOf(TerminBelegtException.class);

    // Kronjuwel: der bereits verarbeitete erste Slot wurde zurückgerollt, keine Buchung übrig.
    assertThat(termine.lade(first.id()).orElseThrow().istBuchbar())
        .as("erster Slot muss nach Rollback wieder buchbar sein")
        .isTrue();
    assertThat(alleBuchungen()).hasSize(1);
  }

  // Der Versionskonflikt am Root braucht echte Nebenläufigkeit und steht deshalb dort, wo er
  // entsteht: in TerminePersistenceAdapterTest. Hier ist nur die Übersetzung in
  // TerminBelegtException interessant, und die ist drei Zeilen im try/catch des Use Case.

  // --- Auswertung (Terminplan je Lehrkraft) ---

  /** Veröffentlichter Sprechtag, Klasse 5a mit zwei Lehrkräften (Adler < Berg alphabetisch). */
  private record AuswertungFixture(
      Sprechtag sprechtag,
      Klasse klasse,
      Lehrer berg,
      Lehrauftrag bergAuftrag,
      Lehrer adler,
      Lehrauftrag adlerAuftrag) {}

  private AuswertungFixture publishedSprechtagWithTwoTeachers() {
    Klasse klasse = persistKlasse("5a");
    Lehrer berg = persistLehrer("Anna", "Berg", "BER");
    Lehrer adler = persistLehrer("Carl", "Adler", "ADL");
    Lehrauftrag bergAuftrag = persistLehrauftrag(berg, klasse, persistFach("Deutsch", "D"));
    Lehrauftrag adlerAuftrag = persistLehrauftrag(adler, klasse, persistFach("Mathe", "M"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new AuswertungFixture(sprechtag, klasse, berg, bergAuftrag, adler, adlerAuftrag);
  }

  private void book(
      Lehrauftrag auftrag, Termin termin, String eltern, String schueler, String notiz) {
    buchen.buchen(
        new BuchungsAnfrage(
            eltern,
            schueler,
            "eltern@example.com",
            List.of(new BuchungsWunsch(auftrag.getId(), termin.id().wert(), notiz))));
  }

  private LehrkraftPlan planOf(SprechtagAuswertung auswertung, Lehrer lehrer) {
    return auswertung.plaene().stream()
        .filter(plan -> plan.lehrerId().equals(lehrer.getId()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void werteAus_returnsAllParticipatingTeachers_sortedByNachname() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    assertThat(auswertung.titel()).isEqualTo("Frühling");
    assertThat(auswertung.datum()).isEqualTo(DATE);
    assertThat(auswertung.plaene())
        .extracting(LehrkraftPlan::anzeigeName)
        .containsExactly("Carl Adler", "Anna Berg");
  }

  @Test
  void werteAus_teacherWithoutBooking_hasZeroCountAndEmptyZeilen() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    book(f.bergAuftrag(), termineVon(f.berg()).get(0), "Eltern A", "Kind A", "n");

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    LehrkraftPlan adler = planOf(auswertung, f.adler());
    assertThat(adler.anzahl()).isZero();
    assertThat(adler.zeilen()).isEmpty();
  }

  @Test
  void werteAus_cancelledBookingsExcluded_activeIncluded() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    List<Termin> bergSlots = termineVon(f.berg());
    book(f.bergAuftrag(), bergSlots.get(0), "Eltern A", "Kind A", "n1");
    book(f.bergAuftrag(), bergSlots.get(1), "Eltern B", "Kind B", "n2");
    storniere(buchung -> buchung.familie().schuelerName().equals("Kind A"));

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    LehrkraftPlan berg = planOf(auswertung, f.berg());
    assertThat(berg.anzahl()).isEqualTo(1);
    assertThat(berg.zeilen()).extracting(BuchungsZeile::schuelerName).containsExactly("Kind B");
  }

  @Test
  void werteAus_zeileFields_areMappedCorrectly() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    Termin slot = termineVon(f.berg()).get(0); // 14:00
    book(f.bergAuftrag(), slot, "Eltern Müller", "Lukas Müller", "Leistung besprechen");

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    BuchungsZeile zeile = planOf(auswertung, f.berg()).zeilen().get(0);
    assertThat(zeile.startzeit()).isEqualTo(LocalTime.of(14, 0));
    assertThat(zeile.schuelerName()).isEqualTo("Lukas Müller");
    assertThat(zeile.klasse()).isEqualTo("5a");
    assertThat(zeile.fach()).isEqualTo("Deutsch");
    assertThat(zeile.elternName()).isEqualTo("Eltern Müller");
    assertThat(zeile.notiz()).isEqualTo("Leistung besprechen");
  }

  @Test
  void werteAus_bleibtStabil_wennDerLehrauftragVerschwindet() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    book(f.bergAuftrag(), termineVon(f.berg()).get(0), "Eltern Müller", "Lukas Müller", "n");
    // Der kommende Import lässt Lehraufträge verschwinden; die Buchung hält ihren Stand selbst.
    jdbc.update("delete from lehrauftrag where id = ?", f.bergAuftrag().getId());

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    BuchungsZeile zeile = planOf(auswertung, f.berg()).zeilen().get(0);
    assertThat(zeile.klasse()).isEqualTo("5a");
    assertThat(zeile.fach()).isEqualTo("Deutsch");
  }

  @Test
  void werteAus_notizErscheintNurBeiIhrerLehrkraft() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();

    // Ein Submit an beide Lehrkräfte — jeder Wunsch trägt seine eigene Notiz.
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern Müller",
            "Lukas Müller",
            "eltern@example.com",
            List.of(
                new BuchungsWunsch(
                    f.bergAuftrag().getId(),
                    termineVon(f.berg()).get(0).id().wert(),
                    "Nur für Berg"),
                new BuchungsWunsch(
                    f.adlerAuftrag().getId(),
                    termineVon(f.adler()).get(1).id().wert(),
                    "Nur für Adler"))));

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    assertThat(planOf(auswertung, f.berg()).zeilen())
        .extracting(BuchungsZeile::notiz)
        .containsExactly("Nur für Berg");
    assertThat(planOf(auswertung, f.adler()).zeilen())
        .extracting(BuchungsZeile::notiz)
        .containsExactly("Nur für Adler");
  }

  @Test
  void werteAus_bookingsPerTeacher_sortedByStartzeit() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    List<Termin> slots = termineVon(f.berg());
    // Bewusst in verdrehter Reihenfolge buchen — die Auswertung muss chronologisch sortieren.
    book(f.bergAuftrag(), slots.get(2), "E3", "K3", "n"); // 14:30
    book(f.bergAuftrag(), slots.get(0), "E1", "K1", "n"); // 14:00
    book(f.bergAuftrag(), slots.get(1), "E2", "K2", "n"); // 14:15

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    assertThat(planOf(auswertung, f.berg()).zeilen())
        .extracting(BuchungsZeile::startzeit)
        .containsExactly(LocalTime.of(14, 0), LocalTime.of(14, 15), LocalTime.of(14, 30));
  }

  @Test
  void werteAus_countPerTeacher_matchesActiveBookings() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    List<Termin> bergSlots = termineVon(f.berg());
    book(f.bergAuftrag(), bergSlots.get(0), "E1", "K1", "n");
    book(f.bergAuftrag(), bergSlots.get(1), "E2", "K2", "n");
    book(f.adlerAuftrag(), termineVon(f.adler()).get(0), "E3", "K3", "n");

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    assertThat(planOf(auswertung, f.berg()).anzahl()).isEqualTo(2);
    assertThat(planOf(auswertung, f.adler()).anzahl()).isEqualTo(1);
  }

  @Test
  void werteAus_sprechtagWithoutAnyBooking_returnsAllTeachersWithZero() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();

    SprechtagAuswertung auswertung = auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow();

    assertThat(auswertung.plaene()).hasSize(2);
    assertThat(auswertung.plaene())
        .allSatisfy(
            plan -> {
              assertThat(plan.anzahl()).isZero();
              assertThat(plan.zeilen()).isEmpty();
            });
  }

  @Test
  void werteAus_unknownSprechtag_returnsEmpty() {
    assertThat(auswerten.werteAus(UUID.randomUUID())).isEmpty();
  }

  @Test
  void buchen_terminAndLehrauftragDifferentTeacher_throwsIllegalArgument() {
    Fixture f = publishedSprechtag();
    // Zweite Lehrkraft mit eigenem Lehrauftrag (aber ohne materialisierte Termine).
    Lehrer anderer = persistLehrer("Bob", "Klein", "KLE");
    Lehrauftrag fremderAuftrag = persistLehrauftrag(anderer, f.klasse(), persistFach("Mathe", "M"));
    Termin terminVonLehrer1 = alleTermine().get(0);

    assertThatThrownBy(() -> buchen.buchen(anfrage(fremderAuftrag, terminVonLehrer1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(alleBuchungen()).isEmpty();
  }
}
