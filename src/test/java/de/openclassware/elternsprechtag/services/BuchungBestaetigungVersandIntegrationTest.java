package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.services.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
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
 * End-to-End der Bestätigungs-Naht: Ein Eltern-Submit über {@link de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen#buchen} löst nach
 * Commit den Versand genau einer Bestätigungsmail aus. Angetrieben wird über den Service, beobachtet
 * am {@link BenachrichtigungSender}-Port — Event-Klassen, Listener-Verdrahtung und die interne
 * Aufteilung der Services bleiben dem Test bewusst unbekannt. Der {@code @Async}-Executor ist ein
 * {@link SyncTaskExecutor}, damit die {@code AFTER_COMMIT}-Verarbeitung ohne Timing beobachtbar ist.
 */
@ServiceTest
@Import({

  SprechtagKontextTestConfig.class,

  BuchungBestaetigungService.class,
  BuchungBestaetigungListener.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextConfig.class,
  BuchungBestaetigungVersandIntegrationTest.SyncAsyncConfig.class
})
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

  private void book(UUID auftrag, String email, String notiz, Termin... termine) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Elke Elternteil",
            "Karl Kind",
            email,
            Arrays.stream(termine)
                .map(t -> new BuchungsWunsch(auftrag, t.id().wert(), notiz))
                .toList()));
  }

  @Test
  void singleBooking_afterCommit_sendsOneConfirmation() {
    Fixture f = publishedSprechtag("Aula");
    book(f.lehrauftrag(), "eltern@example.com", "Bitte pünktlich", alleTermine().get(0));

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
    List<Termin> slots = alleTermine();
    // Bewusst in umgekehrter Reihenfolge gewünscht — die Mail muss trotzdem chronologisch listen.
    book(f.lehrauftrag(), "eltern@example.com", null, slots.get(2), slots.get(0));

    assertThat(sender.empfangen).hasSize(1);
    String text = sender.empfangen.get(0).text();
    assertThat(text).contains("14:00", "14:30");
    assertThat(text.indexOf("14:00")).isLessThan(text.indexOf("14:30"));
  }

  @Test
  void vierTermineInEinemSubmit_ergebenGenauEineBestaetigung() {
    Fixture f = publishedSprechtag("Aula");
    List<Termin> slots = alleTermine();

    book(
        f.lehrauftrag(),
        "eltern@example.com",
        null,
        slots.get(0),
        slots.get(1),
        slots.get(2),
        slots.get(3));

    // Der Punkt der Bündelung: vier Aggregate melden vier Ereignisse, der Use Case macht daraus
    // einen Vorgang — und die Familie bekommt eine Mail, nicht vier.
    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text()).contains("14:00", "14:15", "14:30", "14:45");
  }

  @Test
  void twoSubmits_sameAddress_sendTwoConfirmationsEachWithOwnSlots() {
    Fixture f = publishedSprechtag("Aula");
    List<Termin> slots = alleTermine();
    book(f.lehrauftrag(), "eltern@example.com", null, slots.get(0));
    book(f.lehrauftrag(), "eltern@example.com", null, slots.get(3));

    assertThat(sender.empfangen).hasSize(2);
    assertThat(sender.empfangen.get(0).text()).contains("14:00").doesNotContain("14:45");
    assertThat(sender.empfangen.get(1).text()).contains("14:45").doesNotContain("14:00");
  }

  @Test
  void withoutLocation_noEmptyLocationLine() {
    Fixture f = publishedSprechtag(null);
    book(f.lehrauftrag(), "eltern@example.com", null, alleTermine().get(0));

    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text()).doesNotContain("Ort:");
  }

  @Test
  void withSchulkontakt_appearsAsOwnParagraph() {
    Fixture f = publishedSprechtag("Aula");
    book(f.lehrauftrag(), "eltern@example.com", null, alleTermine().get(0));

    String text = sender.empfangen.get(0).text();
    // Eigener, beschrifteter Absatz zwischen Hinweis und Grußformel.
    assertThat(text).contains("\n\nSo erreichen Sie die Schule:\n" + SCHULKONTAKT + "\n\n");
  }

  @Test
  void withoutNote_noNoteLineAtAll() {
    Fixture f = publishedSprechtag("Aula");
    book(f.lehrauftrag(), "eltern@example.com", "   ", alleTermine().get(0));

    assertThat(sender.empfangen).hasSize(1);
    assertThat(sender.empfangen.get(0).text()).doesNotContain("Notiz");
  }

  @Test
  void noteBelongsToItsAppointment_indentedUnderIt() {
    Fixture f = publishedSprechtag("Aula");
    List<Termin> slots = alleTermine();
    // Zwei Termine, nur der zweite trägt eine Notiz.
    buchen.buchen(
        new BuchungsAnfrage(
            "Elke Elternteil",
            "Karl Kind",
            "eltern@example.com",
            List.of(
                new BuchungsWunsch(f.lehrauftrag(), slots.get(0).id().wert(), null),
                new BuchungsWunsch(
                    f.lehrauftrag(), slots.get(1).id().wert(), "Bitte über Mathe sprechen"))));

    assertThat(sender.empfangen).hasSize(1);
    String text = sender.empfangen.get(0).text();
    // Der Termin ohne Notiz bleibt einzeilig, die Notiz steht eingerückt unter ihrem Termin.
    assertThat(text)
        .contains(
            "- 14:00 Uhr, Anna Berg (Deutsch)\n"
                + "- 14:15 Uhr, Anna Berg (Deutsch)\n"
                + "  Notiz: Bitte über Mathe sprechen\n");
  }

  @Test
  void bookingStands_evenWhenSendFails() {
    Fixture f = publishedSprechtag("Aula");
    sender.scheitertFuer.add("eltern@example.com");

    book(f.lehrauftrag(), "eltern@example.com", null, alleTermine().get(0));

    // Versendet wurde versucht, die Zustellung scheiterte — die Buchung steht trotzdem.
    assertThat(sender.versucht).hasSize(1);
    assertThat(sender.empfangen).isEmpty();
    assertThat(alleBuchungen()).hasSize(1);
  }

  @Test
  void failedBooking_onTakenSlot_triggersNoSend() {
    Fixture f = publishedSprechtag("Aula");
    Termin slot = alleTermine().get(0);
    book(f.lehrauftrag(), "erste@example.com", null, slot);
    sender.reset();

    assertThatThrownBy(() -> book(f.lehrauftrag(), "zweite@example.com", null, slot))
        .isInstanceOf(TerminBelegtException.class);

    assertThat(sender.versucht).isEmpty();
  }

  @Test
  void submitWithoutSlots_triggersNoSend() {
    publishedSprechtag("Aula");

    buchen.buchen(
        new BuchungsAnfrage("Elke", "Karl", "eltern@example.com", List.of()));

    assertThat(sender.versucht).isEmpty();
  }
}
