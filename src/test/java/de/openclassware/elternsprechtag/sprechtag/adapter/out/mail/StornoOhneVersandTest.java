package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
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
 * Das Storno schweigt — und zwar beobachtet, nicht behauptet.
 *
 * <p>Beide Versand-Nähte sind verdrahtet und der {@code @Async}-Executor ist synchron; käme nach dem
 * Commit irgendeine Nachricht, stünde sie am Fake-Sender. Der Anlass eines Stornos ist praktisch
 * immer der Anruf der Familie: Beim Tippfehler in der Adresse ginge eine Mail erneut an einen
 * Dritten, beim Löschverlangen wäre sie widersinnig (`ABDECKUNG.md`, Phase 3; ADR 0002).
 */
@ServiceTest
@Import({
  SprechtagKontextTestConfig.class,
  BuchungBestaetigungService.class,
  BuchungBestaetigungListener.class,
  AbsageBenachrichtigungService.class,
  AbsageBenachrichtigungListener.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextConfig.class,
  StornoOhneVersandTest.SyncAsyncConfig.class
})
class StornoOhneVersandTest extends AbstractServiceTest {

  private static final LocalDate DATUM = LocalDate.of(2026, 7, 20);

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

  @Test
  void storniere_verschicktNichts() {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling",
            DATUM,
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            15,
            SprechtagStatus.ENTWURF,
            klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    Termin termin = alleTermine().get(0);
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern Müller",
            "Lukas Müller",
            "eltern.mueller@example.com",
            List.of(new BuchungsWunsch(lehrauftrag, termin.id().wert(), null))));
    // Die Bestätigungsmail der Buchung ist nicht Gegenstand dieses Tests.
    sender.reset();
    UUID buchung = termine.lade(termin.id()).orElseThrow().aktiveBuchung().orElseThrow().id().wert();

    stornieren.storniere(buchung);

    assertThat(sender.versucht).isEmpty();
    assertThat(sender.empfangen).isEmpty();
  }
}
