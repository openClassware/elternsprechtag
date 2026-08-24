package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.domain.Fach;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.domain.TerminStatusEnum;
import de.openclassware.elternsprechtag.services.KlassenService.KlasseOption;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagForm;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagRow;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@ServiceTest
@Import({SprechtagService.class, BuchungService.class, KlassenService.class})
@RecordApplicationEvents
class SprechtagServiceTest extends AbstractServiceTest {

  @Autowired private ApplicationEvents events;

  private static final LocalDate DATE = LocalDate.of(2026, 7, 20);

  private SprechtagForm form(String titel, LocalTime start, LocalTime end, int slot, Klasse... klassen) {
    SprechtagForm form = new SprechtagForm();
    form.setTitel(titel);
    form.setStartDate(DATE);
    form.setStartTime(start);
    form.setEndTime(end);
    form.setSlotInMinutes(slot);
    form.setKlassen(
        Set.copyOf(
            java.util.Arrays.stream(klassen)
                .map(k -> new KlasseOption(k.getId(), k.getName()))
                .toList()));
    return form;
  }

  @Test
  void createOrUpdate_create_persistsFieldsAndDoesNotMaterializeDraft() {
    Klasse klasse = persistKlasse("5a");

    UUID id =
        sprechtagService.createOrUpdate(
            null, form("Frühling", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse),
            SprechtagStatusEnum.ENTWURF);

    Sprechtag saved = sprechtagRepository.findById(id).orElseThrow();
    assertThat(saved.getTitel()).isEqualTo("Frühling");
    assertThat(saved.getStatus()).isEqualTo(SprechtagStatusEnum.ENTWURF);
    assertThat(saved.getKlassen()).extracting(Klasse::getName).containsExactly("5a");
    assertThat(terminRepository.count()).as("Entwurf materialisiert keine Termine").isZero();
  }

  @Test
  void createOrUpdate_update_keepsIdAndChangesFields() {
    Klasse klasse = persistKlasse("5a");
    UUID id =
        sprechtagService.createOrUpdate(
            null, form("Alt", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse),
            SprechtagStatusEnum.ENTWURF);

    UUID sameId =
        sprechtagService.createOrUpdate(
            id, form("Neu", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse),
            SprechtagStatusEnum.ENTWURF);

    assertThat(sameId).isEqualTo(id);
    assertThat(sprechtagRepository.count()).isEqualTo(1);
    assertThat(sprechtagRepository.findById(id).orElseThrow().getTitel()).isEqualTo("Neu");
  }

  @Test
  void changeStatus_publish_materializesOneTerminPerTeacherAndSlot() {
    Klasse klasse = persistKlasse("5a");
    Lehrer lehrer = persistLehrer("Anna", "Berg", "BER");
    Fach fach = persistFach("Deutsch", "D");
    persistLehrauftrag(lehrer, klasse, fach);
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatusEnum.ENTWURF, klasse);

    sprechtagService.changeStatus(sprechtag.getId(), SprechtagStatusEnum.VEROEFFENTLICHT);

    // 14:00–15:00 in 15-min-Slots => 4 Slots, 1 Lehrkraft => 4 Termine, alle FREI.
    assertThat(terminRepository.count()).isEqualTo(4);
    assertThat(terminRepository.findAll())
        .allSatisfy(t -> assertThat(t.getStatus()).isEqualTo(TerminStatusEnum.FREI))
        .allSatisfy(t -> assertThat(t.getLehrer().getId()).isEqualTo(lehrer.getId()));
  }

  @Test
  void materialize_dropsPartialTrailingSlot() {
    Klasse klasse = persistKlasse("5a");
    Lehrer lehrer = persistLehrer("Anna", "Berg", "BER");
    persistLehrauftrag(lehrer, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Kurz", DATE, LocalTime.of(14, 0), LocalTime.of(14, 50), 20,
            SprechtagStatusEnum.ENTWURF, klasse);

    sprechtagService.changeStatus(sprechtag.getId(), SprechtagStatusEnum.VEROEFFENTLICHT);

    // 14:00 und 14:20 passen (je +20). 14:40+20=15:00 > 14:50 => entfällt. => 2 Slots.
    assertThat(terminRepository.count()).isEqualTo(2);
  }

  @Test
  void publish_isIdempotent_doesNotDuplicateTermine() {
    Klasse klasse = persistKlasse("5a");
    Lehrer lehrer = persistLehrer("Anna", "Berg", "BER");
    persistLehrauftrag(lehrer, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatusEnum.ENTWURF, klasse);

    sprechtagService.changeStatus(sprechtag.getId(), SprechtagStatusEnum.VEROEFFENTLICHT);
    assertThat(terminRepository.count()).isEqualTo(4);

    // Erneutes Speichern eines bereits veröffentlichten Sprechtags erzeugt keine neuen Termine.
    sprechtagService.createOrUpdate(
        sprechtag.getId(),
        form("Frühling", LocalTime.of(14, 0), LocalTime.of(15, 0), 15, klasse),
        SprechtagStatusEnum.VEROEFFENTLICHT);

    assertThat(terminRepository.count()).isEqualTo(4);
  }

  @Test
  void changeStatus_cancelPublished_publishesAbsageEvent() {
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatusEnum.VEROEFFENTLICHT, persistKlasse("5a"));

    sprechtagService.changeStatus(sprechtag.getId(), SprechtagStatusEnum.ABGESAGT);

    assertThat(events.stream(SprechtagAbgesagtEvent.class))
        .extracting(SprechtagAbgesagtEvent::sprechtagId)
        .containsExactly(sprechtag.getId());
  }

  @Test
  void changeStatus_complete_doesNotPublishAbsageEvent() {
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatusEnum.VEROEFFENTLICHT, persistKlasse("5a"));

    sprechtagService.changeStatus(sprechtag.getId(), SprechtagStatusEnum.ABGESCHLOSSEN);

    assertThat(events.stream(SprechtagAbgesagtEvent.class)).isEmpty();
  }

  @Test
  void changeStatus_invalidTransition_throws() {
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatusEnum.ENTWURF, persistKlasse("5a"));

    // ENTWURF -> ABGESCHLOSSEN ist nicht erlaubt.
    assertThatThrownBy(
            () ->
                sprechtagService.changeStatus(
                    sprechtag.getId(), SprechtagStatusEnum.ABGESCHLOSSEN))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void findAllRows_sortedByStartDate_withKlassenNames() {
    Klasse klasse = persistKlasse("5a");
    persistSprechtag(
        "Später", LocalDate.of(2026, 9, 1), LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
        SprechtagStatusEnum.ENTWURF, klasse);
    persistSprechtag(
        "Früher", LocalDate.of(2026, 3, 1), LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
        SprechtagStatusEnum.ENTWURF, klasse);

    var rows = sprechtagService.findAllRows();

    assertThat(rows).extracting(SprechtagRow::titel).containsExactly("Früher", "Später");
    assertThat(rows.get(0).klassen()).containsExactly("5a");
  }

  @Test
  void loadForm_mapsSelectedKlassenToOptions() {
    Klasse a = persistKlasse("5a");
    Klasse b = persistKlasse("7a");
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatusEnum.ENTWURF, a, b);

    SprechtagForm loaded = sprechtagService.loadForm(sprechtag.getId()).orElseThrow();

    assertThat(loaded.getTitel()).isEqualTo("Frühling");
    assertThat(loaded.getKlassen())
        .extracting(KlasseOption::name)
        .containsExactlyInAnyOrder("5a", "7a");
  }

  @Test
  void duplicate_createsDraftWithFreshTokenAndSameKlassen() {
    Klasse klasse = persistKlasse("5a");
    Sprechtag original =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatusEnum.VEROEFFENTLICHT, klasse);

    UUID copyId = sprechtagService.duplicate(original.getId());

    Sprechtag copy = sprechtagRepository.findById(copyId).orElseThrow();
    assertThat(copy.getStatus()).isEqualTo(SprechtagStatusEnum.ENTWURF);
    assertThat(copy.getAccessToken()).isNotEqualTo(original.getAccessToken());
    assertThat(copy.getKlassen()).extracting(Klasse::getName).containsExactly("5a");
  }
}
