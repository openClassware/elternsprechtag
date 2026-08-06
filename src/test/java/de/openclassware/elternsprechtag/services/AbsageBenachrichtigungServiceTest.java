package de.openclassware.elternsprechtag.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import de.openclassware.elternsprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.domain.BuchungStatusEnum;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
  SprechtagService.class,
  BuchungService.class,
  KlassenService.class,
  AbsageBenachrichtigungService.class,
  FakeBenachrichtigungSender.class,
  BenachrichtigungTextTestConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AbsageBenachrichtigungServiceTest extends AbstractServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 7, 20);

  @Autowired private AbsageBenachrichtigungService absageBenachrichtigungService;
  @Autowired private FakeBenachrichtigungSender sender;

  @Value("${elternsprechtag.schoolname}")
  private String schulname;

  @BeforeEach
  void resetSender() {
    sender.reset();
  }

  /** Ein veröffentlichter Sprechtag mit einer Lehrkraft (4 materialisierte Slots). */
  private record Fixture(Sprechtag sprechtag, Lehrauftrag lehrauftrag, Lehrer lehrer) {}

  private Fixture publishedSprechtag() {
    Klasse klasse = persistKlasse("5a");
    Lehrer lehrer = persistLehrer("Anna", "Berg", "BER");
    Fach fach = persistFach("Deutsch", "D");
    Lehrauftrag lehrauftrag = persistLehrauftrag(lehrer, klasse, fach);
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatusEnum.ENTWURF, klasse);
    sprechtagService.changeStatus(sprechtag.getId(), SprechtagStatusEnum.VEROEFFENTLICHT);
    return new Fixture(sprechtag, lehrauftrag, lehrer);
  }

  private List<Termin> termineSorted() {
    return terminRepository.findAll().stream()
        .sorted(Comparator.comparing(Termin::getStartzeit))
        .toList();
  }

  /** Bucht einen Slot mit gegebener Eltern-E-Mail und liefert die erzeugte Buchung. */
  private void book(Lehrauftrag auftrag, Termin termin, String email) {
    buchungService.buchen(
        new BuchungsAnfrage(
            "Eltern " + email,
            "Kind " + email,
            email,
            "n",
            List.of(new BuchungsWunsch(auftrag.getId(), termin.getId()))));
  }

  @Test
  void benachrichtige_activeBookings_yieldOneRecipientEach() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = termineSorted();
    book(f.lehrauftrag(), slots.get(0), "a@example.com");
    book(f.lehrauftrag(), slots.get(1), "b@example.com");

    absageBenachrichtigungService.benachrichtige(f.sprechtag().getId());

    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactlyInAnyOrder("a@example.com", "b@example.com");
  }

  @Test
  void benachrichtige_sameParentMultipleBookings_dedupedToOne() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = termineSorted();
    // Dieselbe Adresse an zwei Slots — darf nur eine Benachrichtigung erzeugen.
    buchungService.buchen(
        new BuchungsAnfrage(
            "Eltern Müller",
            "Kind Müller",
            "mueller@example.com",
            "n",
            List.of(
                new BuchungsWunsch(f.lehrauftrag().getId(), slots.get(0).getId()),
                new BuchungsWunsch(f.lehrauftrag().getId(), slots.get(1).getId()))));

    absageBenachrichtigungService.benachrichtige(f.sprechtag().getId());

    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("mueller@example.com");
  }

  @Test
  void benachrichtige_cancelledBookingsExcluded_activeIncluded() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = termineSorted();
    book(f.lehrauftrag(), slots.get(0), "aktiv@example.com");
    book(f.lehrauftrag(), slots.get(1), "storniert@example.com");
    Buchung storniert =
        buchungRepository.findAll().stream()
            .filter(b -> b.getElternEmail().equals("storniert@example.com"))
            .findFirst()
            .orElseThrow();
    storniert.setStatus(BuchungStatusEnum.ABGESAGT);
    buchungRepository.save(storniert);

    absageBenachrichtigungService.benachrichtige(f.sprechtag().getId());

    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("aktiv@example.com");
  }

  @Test
  void benachrichtige_message_carriesSubjectAndBodyFromTranslations() {
    Fixture f = publishedSprechtag();
    book(f.lehrauftrag(), termineSorted().get(0), "eltern@example.com");

    absageBenachrichtigungService.benachrichtige(f.sprechtag().getId());

    assertThat(sender.empfangen).hasSize(1);
    Nachricht nachricht = sender.empfangen.get(0);
    assertThat(nachricht.empfaenger()).isEqualTo("eltern@example.com");
    // Titel und Datum (zentral formatiert) im Betreff, Schulname als Absender-Grußformel im Text.
    assertThat(nachricht.betreff()).isEqualTo("Sprechtag „Frühling“ am 20. Juli 2026 abgesagt");
    assertThat(nachricht.text())
        .contains("Frühling", "20. Juli 2026", "abgesagt")
        .endsWith(schulname);
  }

  @Test
  void benachrichtige_sprechtagWithoutActiveBooking_sendsNothing() {
    Fixture f = publishedSprechtag();

    absageBenachrichtigungService.benachrichtige(f.sprechtag().getId());

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
    List<Termin> slots = termineSorted();
    book(f.lehrauftrag(), slots.get(0), "a@example.com");
    book(f.lehrauftrag(), slots.get(1), "a@example.com"); // dieselbe Adresse -> zählt einmal
    book(f.lehrauftrag(), slots.get(2), "b@example.com");
    Buchung storniert =
        buchungRepository.findAll().stream()
            .filter(b -> b.getElternEmail().equals("b@example.com"))
            .findFirst()
            .orElseThrow();
    storniert.setStatus(BuchungStatusEnum.ABGESAGT);
    buchungRepository.save(storniert);

    // a@ (aktiv, dedupliziert) zählt; b@ (storniert) nicht.
    assertThat(absageBenachrichtigungService.zaehleAktiveEmpfaenger(f.sprechtag().getId()))
        .isEqualTo(1);
  }

  @Test
  void zaehleAktiveEmpfaenger_withoutActiveBooking_isZero() {
    Fixture f = publishedSprechtag();

    assertThat(absageBenachrichtigungService.zaehleAktiveEmpfaenger(f.sprechtag().getId()))
        .isZero();
  }

  @Test
  void benachrichtige_senderFailsForOneAddress_othersStillDelivered() {
    Fixture f = publishedSprechtag();
    List<Termin> slots = termineSorted();
    book(f.lehrauftrag(), slots.get(0), "fehlerhaft@example.com");
    book(f.lehrauftrag(), slots.get(1), "ok@example.com");
    sender.scheitertFuer.add("fehlerhaft@example.com");

    absageBenachrichtigungService.benachrichtige(f.sprechtag().getId());

    // Der Fehler bei der ersten Adresse stoppt den Versand an die übrigen nicht.
    assertThat(sender.empfangen)
        .extracting(Nachricht::empfaenger)
        .containsExactly("ok@example.com");
  }
}
