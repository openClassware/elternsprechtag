package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
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
 * End-to-End der Ausfall-Naht (Issue #158): Ein Ausfall-Vorgang löst nach Commit den Versand aus.
 * Angetrieben über denselben Use-Case-Port wie in #157 ({@code entfallenLassen.lassEntfallen}),
 * beobachtet am {@link BenachrichtigungSender}-Port — Ereignisklasse, Listener-Verdrahtung und die
 * interne Gruppierung nach Adresse bleiben dem Test bewusst unbekannt. Der {@code @Async}-Executor
 * ist ein {@link SyncTaskExecutor}, damit die {@code AFTER_COMMIT}-Verarbeitung ohne Timing
 * beobachtbar ist.
 */
@ServiceTest
@Import({
  SprechtagKontextTestConfig.class,
  AusfallBenachrichtigungService.class,
  AusfallBenachrichtigungListener.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextConfig.class,
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
            "Aula",
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
    bucheFuer(lehrauftrag, email, "Karl Kind", termine);
  }

  private void bucheFuer(UUID lehrauftrag, String email, String kind, Termin... termine) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Elke Elternteil",
            kind,
            email,
            Arrays.stream(termine)
                .map(t -> new BuchungsWunsch(lehrauftrag, t.id().wert(), null))
                .toList()));
  }

  private static List<UUID> ids(Termin... termine) {
    return Arrays.stream(termine).map(t -> t.id().wert()).toList();
  }

  private Nachricht nachrichtAn(String empfaenger) {
    return sender.empfangen.stream()
        .filter(nachricht -> nachricht.empfaenger().equals(empfaenger))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void entfallenerTermin_afterCommit_benachrichtigtDieFamilie() {
    Fixture f = veroeffentlichterSprechtag();
    Termin slot = alleTermine().get(0);
    buche(f.lehrauftrag(), "eltern@example.com", slot);
    sender.reset();

    entfallenLassen.lassEntfallen(ids(slot));

    assertThat(sender.empfangen).hasSize(1);
    Nachricht nachricht = sender.empfangen.get(0);
    assertThat(nachricht.empfaenger()).isEqualTo("eltern@example.com");
    // Betreff und Text nennen Sprechtag und Datum, die Position Uhrzeit, Lehrkraft und Fach.
    String datum = Formats.dateLong(LocalDate.now().plusDays(7));
    assertThat(nachricht.betreff()).contains("Frühling", datum);
    assertThat(nachricht.text()).contains("Frühling", datum, "14:00", "Anna Berg", "Deutsch");
    // Der Schulkontakt steht als eigener Absatz, unmittelbar gefolgt von der Grußformel.
    assertThat(nachricht.text())
        .contains(SCHULKONTAKT + "\n\nMit freundlichen Grüßen\nGesamtschule Lindenhof");
  }

  @Test
  void ausfallMail_enthaeltWederZugangsLinkNochOrt() {
    Fixture f = veroeffentlichterSprechtag();
    Termin slot = alleTermine().get(0);
    buche(f.lehrauftrag(), "eltern@example.com", slot);
    sender.reset();

    entfallenLassen.lassEntfallen(ids(slot));

    String text = sender.empfangen.get(0).text();
    // Keine Aktion: Der Zugangs-Link ist #109 und bleibt hier draußen …
    assertThat(text).doesNotContain(f.sprechtag().accessToken().wert()).doesNotContain("http");
    // … und der Ort ist für einen Termin, der nicht stattfindet, gegenstandslos.
    assertThat(text).doesNotContain("Aula");
  }

  @Test
  void zweiTermineDerselbenFamilie_ergebenGenauEineNachricht() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), "eltern@example.com", slots.get(0), slots.get(1));
    sender.reset();

    entfallenLassen.lassEntfallen(ids(slots.get(0), slots.get(1)));

    // Eine Nachricht, zwei Positionen — nicht zwei fast gleiche Mails. Je Position Uhrzeit,
    // Lehrkraft und Fach.
    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text())
        .contains("- 14:00 Uhr, Anna Berg (Deutsch)")
        .contains("- 14:15 Uhr, Anna Berg (Deutsch)");
  }

  @Test
  void zweiGeschwisterUnterEinerAdresse_ergebenEineNachrichtOhneKindZeile() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    bucheFuer(f.lehrauftrag(), "eltern@example.com", "Karl Kind", slots.get(0));
    bucheFuer(f.lehrauftrag(), "eltern@example.com", "Klara Kind", slots.get(1));
    sender.reset();

    entfallenLassen.lassEntfallen(ids(slots.get(0), slots.get(1)));

    // Gebündelt wird über die Adresse, und die teilen sich Geschwister. Eine aus der ersten Zeile
    // abgeleitete Kopfzeile nennte genau eines der beiden Kinder, während darunter die Termine
    // beider stehen — deshalb trägt diese Mail als einzige keine Kind-Zeile.
    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text())
        .doesNotContain("Karl Kind", "Klara Kind")
        .contains("14:00", "14:15");
  }

  @Test
  void zweiFamilien_bekommenJeEineEigeneNachricht() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), "mueller@example.com", slots.get(0));
    buche(f.lehrauftrag(), "schmidt@example.com", slots.get(1));
    sender.reset();

    entfallenLassen.lassEntfallen(ids(slots.get(0), slots.get(1)));

    assertThat(sender.empfangen).hasSize(2);
    assertThat(sender.empfangen).extracting(Nachricht::empfaenger)
        .containsExactlyInAnyOrder("mueller@example.com", "schmidt@example.com");
    // Jede Nachricht nennt nur den eigenen Termin — die Gruppierung trennt die Familien.
    assertThat(nachrichtAn("mueller@example.com").text()).contains("14:00").doesNotContain("14:15");
    assertThat(nachrichtAn("schmidt@example.com").text()).contains("14:15").doesNotContain("14:00");
  }

  @Test
  void nurFreieTermine_sendenNichts() {
    veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    sender.reset();

    entfallenLassen.lassEntfallen(ids(slots.get(0), slots.get(1)));

    // Kein Empfänger, kein Fehler: Ein freier Slot betrifft niemanden.
    assertThat(sender.versucht).isEmpty();
  }

  @Test
  void einScheiternderEmpfaenger_stopptDieUebrigenNicht() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), "kaputt@example.com", slots.get(0));
    buche(f.lehrauftrag(), "gesund@example.com", slots.get(1));
    sender.reset();
    sender.scheitertFuer.add("kaputt@example.com");

    entfallenLassen.lassEntfallen(ids(slots.get(0), slots.get(1)));

    assertThat(sender.versucht).hasSize(2);
    assertThat(sender.empfangen).extracting(Nachricht::empfaenger)
        .containsExactly("gesund@example.com");
  }
}
