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
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftOption;
import de.openclassware.elternsprechtag.services.BuchungService.TerminBelegtException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
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
    return new BuchungsAnfrage("Eltern Müller", "Kind Müller", "Bitte pünktlich", wuensche);
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
    assertThat(terminRepository.findById(termin.getId()).orElseThrow().getStatus())
        .isEqualTo(TerminStatusEnum.BELEGT);
    assertThat(buchungRepository.count()).isEqualTo(1);
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
