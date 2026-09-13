package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.services.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@ServiceTest
@Import({

  SprechtagKontextTestConfig.class,

  AbsageBenachrichtigungService.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextConfig.class
})
class AbsageBenachrichtigungServiceTest extends AbstractServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 7, 20);

  @Autowired private AbsageBenachrichtigungService absageBenachrichtigungService;
  @Autowired private FakeBenachrichtigungSender sender;

  @BeforeEach
  void resetSender() {
    sender.reset();
  }

  /** Ein veröffentlichter Sprechtag mit einer Lehrkraft (4 materialisierte Slots). */
  private record Fixture(Sprechtag sprechtag, UUID lehrauftrag, UUID lehrkraft) {}

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
    return new Fixture(sprechtag, lehrauftrag, lehrkraft);
  }

  /** Bucht einen Slot mit gegebener Eltern-E-Mail und liefert die erzeugte Buchung. */
  private void book(UUID auftrag, Termin termin, String email) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern " + email,
            "Kind " + email,
            email,
            List.of(new BuchungsWunsch(auftrag, termin.id().wert(), "n"))));
  }

  @Test
  void benachrichtige_activeBookings_yieldOneRecipientEach() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = alleTermine();
    book(f.lehrauftrag(), slots.get(0), "a@example.com");
    book(f.lehrauftrag(), slots.get(1), "b@example.com");

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id().wert());

    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactlyInAnyOrder("a@example.com", "b@example.com");
  }

  @Test
  void benachrichtige_sameParentMultipleBookings_dedupedToOne() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = alleTermine();
    // Dieselbe Adresse an zwei Slots — darf nur eine Benachrichtigung erzeugen.
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern Müller",
            "Kind Müller",
            "mueller@example.com",
            List.of(
                new BuchungsWunsch(f.lehrauftrag(), slots.get(0).id().wert(), "n"),
                new BuchungsWunsch(f.lehrauftrag(), slots.get(1).id().wert(), "n"))));

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id().wert());

    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("mueller@example.com");
  }

  @Test
  void benachrichtige_cancelledBookingsExcluded_activeIncluded() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = alleTermine();
    book(f.lehrauftrag(), slots.get(0), "aktiv@example.com");
    book(f.lehrauftrag(), slots.get(1), "storniert@example.com");
    storniere(b -> b.familie().email().equals("storniert@example.com"));

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id().wert());

    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("aktiv@example.com");
  }

  @Test
  void benachrichtige_message_carriesBetreffUndText() {
    Fixture f = publishedSprechtag();
    book(f.lehrauftrag(), alleTermine().get(0), "eltern@example.com");

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id().wert());

    assertThat(sender.empfangen).hasSize(1);
    Nachricht nachricht = sender.empfangen.get(0);
    assertThat(nachricht.empfaenger()).isEqualTo("eltern@example.com");
    // Titel und Datum (zentral formatiert) im Betreff, Schulname als Absenderzeile im Text.
    assertThat(nachricht.betreff()).isEqualTo("Sprechtag „Frühling“ am 20. Juli 2026 abgesagt");
    assertThat(nachricht.text())
        .isEqualTo(
            """
            Guten Tag,

            der Sprechtag „Frühling“ am 20. Juli 2026 muss leider abgesagt werden.

            Ihr bereits gebuchter Termin entfällt damit. Bei Fragen wenden Sie sich bitte an die \
            Schule.

            So erreichen Sie die Schule:
            Sekretariat, Tel. 0123 456789

            Mit freundlichen Grüßen
            Gesamtschule Lindenhof""");
  }


  @Test
  void benachrichtige_sprechtagWithoutActiveBooking_sendsNothing() {
    Fixture f = publishedSprechtag();

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id().wert());

    assertThat(sender.empfangen).isEmpty();
  }

  @Test
  void benachrichtige_unknownSprechtag_sendsNothingAndDoesNotThrow() {
    assertThatCode(() -> absageBenachrichtigungService.benachrichtige(UUID.randomUUID()))
        .doesNotThrowAnyException();
    assertThat(sender.empfangen).isEmpty();
  }

  @Test
  void zaehleAktiveEmpfaenger_dedupedByEmail_excludesCancelled() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = alleTermine();
    book(f.lehrauftrag(), slots.get(0), "a@example.com");
    book(f.lehrauftrag(), slots.get(1), "a@example.com"); // dieselbe Adresse -> zählt einmal
    book(f.lehrauftrag(), slots.get(2), "b@example.com");
    storniere(b -> b.familie().email().equals("b@example.com"));

    // a@ (aktiv, dedupliziert) zählt; b@ (storniert) nicht.
    assertThat(absageBenachrichtigungService.zaehleAktiveEmpfaenger(f.sprechtag().id().wert()))
        .isEqualTo(1);
  }

  @Test
  void zaehleAktiveEmpfaenger_withoutActiveBooking_isZero() {
    Fixture f = publishedSprechtag();

    assertThat(absageBenachrichtigungService.zaehleAktiveEmpfaenger(f.sprechtag().id().wert()))
        .isZero();
  }

  @Test
  void benachrichtige_senderFailsForOneAddress_othersStillDelivered() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = alleTermine();
    book(f.lehrauftrag(), slots.get(0), "fehlerhaft@example.com");
    book(f.lehrauftrag(), slots.get(1), "ok@example.com");
    sender.scheitertFuer.add("fehlerhaft@example.com");

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id().wert());

    // Der Fehler bei der ersten Adresse stoppt den Versand an die übrigen nicht.
    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("ok@example.com");
  }
}
