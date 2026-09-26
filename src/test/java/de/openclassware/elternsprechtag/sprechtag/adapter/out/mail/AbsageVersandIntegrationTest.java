package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * End-to-End der Absage-Naht: Die Absage ({@code VEROEFFENTLICHT -> ABGESAGT}) löst über das
 * Domänen-Event den Versand aus. Der {@code @Async}-Executor ist hier ein {@link SyncTaskExecutor},
 * damit die {@code AFTER_COMMIT}-Verarbeitung deterministisch (im selben Thread, ohne Timing) läuft
 * — geprüft wird das beobachtbare Ergebnis am Fake-Sender, nicht die Event-Mechanik.
 */
@ServiceTest
@Import({

  SprechtagKontextTestConfig.class,

  AbsageBenachrichtigungService.class,
  AbsageBenachrichtigungListener.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextConfig.class,
  AbsageVersandIntegrationTest.SyncAsyncConfig.class
})
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

  private record Fixture(Sprechtag sprechtag, UUID lehrauftrag) {}

  private Fixture publishedSprechtag() {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID fach = persistFach("Deutsch", "D");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, fach);
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, lehrauftrag);
  }

  private void book(UUID auftrag, Termin termin, String email) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern " + email,
            "Kind " + email,
            email,
            List.of(new BuchungsWunsch(auftrag, termin.id().wert(), "n"))));
  }

  @Test
  void absage_afterCommit_notifiesActiveBookings() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = alleTermine();
    book(f.lehrauftrag(), slots.get(0), "a@example.com");
    book(f.lehrauftrag(), slots.get(1), "b@example.com");

    absagen.sageAb(f.sprechtag().id().wert());

    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactlyInAnyOrder("a@example.com", "b@example.com");
    // Über die ganze Naht hinweg trägt jede Nachricht die fertige Absage-Mail.
    assertThat(sender.empfangen)
        .allSatisfy(
            nachricht -> {
              assertThat(nachricht.betreff())
                  .isEqualTo("Sprechtag „Frühling“ am 20. Juli 2026 abgesagt");
              assertThat(nachricht.text())
                  .contains("Frühling", "20. Juli 2026", "Gesamtschule Lindenhof");
            });
  }

  @Test
  void absage_withoutBookings_sendsNothingAndStands() {
    Fixture f = publishedSprechtag();

    absagen.sageAb(f.sprechtag().id().wert());

    assertThat(sender.empfangen).isEmpty();
    assertThat(ladeSprechtag(f.sprechtag().id().wert()).status())
        .isEqualTo(SprechtagStatus.ABGESAGT);
  }

  @Test
  void absage_stands_evenWhenSendFails() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = alleTermine();
    book(f.lehrauftrag(), slots.get(0), "fehlerhaft@example.com");
    book(f.lehrauftrag(), slots.get(1), "ok@example.com");
    sender.scheitertFuer.add("fehlerhaft@example.com");

    absagen.sageAb(f.sprechtag().id().wert());

    // Absage ist committet und bleibt, der Fehler bei einer Adresse stoppt die übrigen nicht.
    assertThat(ladeSprechtag(f.sprechtag().id().wert()).status())
        .isEqualTo(SprechtagStatus.ABGESAGT);
    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("ok@example.com");
  }

  @Test
  void abschlussDurchDenTageslauf_triggersNoSend() {
    Fixture f = publishedSprechtag();
    book(f.lehrauftrag(), alleTermine().get(0), "eltern@example.com");

    abschliessen.schliesseVorbeiAb();

    assertThat(sender.empfangen).isEmpty();
  }
}
