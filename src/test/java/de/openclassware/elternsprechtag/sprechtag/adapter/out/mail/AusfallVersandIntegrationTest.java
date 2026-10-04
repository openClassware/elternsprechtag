package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.Anmeldefrist;
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
 * End-to-End der Ausfall-Naht (Issue #156): Die Sammelaktion „Lehrkraft fällt aus" löst nach Commit
 * den Versand aus. Angetrieben über {@code entfallenLassen.entfallenLassen(...)}, beobachtet am
 * {@link BenachrichtigungSender}-Port — Event-Klasse, Listener-Verdrahtung und die interne
 * Gruppierung nach Familie bleiben dem Test bewusst unbekannt. Der {@code @Async}-Executor ist ein
 * {@link SyncTaskExecutor}, damit die {@code AFTER_COMMIT}-Verarbeitung ohne Timing beobachtbar ist.
 */
@ServiceTest
@Import({
  SprechtagKontextTestConfig.Kern.class,
  SprechtagKontextTestConfig.Ereigniseingang.class,
  MailVersandTestConfig.class,
  AusfallVersandIntegrationTest.SyncAsyncConfig.class
})
class AusfallVersandIntegrationTest extends AbstractServiceTest {

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

  private Fixture veroeffentlichterSprechtag() {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling",
            LocalDate.now().plusDays(7),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            15,
            SprechtagStatus.ENTWURF,
            klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, lehrauftrag);
  }

  private void buche(UUID lehrauftrag, String email, Termin... termine) {
    buche(lehrauftrag, "Karl Kind", email, termine);
  }

  private void buche(UUID lehrauftrag, String kind, String email, Termin... termine) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Elke Elternteil",
            kind,
            email,
            Arrays.stream(termine)
                .map(t -> new BuchungsWunsch(lehrauftrag, t.id().wert(), null))
                .toList()));
  }

  @Test
  void entfallenerTerminMitBuchung_afterCommit_sendetEineAusfallMail() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    buche(f.lehrauftrag(), "eltern@example.com", termin);
    sender.reset();

    entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(sender.empfangen).hasSize(1);
    Nachricht nachricht = sender.empfangen.get(0);
    assertThat(nachricht.empfaenger()).isEqualTo("eltern@example.com");
    assertThat(nachricht.betreff()).contains("Frühling");
    assertThat(nachricht.text())
        .contains("14:00", "Anna Berg", "Deutsch", "Karl Kind", "5a", "Gesamtschule Lindenhof");
  }

  /** Issue #109: Vor dem Anmeldeschluss führt die Mail über den Elternlink zurück in die Buchung. */
  @Test
  void vorDemAnmeldeschluss_traegtDieMailDenElternlink() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    buche(f.lehrauftrag(), "eltern@example.com", termin);
    sender.reset();

    entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    String link =
        "https://elternsprechtag.test/elternsprechtag/" + f.sprechtag().accessToken().wert();
    assertThat(sender.empfangen.get(0).text())
        .contains("selbst einen neuen Termin buchen:\n" + link + "\n\n")
        .contains(SCHULKONTAKT);
  }

  /**
   * Issue #109: Nach dem Anmeldeschluss führte der Link auf „Anmeldung beendet" — die Mail bleibt
   * beim Verweis auf die Schule.
   */
  @Test
  void nachDemAnmeldeschluss_bleibtDieMailOhneLink() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    buche(f.lehrauftrag(), "eltern@example.com", termin);
    Sprechtag geladen = ladeSprechtag(f.sprechtag().id().wert());
    geladen.aendereAnmeldefrist(Anmeldefrist.vonTagen(Anmeldefrist.HOECHSTENS_TAGE));
    sprechtage.speichere(geladen);
    sender.reset();

    entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(sender.empfangen.get(0).text())
        .doesNotContain("https://elternsprechtag.test", "neuen Termin buchen")
        .contains(SCHULKONTAKT);
  }

  @Test
  void zweiTermineDerselbenFamilie_ergebenGenauEineAusfallMail() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), "eltern@example.com", slots.get(0), slots.get(1));
    sender.reset();

    entfallenLassen.entfallenLassen(List.of(slots.get(0).id().wert(), slots.get(1).id().wert()));

    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text()).contains("14:00", "14:15");
  }

  /** ADR 0007: Geschwister oder Familien an der Stellvertreteradresse bekommen je eine eigene Mail. */
  @Test
  void zweiKinderAnEinerAdresse_ergebenJeKindEineAusfallMail() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), "Lena Müller", "sekretariat@schule.example", slots.get(0));
    buche(f.lehrauftrag(), "Ben Yilmaz", "sekretariat@schule.example", slots.get(1));
    sender.reset();

    entfallenLassen.entfallenLassen(List.of(slots.get(0).id().wert(), slots.get(1).id().wert()));

    assertThat(sender.empfangen).hasSize(2);
    assertThat(sender.empfangen.get(0).text())
        .contains("Kind: Lena Müller (Klasse 5a)", "14:00")
        .doesNotContain("Ben Yilmaz", "14:15");
    assertThat(sender.empfangen.get(1).text())
        .contains("Kind: Ben Yilmaz (Klasse 5a)", "14:15")
        .doesNotContain("Lena Müller", "14:00");
  }

  @Test
  void entfallenerTerminOhneBuchung_sendetNichts() {
    veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    sender.reset();

    entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(sender.versucht).isEmpty();
  }

  @Test
  void einAdressfehler_stopptDieUebrigenNicht() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), "boese@example.com", slots.get(0));
    buche(f.lehrauftrag(), "gut@example.com", slots.get(1));
    sender.reset();
    sender.scheitertFuer.add("boese@example.com");

    entfallenLassen.entfallenLassen(List.of(slots.get(0).id().wert(), slots.get(1).id().wert()));

    assertThat(sender.versucht).hasSize(2);
    assertThat(sender.empfangen).extracting(Nachricht::empfaenger).containsExactly("gut@example.com");
  }
}
