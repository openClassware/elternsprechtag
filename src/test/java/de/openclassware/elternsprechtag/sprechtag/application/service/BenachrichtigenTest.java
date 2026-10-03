package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.FakeBenachrichtigungen;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.NichtErreicht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungsVorlauf;
import de.openclassware.elternsprechtag.sprechtag.domain.Mailart;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Der Zustellzustand (Issue #110) über den ganzen Weg: Ein Vorgang wird festgeschrieben, sein
 * Ereignis stößt nach dem Commit den Versand an, der Use Case hält den Ausgang fest — und Auswertung
 * wie Übersicht zeigen, wen die Nachricht nicht erreicht hat.
 *
 * <p>Der Versand selbst ist die {@link FakeBenachrichtigungen Attrappe}: Sie bündelt je Adresse wie
 * der echte und lässt einzelne Adressen scheitern. Formuliert wird hier nichts; das prüfen die
 * Versand-Tests unter {@code adapter.out.mail}. Die feineren Regeln der Liste (Storno, Anonymisierung,
 * Ersetzen) stehen im Persistenztest der Leseseite.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class BenachrichtigenTest extends AbstractServiceTest {

  // In der Zukunft: Der Elternlink bucht nur bis zum Anmeldeschluss (Issue #122).
  private static final LocalDate DATUM = LocalDate.of(2099, 7, 20);

  @Autowired private FakeBenachrichtigungen versand;

  @BeforeEach
  void resetVersand() {
    versand.reset();
  }

  private record Fixture(Sprechtag sprechtag, UUID lehrauftrag) {}

  private Fixture veroeffentlichterSprechtag(LocalDate datum, ErinnerungsVorlauf vorlauf) {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling",
            null,
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

  private Fixture veroeffentlichterSprechtag() {
    return veroeffentlichterSprechtag(DATUM, ErinnerungsVorlauf.KEINE);
  }

  private void buche(UUID lehrauftrag, Termin termin, String kind, String email) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern " + kind,
            kind,
            email,
            List.of(new BuchungsWunsch(lehrauftrag, termin.id().wert(), null))));
  }

  private List<NichtErreicht> nichtErreicht(Fixture f) {
    return auswerten.werteAus(f.sprechtag().id().wert()).orElseThrow().nichtErreicht();
  }

  private int hinweisInDerUebersicht(Fixture f) {
    return sprechtagsuebersicht.alle().stream()
        .filter(zeile -> zeile.id().equals(f.sprechtag().id().wert()))
        .findFirst()
        .orElseThrow()
        .nichtErreicht();
  }

  @Test
  void gescheiterteBestaetigung_stehtInDerAuswertung_undZaehltInDerUebersicht() {
    Fixture f = veroeffentlichterSprechtag();
    versand.scheitertFuer.add("mueler@exmaple.com");

    buche(f.lehrauftrag(), alleTermine().get(0), "Lena Müller", "mueler@exmaple.com");

    assertThat(nichtErreicht(f))
        .singleElement()
        .satisfies(
            eintrag -> {
              assertThat(eintrag.art()).isEqualTo(Mailart.BESTAETIGUNG);
              assertThat(eintrag.email()).isEqualTo("mueler@exmaple.com");
              assertThat(eintrag.schuelerNamen()).containsExactly("Lena Müller");
              assertThat(eintrag.elternNamen()).containsExactly("Eltern Lena Müller");
            });
    assertThat(hinweisInDerUebersicht(f)).isEqualTo(1);
  }

  @Test
  void zugestellteBestaetigung_hinterlaesstKeinenEintrag() {
    Fixture f = veroeffentlichterSprechtag();

    buche(f.lehrauftrag(), alleTermine().get(0), "Lena Müller", "mueller@example.com");

    assertThat(nichtErreicht(f)).isEmpty();
    assertThat(hinweisInDerUebersicht(f)).isZero();
  }

  @Test
  void gescheiterteAbsage_anGeteilteAdresse_isteinEintragMitBeidenKindern() {
    Fixture f = veroeffentlichterSprechtag();
    List<Termin> slots = alleTermine();
    buche(f.lehrauftrag(), slots.get(0), "Lena Müller", "mueller@example.com");
    buche(f.lehrauftrag(), slots.get(1), "Tom Müller", "mueller@example.com");
    buche(f.lehrauftrag(), slots.get(2), "Kai Schmidt", "schmidt@example.com");
    versand.scheitertFuer.add("mueller@example.com");

    absagen.sageAb(f.sprechtag().id().wert());

    assertThat(nichtErreicht(f))
        .singleElement()
        .satisfies(
            eintrag -> {
              assertThat(eintrag.art()).isEqualTo(Mailart.ABSAGE);
              assertThat(eintrag.schuelerNamen()).containsExactly("Lena Müller", "Tom Müller");
            });
    assertThat(hinweisInDerUebersicht(f)).isEqualTo(1);
  }

  @Test
  void gescheiterteAusfallNachricht_bleibtSichtbar_obwohlDieBuchungEntfallenIst() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    buche(f.lehrauftrag(), termin, "Lena Müller", "mueller@example.com");
    versand.scheitertFuer.add("mueller@example.com");

    entfallenLassen.entfallenLassen(List.of(termin.id().wert()));

    assertThat(nichtErreicht(f)).extracting(NichtErreicht::art).containsExactly(Mailart.AUSFALL);
  }

  @Test
  void entfernteAngaben_nehmenDenEintragAusDerListe() {
    Fixture f = veroeffentlichterSprechtag();
    Termin termin = alleTermine().get(0);
    buche(f.lehrauftrag(), termin, "Lena Müller", "mueller@example.com");
    versand.scheitertFuer.add("mueller@example.com");
    entfallenLassen.entfallenLassen(List.of(termin.id().wert()));
    Buchung entfallen = alleBuchungen().get(0);

    angabenEntfernen.entferne(entfallen.id().wert());

    assertThat(nichtErreicht(f)).isEmpty();
  }

  /** Die Vormerkung am Aggregat bleibt der Schutz gegen Doppelversand — auch beim Fehlschlag. */
  @Test
  void gescheiterteErinnerung_stehtInDerListe_undWirdNichtWiederholt() {
    Fixture f =
        veroeffentlichterSprechtag(LocalDate.now().plusDays(1), ErinnerungsVorlauf.EIN_TAG);
    buche(f.lehrauftrag(), alleTermine().get(0), "Lena Müller", "mueller@example.com");
    versand.scheitertFuer.add("mueller@example.com");

    int erinnert = erinnern.erinnere();
    int zweiterLauf = erinnern.erinnere();

    assertThat(erinnert).isEqualTo(1);
    assertThat(zweiterLauf).isZero();
    assertThat(nichtErreicht(f)).extracting(NichtErreicht::art).containsExactly(Mailart.ERINNERUNG);
  }
}
