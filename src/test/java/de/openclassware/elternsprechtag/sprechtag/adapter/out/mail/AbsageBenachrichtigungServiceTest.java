package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen.Versand;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellergebnis;
import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
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
@Import({SprechtagKontextTestConfig.Kern.class, MailVersandTestConfig.class})
class AbsageBenachrichtigungServiceTest extends AbstractServiceTest {

  // In der Zukunft: Der Elternlink bucht nur bis zum Anmeldeschluss (Issue #122).
  private static final LocalDate DATE = LocalDate.of(2099, 7, 20);

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

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id());

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

    List<Versand> versand = absageBenachrichtigungService.benachrichtige(f.sprechtag().id());

    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("mueller@example.com");
    // Issue #110: Die eine Nachricht trägt beide Buchungen — ihr Ergebnis gilt für jede davon.
    assertThat(versand).singleElement().satisfies(
        nachricht -> {
          assertThat(nachricht.buchungen())
              .containsExactlyInAnyOrderElementsOf(
                  alleBuchungen().stream().map(Buchung::id).toList());
          assertThat(nachricht.ergebnis()).isEqualTo(Zustellergebnis.ABGESCHICKT);
        });
  }

  @Test
  void benachrichtige_cancelledBookingsExcluded_activeIncluded() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = alleTermine();
    book(f.lehrauftrag(), slots.get(0), "aktiv@example.com");
    book(f.lehrauftrag(), slots.get(1), "storniert@example.com");
    storniere(b -> b.familie().email().equals("storniert@example.com"));

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id());

    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("aktiv@example.com");
  }

  @Test
  void benachrichtige_message_carriesBetreffUndText() {
    Fixture f = publishedSprechtag();
    book(f.lehrauftrag(), alleTermine().get(0), "eltern@example.com");

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id());

    assertThat(sender.empfangen).hasSize(1);
    Nachricht nachricht = sender.empfangen.get(0);
    assertThat(nachricht.empfaenger()).isEqualTo("eltern@example.com");
    // Titel und Datum (zentral formatiert) im Betreff, Schulname als Absenderzeile im Text.
    assertThat(nachricht.betreff()).isEqualTo("Sprechtag „Frühling“ am 20. Juli 2099 abgesagt");
    assertThat(nachricht.text())
        .isEqualTo(
            """
            Guten Tag,

            der Sprechtag „Frühling“ am 20. Juli 2099 muss leider abgesagt werden.

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

    absageBenachrichtigungService.benachrichtige(f.sprechtag().id());

    assertThat(sender.empfangen).isEmpty();
  }

  @Test
  void benachrichtige_unknownSprechtag_sendsNothingAndDoesNotThrow() {
    assertThatCode(() -> absageBenachrichtigungService.benachrichtige(SprechtagId.neu()))
        .doesNotThrowAnyException();
    assertThat(sender.empfangen).isEmpty();
  }

  @Test
  void benachrichtige_senderFailsForOneAddress_othersStillDelivered() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = alleTermine();
    book(f.lehrauftrag(), slots.get(0), "fehlerhaft@example.com");
    book(f.lehrauftrag(), slots.get(1), "ok@example.com");
    sender.scheitertFuer.add("fehlerhaft@example.com");

    List<Versand> versand = absageBenachrichtigungService.benachrichtige(f.sprechtag().id());

    // Der Fehler bei der ersten Adresse stoppt den Versand an die übrigen nicht.
    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("ok@example.com");
    // Issue #110: Der Fehlschlag bleibt nicht im Log, er kommt als Ergebnis zurück.
    assertThat(versand)
        .extracting(Versand::buchungen, Versand::ergebnis)
        .containsExactlyInAnyOrder(
            tuple(List.of(buchungVon("fehlerhaft@example.com")), Zustellergebnis.FEHLGESCHLAGEN),
            tuple(List.of(buchungVon("ok@example.com")), Zustellergebnis.ABGESCHICKT));
  }

  private BuchungId buchungVon(String email) {
    return alleBuchungen().stream()
        .filter(b -> b.familie().email().equals(email))
        .map(Buchung::id)
        .findFirst()
        .orElseThrow();
  }
}
