package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.domain.BuchungStatusEnum;
import de.openclassware.elternsprechtag.domain.Fach;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.domain.Termin;
import de.openclassware.elternsprechtag.domain.TerminStatusEnum;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsAnfrage;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsWunsch;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsZeile;
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftOption;
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftPlan;
import de.openclassware.elternsprechtag.services.BuchungService.SprechtagAuswertung;
import de.openclassware.elternsprechtag.services.BuchungService.TerminBelegtException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({SprechtagService.class, BuchungService.class, KlassenService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BuchungServiceTest extends AbstractServiceTest {

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
            SprechtagStatusEnum.ENTWURF, klasse);
    sprechtagService.changeStatus(sprechtag.getId(), SprechtagStatusEnum.VEROEFFENTLICHT);
    return new Fixture(sprechtag, klasse, lehrauftrag, lehrer);
  }

  private List<Termin> termineSorted() {
    return terminRepository.findAll().stream()
        .sorted(Comparator.comparing(Termin::getStartzeit))
        .toList();
  }

  private BuchungsAnfrage anfrage(Lehrauftrag lehrauftrag, Termin... termine) {
    List<BuchungsWunsch> wuensche =
        java.util.Arrays.stream(termine)
            .map(t -> new BuchungsWunsch(lehrauftrag.getId(), t.getId()))
            .toList();
    return new BuchungsAnfrage(
        "Eltern Müller", "Kind Müller", "eltern.mueller@example.com", "Bitte pünktlich", wuensche);
  }

  @Test
  void ladeLehrkraftOptionen_returnsTeacherWithSlotsAndSubjects() {
    Fixture f = publishedSprechtag();

    List<LehrkraftOption> optionen =
        buchungService.ladeLehrkraftOptionen(f.sprechtag().getId(), f.klasse().getId());

    assertThat(optionen).hasSize(1);
    LehrkraftOption option = optionen.get(0);
    assertThat(option.lehrauftragId()).isEqualTo(f.lehrauftrag().getId());
    assertThat(option.faecher()).containsExactly("Deutsch");
    assertThat(option.slots()).hasSize(4);
    assertThat(option.slots()).allSatisfy(slot -> assertThat(slot.belegt()).isFalse());
  }

  @Test
  void buchen_happyPath_createsBuchungAndMarksTerminBelegt() {
    Fixture f = publishedSprechtag();
    Termin termin = termineSorted().get(0);

    int gebucht = buchungService.buchen(anfrage(f.lehrauftrag(), termin));

    assertThat(gebucht).isEqualTo(1);
    Buchung persisted = buchungRepository.findAll().get(0);
    assertThat(persisted.getStatus()).isEqualTo(BuchungStatusEnum.ZUGESAGT);
    assertThat(persisted.getElternName()).isEqualTo("Eltern Müller");
    assertThat(persisted.getElternEmail()).isEqualTo("eltern.mueller@example.com");
    assertThat(terminRepository.findById(termin.getId()).orElseThrow().getStatus())
        .isEqualTo(TerminStatusEnum.BELEGT);
    assertThat(buchungRepository.count()).isEqualTo(1);
  }

  @Test
  void buchen_persistsElternEmail_onEveryBuchung() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = termineSorted();

    buchungService.buchen(anfrage(f.lehrauftrag(), frei.get(0), frei.get(1)));

    assertThat(buchungRepository.findAll())
        .hasSize(2)
        .allSatisfy(b -> assertThat(b.getElternEmail()).isEqualTo("eltern.mueller@example.com"));
  }

  @Test
  void buchen_multipleWuensche_allBooked() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = termineSorted();

    int gebucht = buchungService.buchen(anfrage(f.lehrauftrag(), frei.get(0), frei.get(1)));

    assertThat(gebucht).isEqualTo(2);
    assertThat(buchungRepository.count()).isEqualTo(2);
    assertThat(terminRepository.findById(frei.get(0).getId()).orElseThrow().getStatus())
        .isEqualTo(TerminStatusEnum.BELEGT);
    assertThat(terminRepository.findById(frei.get(1).getId()).orElseThrow().getStatus())
        .isEqualTo(TerminStatusEnum.BELEGT);
  }

  @Test
  void buchen_slotAlreadyBelegt_throwsAndPersistsNothing() {
    Fixture f = publishedSprechtag();
    Termin termin = termineSorted().get(0);
    termin.setStatus(TerminStatusEnum.BELEGT);
    terminRepository.save(termin);

    assertThatThrownBy(() -> buchungService.buchen(anfrage(f.lehrauftrag(), termin)))
        .isInstanceOf(TerminBelegtException.class);
    assertThat(buchungRepository.count()).isZero();
  }

  @Test
  void buchen_allOrNothing_rollsBackEarlierSlotWhenLaterSlotTaken() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = termineSorted();
    Termin first = frei.get(0);
    Termin second = frei.get(1);
    // Zweiter Slot wird zwischenzeitlich vergeben.
    second.setStatus(TerminStatusEnum.BELEGT);
    terminRepository.save(second);

    assertThatThrownBy(() -> buchungService.buchen(anfrage(f.lehrauftrag(), first, second)))
        .isInstanceOf(TerminBelegtException.class);

    // Kronjuwel: der bereits verarbeitete erste Slot wurde zurückgerollt, keine Buchung übrig.
    assertThat(terminRepository.findById(first.getId()).orElseThrow().getStatus())
        .as("erster Slot muss nach Rollback wieder FREI sein")
        .isEqualTo(TerminStatusEnum.FREI);
    assertThat(buchungRepository.count()).isZero();
  }

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
            SprechtagStatusEnum.ENTWURF, klasse);
    sprechtagService.changeStatus(sprechtag.getId(), SprechtagStatusEnum.VEROEFFENTLICHT);
    return new AuswertungFixture(sprechtag, klasse, berg, bergAuftrag, adler, adlerAuftrag);
  }

  private List<Termin> termineOf(Lehrer lehrer) {
    return terminRepository.findAll().stream()
        .filter(termin -> termin.getLehrer().getId().equals(lehrer.getId()))
        .sorted(Comparator.comparing(Termin::getStartzeit))
        .toList();
  }

  private void book(
      Lehrauftrag auftrag, Termin termin, String eltern, String schueler, String notiz) {
    buchungService.buchen(
        new BuchungsAnfrage(
            eltern,
            schueler,
            "eltern@example.com",
            notiz,
            List.of(new BuchungsWunsch(auftrag.getId(), termin.getId()))));
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

    SprechtagAuswertung auswertung = buchungService.werteAus(f.sprechtag().getId()).orElseThrow();

    assertThat(auswertung.titel()).isEqualTo("Frühling");
    assertThat(auswertung.datum()).isEqualTo(DATE);
    assertThat(auswertung.plaene())
        .extracting(LehrkraftPlan::anzeigeName)
        .containsExactly("Carl Adler", "Anna Berg");
  }

  @Test
  void werteAus_teacherWithoutBooking_hasZeroCountAndEmptyZeilen() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    book(f.bergAuftrag(), termineOf(f.berg()).get(0), "Eltern A", "Kind A", "n");

    SprechtagAuswertung auswertung = buchungService.werteAus(f.sprechtag().getId()).orElseThrow();

    LehrkraftPlan adler = planOf(auswertung, f.adler());
    assertThat(adler.anzahl()).isZero();
    assertThat(adler.zeilen()).isEmpty();
  }

  @Test
  void werteAus_cancelledBookingsExcluded_activeIncluded() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    List<Termin> bergSlots = termineOf(f.berg());
    book(f.bergAuftrag(), bergSlots.get(0), "Eltern A", "Kind A", "n1");
    book(f.bergAuftrag(), bergSlots.get(1), "Eltern B", "Kind B", "n2");
    Buchung storniert =
        buchungRepository.findAll().stream()
            .filter(b -> b.getSchuelerName().equals("Kind A"))
            .findFirst()
            .orElseThrow();
    storniert.setStatus(BuchungStatusEnum.ABGESAGT);
    buchungRepository.save(storniert);

    SprechtagAuswertung auswertung = buchungService.werteAus(f.sprechtag().getId()).orElseThrow();

    LehrkraftPlan berg = planOf(auswertung, f.berg());
    assertThat(berg.anzahl()).isEqualTo(1);
    assertThat(berg.zeilen())
        .extracting(BuchungsZeile::schuelerName)
        .containsExactly("Kind B");
  }

  @Test
  void werteAus_zeileFields_areMappedCorrectly() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    Termin slot = termineOf(f.berg()).get(0); // 14:00
    book(f.bergAuftrag(), slot, "Eltern Müller", "Lukas Müller", "Leistung besprechen");

    SprechtagAuswertung auswertung = buchungService.werteAus(f.sprechtag().getId()).orElseThrow();

    BuchungsZeile zeile = planOf(auswertung, f.berg()).zeilen().get(0);
    assertThat(zeile.startzeit()).isEqualTo(LocalTime.of(14, 0));
    assertThat(zeile.schuelerName()).isEqualTo("Lukas Müller");
    assertThat(zeile.klasse()).isEqualTo("5a");
    assertThat(zeile.fach()).isEqualTo("Deutsch");
    assertThat(zeile.elternName()).isEqualTo("Eltern Müller");
    assertThat(zeile.notiz()).isEqualTo("Leistung besprechen");
  }

  @Test
  void werteAus_bookingsPerTeacher_sortedByStartzeit() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    List<Termin> slots = termineOf(f.berg());
    // Bewusst in verdrehter Reihenfolge buchen — die Auswertung muss chronologisch sortieren.
    book(f.bergAuftrag(), slots.get(2), "E3", "K3", "n"); // 14:30
    book(f.bergAuftrag(), slots.get(0), "E1", "K1", "n"); // 14:00
    book(f.bergAuftrag(), slots.get(1), "E2", "K2", "n"); // 14:15

    SprechtagAuswertung auswertung = buchungService.werteAus(f.sprechtag().getId()).orElseThrow();

    assertThat(planOf(auswertung, f.berg()).zeilen())
        .extracting(BuchungsZeile::startzeit)
        .containsExactly(LocalTime.of(14, 0), LocalTime.of(14, 15), LocalTime.of(14, 30));
  }

  @Test
  void werteAus_countPerTeacher_matchesActiveBookings() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();
    List<Termin> bergSlots = termineOf(f.berg());
    book(f.bergAuftrag(), bergSlots.get(0), "E1", "K1", "n");
    book(f.bergAuftrag(), bergSlots.get(1), "E2", "K2", "n");
    book(f.adlerAuftrag(), termineOf(f.adler()).get(0), "E3", "K3", "n");

    SprechtagAuswertung auswertung = buchungService.werteAus(f.sprechtag().getId()).orElseThrow();

    assertThat(planOf(auswertung, f.berg()).anzahl()).isEqualTo(2);
    assertThat(planOf(auswertung, f.adler()).anzahl()).isEqualTo(1);
  }

  @Test
  void werteAus_sprechtagWithoutAnyBooking_returnsAllTeachersWithZero() {
    AuswertungFixture f = publishedSprechtagWithTwoTeachers();

    SprechtagAuswertung auswertung = buchungService.werteAus(f.sprechtag().getId()).orElseThrow();

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
    assertThat(buchungService.werteAus(UUID.randomUUID())).isEmpty();
  }

  @Test
  void buchen_terminAndLehrauftragDifferentTeacher_throwsIllegalArgument() {
    Fixture f = publishedSprechtag();
    // Zweite Lehrkraft mit eigenem Lehrauftrag (aber ohne materialisierte Termine).
    Lehrer anderer = persistLehrer("Bob", "Klein", "KLE");
    Lehrauftrag fremderAuftrag = persistLehrauftrag(anderer, f.klasse(), persistFach("Mathe", "M"));
    Termin terminVonLehrer1 = termineSorted().get(0);

    assertThatThrownBy(
            () -> buchungService.buchen(anfrage(fremderAuftrag, terminVonLehrer1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(buchungRepository.count()).isZero();
  }
}
