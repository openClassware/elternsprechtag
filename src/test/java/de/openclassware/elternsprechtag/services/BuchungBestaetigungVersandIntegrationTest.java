package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.domain.Fach;
import de.openclassware.elternsprechtag.domain.Klasse;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.domain.Termin;
import de.openclassware.elternsprechtag.services.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsAnfrage;
import de.openclassware.elternsprechtag.services.BuchungService.BuchungsWunsch;
import de.openclassware.elternsprechtag.services.BuchungService.TerminBelegtException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
 * End-to-End der Bestätigungs-Naht: Ein Eltern-Submit über {@link BuchungService#buchen} löst nach
 * Commit den Versand genau einer Bestätigungsmail aus. Angetrieben wird über den Service, beobachtet
 * am {@link BenachrichtigungSender}-Port — Event-Klassen, Listener-Verdrahtung und die interne
 * Aufteilung der Services bleiben dem Test bewusst unbekannt. Der {@code @Async}-Executor ist ein
 * {@link SyncTaskExecutor}, damit die {@code AFTER_COMMIT}-Verarbeitung ohne Timing beobachtbar ist.
 */
@DataJpaTest
@Import({
  SprechtagService.class,
  BuchungService.class,
  KlassenService.class,
  BuchungBestaetigungService.class,
  BuchungBestaetigungListener.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextConfig.class,
  BuchungBestaetigungVersandIntegrationTest.SyncAsyncConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BuchungBestaetigungVersandIntegrationTest extends AbstractServiceTest {

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

  private Fixture publishedSprechtag(String ort) {
    Klasse klasse = persistKlasse("5a");
    Lehrer lehrer = persistLehrer("Anna", "Berg", "BER");
    Fach fach = persistFach("Deutsch", "D");
    Lehrauftrag lehrauftrag = persistLehrauftrag(lehrer, klasse, fach);
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatusEnum.ENTWURF, klasse);
    sprechtag.setLocation(ort);
    sprechtagRepository.save(sprechtag);
    sprechtagService.changeStatus(sprechtag.getId(), SprechtagStatusEnum.VEROEFFENTLICHT);
    return new Fixture(sprechtag, lehrauftrag);
  }

  private List<Termin> termineSorted() {
    return terminRepository.findAll().stream()
        .sorted(Comparator.comparing(Termin::getStartzeit))
        .toList();
  }

  private void book(Lehrauftrag auftrag, String email, String notiz, Termin... termine) {
    buchungService.buchen(
        new BuchungsAnfrage(
            "Elke Elternteil",
            "Karl Kind",
            email,
            notiz,
            Arrays.stream(termine)
                .map(t -> new BuchungsWunsch(auftrag.getId(), t.getId()))
                .toList()));
  }

  @Test
  void singleBooking_afterCommit_sendsOneConfirmation() {
    Fixture f = publishedSprechtag("Aula");
    book(f.lehrauftrag(), "eltern@example.com", "Bitte pünktlich", termineSorted().get(0));

    assertThat(sender.empfangen).hasSize(1);
    Nachricht nachricht = sender.empfangen.get(0);
    assertThat(nachricht.empfaenger()).isEqualTo("eltern@example.com");
    assertThat(nachricht.betreff()).contains("Frühling", "20. Juli 2026");
    assertThat(nachricht.text())
        .contains(
            "14:00", "Anna Berg", "Deutsch", "Karl Kind", "5a", "Aula", "Bitte pünktlich",
            "Gesamtschule Lindenhof");
  }

  @Test
  void multipleSlots_inOneSubmit_sendOneChronologicalConfirmation() {
    Fixture f = publishedSprechtag("Aula");
    List<Termin> slots = termineSorted();
    // Bewusst in umgekehrter Reihenfolge gewünscht — die Mail muss trotzdem chronologisch listen.
    book(f.lehrauftrag(), "eltern@example.com", null, slots.get(2), slots.get(0));

    assertThat(sender.empfangen).hasSize(1);
    String text = sender.empfangen.get(0).text();
    assertThat(text).contains("14:00", "14:30");
    assertThat(text.indexOf("14:00")).isLessThan(text.indexOf("14:30"));
  }

  @Test
  void twoSubmits_sameAddress_sendTwoConfirmationsEachWithOwnSlots() {
    Fixture f = publishedSprechtag("Aula");
    List<Termin> slots = termineSorted();
    book(f.lehrauftrag(), "eltern@example.com", null, slots.get(0));
    book(f.lehrauftrag(), "eltern@example.com", null, slots.get(3));

    assertThat(sender.empfangen).hasSize(2);
    assertThat(sender.empfangen.get(0).text()).contains("14:00").doesNotContain("14:45");
    assertThat(sender.empfangen.get(1).text()).contains("14:45").doesNotContain("14:00");
  }

  @Test
  void withoutLocation_noEmptyLocationLine() {
    Fixture f = publishedSprechtag(null);
    book(f.lehrauftrag(), "eltern@example.com", null, termineSorted().get(0));

    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text()).doesNotContain("Ort:");
  }

  @Test
  void withoutNote_noEmptyNoteSection() {
    Fixture f = publishedSprechtag("Aula");
    book(f.lehrauftrag(), "eltern@example.com", "   ", termineSorted().get(0));

    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text()).doesNotContain("Ihre Notiz");
  }

  @Test
  void bookingStands_evenWhenSendFails() {
    Fixture f = publishedSprechtag("Aula");
    sender.scheitertFuer.add("eltern@example.com");

    book(f.lehrauftrag(), "eltern@example.com", null, termineSorted().get(0));

    // Versendet wurde versucht, die Zustellung scheiterte — die Buchung steht trotzdem.
    assertThat(sender.versucht).hasSize(1);
    assertThat(sender.empfangen).isEmpty();
    assertThat(buchungRepository.findAll()).hasSize(1);
  }

  @Test
  void failedBooking_onTakenSlot_triggersNoSend() {
    Fixture f = publishedSprechtag("Aula");
    Termin slot = termineSorted().get(0);
    book(f.lehrauftrag(), "erste@example.com", null, slot);
    sender.reset();

    assertThatThrownBy(() -> book(f.lehrauftrag(), "zweite@example.com", null, slot))
        .isInstanceOf(TerminBelegtException.class);

    assertThat(sender.versucht).isEmpty();
  }

  @Test
  void submitWithoutSlots_triggersNoSend() {
    publishedSprechtag("Aula");

    buchungService.buchen(
        new BuchungsAnfrage("Elke", "Karl", "eltern@example.com", null, List.of()));

    assertThat(sender.versucht).isEmpty();
  }
}
