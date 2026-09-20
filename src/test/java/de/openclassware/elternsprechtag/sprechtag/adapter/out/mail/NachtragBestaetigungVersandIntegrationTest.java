package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
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
 * Dieselbe Bestätigungs-Naht wie {@link BuchungBestaetigungVersandIntegrationTest}, hier über den
 * Organizer-Nachtrag angestoßen: {@code Nachtragen#trageNach} veröffentlicht dasselbe gebündelte
 * {@code BuchungenBestaetigt}-Ereignis wie {@code Buchen#buchen}, weil beide über {@code
 * BuchungsVorgangService} laufen — der Versand-Listener kennt den Unterschied nicht.
 */
@ServiceTest
@Import({
  SprechtagKontextTestConfig.class,
  BuchungBestaetigungService.class,
  BuchungBestaetigungListener.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextConfig.class,
  NachtragBestaetigungVersandIntegrationTest.SyncAsyncConfig.class
})
class NachtragBestaetigungVersandIntegrationTest extends AbstractServiceTest {

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

  private Fixture publishedSprechtag(String ort) {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID fach = persistFach("Deutsch", "D");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, fach);
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", ort, DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, lehrauftrag);
  }

  private void trageNach(UUID auftrag, String email, String notiz, Termin... termine) {
    nachtragen.trageNach(
        new NachtragsAnfrage(
            "Elke Elternteil",
            "Karl Kind",
            email,
            Arrays.stream(termine)
                .map(t -> new NachtragsWunsch(auftrag, t.id().wert(), notiz))
                .toList()));
  }

  @Test
  void singleNachtrag_afterCommit_sendsOneConfirmation() {
    Fixture f = publishedSprechtag("Aula");
    trageNach(f.lehrauftrag(), "eltern@example.com", "Bitte pünktlich", alleTermine().get(0));

    assertThat(sender.empfangen).hasSize(1);
    Nachricht nachricht = sender.empfangen.get(0);
    assertThat(nachricht.empfaenger()).isEqualTo("eltern@example.com");
    assertThat(nachricht.betreff()).contains("Frühling", "20. Juli 2026");
    assertThat(nachricht.text())
        .contains("14:00", "Anna Berg", "Deutsch", "Karl Kind", "5a", "Aula", "Bitte pünktlich");
  }

  @Test
  void vierTermineInEinemNachtrag_ergebenGenauEineBestaetigung() {
    Fixture f = publishedSprechtag("Aula");
    List<Termin> slots = alleTermine();

    trageNach(
        f.lehrauftrag(), "eltern@example.com", null, slots.get(0), slots.get(1), slots.get(2),
        slots.get(3));

    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text()).contains("14:00", "14:15", "14:30", "14:45");
  }

  @Test
  void rollback_sendetKeineMail() {
    Fixture f = publishedSprechtag("Aula");
    Termin slot = alleTermine().get(0);
    trageNach(f.lehrauftrag(), "erste@example.com", null, slot);
    sender.reset();

    assertThatThrownBy(() -> trageNach(f.lehrauftrag(), "zweite@example.com", null, slot))
        .isInstanceOf(TerminBelegtException.class);

    assertThat(sender.versucht).isEmpty();
    assertThat(sender.empfangen).isEmpty();
  }
}
