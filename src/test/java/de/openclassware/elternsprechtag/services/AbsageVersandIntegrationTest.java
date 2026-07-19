package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.domain.Fach;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.domain.Termin;
import de.openclassware.elternsprechtag.services.AbsageBenachrichtigungService.AbsageEmpfaenger;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsAnfrage;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsWunsch;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-End der Absage-Naht: Die Absage ({@code VEROEFFENTLICHT -> ABGESAGT}) löst über das
 * Domänen-Event den Versand aus. Der {@code @Async}-Executor ist hier ein {@link SyncTaskExecutor},
 * damit die {@code AFTER_COMMIT}-Verarbeitung deterministisch (im selben Thread, ohne Timing) läuft
 * — geprüft wird das beobachtbare Ergebnis am Fake-Sender, nicht die Event-Mechanik.
 */
@DataJpaTest
@Import({
  SprechtagService.class,
  BuchungService.class,
  KlassenService.class,
  AbsageBenachrichtigungService.class,
  AbsageBenachrichtigungListener.class,
  FakeBenachrichtigungSender.class,
  AbsageVersandIntegrationTest.SyncAsyncConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AbsageVersandIntegrationTest extends AbstractServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 7, 20);

  @TestConfiguration
  @EnableAsync
  static class SyncAsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
      return new SyncTaskExecutor();
    }
  }

  @Autowired private FakeBenachrichtigungSender sender;

  @BeforeEach
  void resetSender() {
    sender.reset();
  }

  private record Fixture(Sprechtag sprechtag, Lehrauftrag lehrauftrag) {}

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
    return new Fixture(sprechtag, lehrauftrag);
  }

  private List<Termin> termineSorted() {
    return terminRepository.findAll().stream()
        .sorted(Comparator.comparing(Termin::getStartzeit))
        .toList();
  }

  private void book(Lehrauftrag auftrag, Termin termin, String email) {
    buchungService.buchen(
        new BuchungsAnfrage(
            "Eltern " + email,
            "Kind " + email,
            email,
            "n",
            List.of(new BuchungsWunsch(auftrag.getId(), termin.getId()))));
  }

  @Test
  void absage_afterCommit_notifiesActiveBookings() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = termineSorted();
    book(f.lehrauftrag(), slots.get(0), "a@example.com");
    book(f.lehrauftrag(), slots.get(1), "b@example.com");

    sprechtagService.changeStatus(f.sprechtag().getId(), SprechtagStatusEnum.ABGESAGT);

    assertThat(sender.empfangen)
        .extracting(AbsageEmpfaenger::email)
        .containsExactlyInAnyOrder("a@example.com", "b@example.com");
  }

  @Test
  void absage_withoutBookings_sendsNothingAndStands() {
    Fixture f = publishedSprechtag();

    sprechtagService.changeStatus(f.sprechtag().getId(), SprechtagStatusEnum.ABGESAGT);

    assertThat(sender.empfangen).isEmpty();
    assertThat(sprechtagRepository.findById(f.sprechtag().getId()).orElseThrow().getStatus())
        .isEqualTo(SprechtagStatusEnum.ABGESAGT);
  }

  @Test
  void absage_stands_evenWhenSendFails() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = termineSorted();
    book(f.lehrauftrag(), slots.get(0), "fehlerhaft@example.com");
    book(f.lehrauftrag(), slots.get(1), "ok@example.com");
    sender.scheitertFuer.add("fehlerhaft@example.com");

    sprechtagService.changeStatus(f.sprechtag().getId(), SprechtagStatusEnum.ABGESAGT);

    // Absage ist committet und bleibt, der Fehler bei einer Adresse stoppt die übrigen nicht.
    assertThat(sprechtagRepository.findById(f.sprechtag().getId()).orElseThrow().getStatus())
        .isEqualTo(SprechtagStatusEnum.ABGESAGT);
    assertThat(sender.empfangen)
        .extracting(AbsageEmpfaenger::email)
        .containsExactly("ok@example.com");
  }

  @Test
  void statusChangeOtherThanCancel_triggersNoSend() {
    Fixture f = publishedSprechtag();
    book(f.lehrauftrag(), termineSorted().get(0), "eltern@example.com");

    sprechtagService.changeStatus(f.sprechtag().getId(), SprechtagStatusEnum.ABGESCHLOSSEN);

    assertThat(sender.empfangen).isEmpty();
  }
}
