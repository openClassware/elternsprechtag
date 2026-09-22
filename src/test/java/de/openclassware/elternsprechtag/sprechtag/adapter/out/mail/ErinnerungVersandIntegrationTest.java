package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungsVorlauf;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
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
 * End-to-End der Erinnerungs-Naht (Issue #107): Ein Lauf des Erinnerungs-Schedulers löst nach
 * Commit den Versand aus. Angetrieben über {@code erinnern.erinnere()}, beobachtet am
 * {@link BenachrichtigungSender}-Port — Event-Klasse, Listener-Verdrahtung und die interne
 * Gruppierung nach Familie bleiben dem Test bewusst unbekannt. Der {@code @Async}-Executor ist ein
 * {@link SyncTaskExecutor}, damit die {@code AFTER_COMMIT}-Verarbeitung ohne Timing beobachtbar ist.
 */
@ServiceTest
@Import({
  SprechtagKontextTestConfig.class,
  ErinnerungBenachrichtigungService.class,
  ErinnerungBenachrichtigungListener.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextConfig.class,
  ErinnerungVersandIntegrationTest.SyncAsyncConfig.class
})
class ErinnerungVersandIntegrationTest extends AbstractServiceTest {

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

  private Fixture veroeffentlichterSprechtag(String ort, LocalDate datum, ErinnerungsVorlauf vorlauf) {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling",
            ort,
            datum,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            15,
            SprechtagStatus.ENTWURF,
            vorlauf,
            klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, lehrauftrag);
  }

  private void buche(UUID lehrauftrag, String email, Termin... termine) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Elke Elternteil",
            "Karl Kind",
            email,
            Arrays.stream(termine)
                .map(t -> new BuchungsWunsch(lehrauftrag, t.id().wert(), null))
                .toList()));
  }

  @Test
  void faelligeBuchung_afterCommit_sendetEineErinnerung() {
    Fixture f = veroeffentlichterSprechtag("Aula", LocalDate.now().plusDays(1), ErinnerungsVorlauf.EIN_TAG);
    buche(f.lehrauftrag(), "eltern@example.com", alleTermine().get(0));
    sender.reset();

    erinnern.erinnere();

    assertThat(sender.empfangen).hasSize(1);
    Nachricht nachricht = sender.empfangen.get(0);
    assertThat(nachricht.empfaenger()).isEqualTo("eltern@example.com");
    assertThat(nachricht.betreff()).contains("Frühling");
    assertThat(nachricht.text())
        .contains("14:00", "Anna Berg", "Deutsch", "Karl Kind", "5a", "Aula", "Gesamtschule Lindenhof");
  }

  @Test
  void zweiTermineDerselbenFamilie_ergebenGenauEineErinnerung() {
    Fixture f = veroeffentlichterSprechtag("Aula", LocalDate.now().plusDays(1), ErinnerungsVorlauf.EIN_TAG);
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), "eltern@example.com", slots.get(0), slots.get(1));
    sender.reset();

    erinnern.erinnere();

    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text()).contains("14:00", "14:15");
  }

  @Test
  void nochNichtFaelligeBuchung_sendetNichts() {
    Fixture f = veroeffentlichterSprechtag("Aula", LocalDate.now().plusDays(2), ErinnerungsVorlauf.DREI_TAGE);
    buche(f.lehrauftrag(), "eltern@example.com", alleTermine().get(0));
    sender.reset();

    erinnern.erinnere();

    assertThat(sender.versucht).isEmpty();
  }

  @Test
  void zweiterLauf_erinnertDieselbeBuchungNichtNochEinmal() {
    Fixture f = veroeffentlichterSprechtag("Aula", LocalDate.now().plusDays(1), ErinnerungsVorlauf.EIN_TAG);
    buche(f.lehrauftrag(), "eltern@example.com", alleTermine().get(0));
    erinnern.erinnere();
    sender.reset();

    erinnern.erinnere();

    assertThat(sender.versucht).isEmpty();
  }
}
