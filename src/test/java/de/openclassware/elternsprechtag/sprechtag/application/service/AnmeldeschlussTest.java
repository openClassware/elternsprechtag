package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Nachtragen.NachtragsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.SprechtagFormular;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Veroeffentlichen;
import de.openclassware.elternsprechtag.sprechtag.domain.Anmeldefrist;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungenBestaetigt;
import de.openclassware.elternsprechtag.sprechtag.domain.ElternbuchungGeschlossenException;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Die Anmeldefrist an den Use Cases (Issue #122) — nur, <em>dass</em> die Regel an der richtigen
 * Stelle greift. Die Regel selbst ({@code nimmtElternbuchungenAn}) steht ohne Spring in {@code
 * SprechtagTest}.
 *
 * <p>Ohne {@code Clock}: Die Daten stehen relativ zu {@link LocalDate#now()} — „Sprechtag morgen,
 * Frist 2 Tage" ist eine abgelaufene Frist, an jedem Tag, an dem der Test läuft.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
@RecordApplicationEvents
class AnmeldeschlussTest extends AbstractServiceTest {

  @Autowired private ApplicationEvents events;

  private static final LocalDate MORGEN = LocalDate.now().plusDays(1);

  private record Fixture(Sprechtag sprechtag, UUID lehrauftrag) {}

  /** Ein Sprechtag morgen, als Entwurf mit der gegebenen Frist gespeichert, aber noch ohne Termine. */
  private Fixture entwurf(int fristTage) {
    UUID klasse = persistKlasse("5a");
    UUID lehrkraft = persistLehrkraft("Anna", "Berg", "BER");
    UUID lehrauftrag = persistLehrauftrag(lehrkraft, klasse, persistFach("Deutsch", "D"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", MORGEN, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    Sprechtag geladen = ladeSprechtag(sprechtag.id().wert());
    geladen.aendereAnmeldefrist(Anmeldefrist.vonTagen(fristTage));
    sprechtage.speichere(geladen);
    return new Fixture(geladen, lehrauftrag);
  }

  private Fixture veroeffentlicht(int fristTage) {
    Fixture f = entwurf(fristTage);
    veroeffentlichen.veroeffentliche(f.sprechtag().id().wert());
    return f;
  }

  private static BuchungsAnfrage buchung(UUID lehrauftrag, Termin termin) {
    return new BuchungsAnfrage(
        "Eltern Müller",
        "Kind Müller",
        "mueller@example.com",
        List.of(new BuchungsWunsch(lehrauftrag, termin.id().wert(), null)));
  }

  // --- Elternlink: Buchen und Zugang -------------------------------------------------------

  @Test
  void buchen_vorDemAnmeldeschluss_gehtDurch() {
    Fixture f = veroeffentlicht(1);

    assertThat(buchen.buchen(buchung(f.lehrauftrag(), alleTermine().get(0)))).isEqualTo(1);
  }

  @Test
  void buchen_nachDemAnmeldeschluss_wirdAbgewiesenOhneBuchungUndOhneEreignis() {
    Fixture f = veroeffentlicht(2);
    Termin termin = alleTermine().get(0);

    assertThatThrownBy(() -> buchen.buchen(buchung(f.lehrauftrag(), termin)))
        .isInstanceOf(ElternbuchungGeschlossenException.class);

    assertThat(alleBuchungen()).isEmpty();
    assertThat(events.stream(BuchungenBestaetigt.class)).isEmpty();
  }

  /** Die Seite war noch offen, als der Sprechtag abgesagt wurde. */
  @Test
  void buchen_anEinemAbgesagtenSprechtag_wirdAbgewiesen() {
    Fixture f = veroeffentlicht(1);
    Termin termin = alleTermine().get(0);
    absagen.sageAb(f.sprechtag().id().wert());

    assertThatThrownBy(() -> buchen.buchen(buchung(f.lehrauftrag(), termin)))
        .isInstanceOf(ElternbuchungGeschlossenException.class);

    assertThat(alleBuchungen()).isEmpty();
  }

  @Test
  void zugang_nachDemAnmeldeschluss_istNichtBuchbar() {
    Fixture f = veroeffentlicht(2);

    assertThat(sprechtagszugang.oeffne(f.sprechtag().accessToken().wert()).orElseThrow().buchbar())
        .isFalse();
  }

  @Test
  void zugang_vorDemAnmeldeschluss_istBuchbar() {
    Fixture f = veroeffentlicht(1);

    assertThat(sprechtagszugang.oeffne(f.sprechtag().accessToken().wert()).orElseThrow().buchbar())
        .isTrue();
  }

  // --- Organizer-Strecke -------------------------------------------------------------------

  /** Die Frist schließt nur den Elternlink — Anrufe am Tag selbst nimmt der Organizer an. */
  @Test
  void nachtragen_nachDemAnmeldeschluss_gehtDurch() {
    Fixture f = veroeffentlicht(2);
    Termin termin = alleTermine().get(0);

    int gebucht =
        nachtragen.trageNach(
            new NachtragsAnfrage(
                "Eltern Müller",
                "Kind Müller",
                "mueller@example.com",
                List.of(new NachtragsWunsch(f.lehrauftrag(), termin.id().wert(), null))));

    assertThat(gebucht).isEqualTo(1);
  }

  @Test
  void veroeffentlichen_mitAbgelaufenerFrist_veroeffentlichtUndMeldetEs() {
    Fixture f = entwurf(2);

    Veroeffentlichen.Ergebnis ergebnis = veroeffentlichen.veroeffentliche(f.sprechtag().id().wert());

    assertThat(ergebnis.anmeldungBereitsBeendet()).isTrue();
    assertThat(ladeSprechtag(f.sprechtag().id().wert()).status())
        .isEqualTo(SprechtagStatus.VEROEFFENTLICHT);
  }

  @Test
  void veroeffentlichen_mitOffenerFrist_meldetNichts() {
    Fixture f = entwurf(1);

    assertThat(veroeffentlichen.veroeffentliche(f.sprechtag().id().wert()).anmeldungBereitsBeendet())
        .isFalse();
  }

  // --- Formular: hin und zurück ------------------------------------------------------------

  private SprechtagFormular formular(Integer fristTage) {
    SprechtagFormular formular = new SprechtagFormular();
    formular.setTitel("Frühling");
    formular.setDatum(MORGEN);
    formular.setBeginn(LocalTime.of(14, 0));
    formular.setEnde(LocalTime.of(15, 0));
    formular.setSlotInMinuten(15);
    formular.setSchulkontakt(SCHULKONTAKT);
    formular.setKlasseIds(java.util.Set.of(persistKlasse("5a")));
    formular.setAnmeldefristTage(fristTage);
    return formular;
  }

  @Test
  void einNeuesFormularSchlaegtDenVortagVor() {
    assertThat(new SprechtagFormular().getAnmeldefristTage()).isEqualTo(1);
  }

  @Test
  void anlegen_speichertDieFristUndDasFormularLiefertSieZurueck() {
    UUID id = anlegen.lege(formular(4));

    assertThat(ladeSprechtag(id).anmeldefrist()).isEqualTo(Anmeldefrist.vonTagen(4));
    assertThat(bearbeiten.ladeFormular(id).orElseThrow().getAnmeldefristTage()).isEqualTo(4);
  }

  @Test
  void bearbeiten_aendertDieFristAuchNachDemVeroeffentlichen() {
    Fixture f = veroeffentlicht(2);
    UUID id = f.sprechtag().id().wert();
    SprechtagFormular geladen = bearbeiten.ladeFormular(id).orElseThrow();
    geladen.setAnmeldefristTage(0);

    bearbeiten.bearbeite(id, geladen);

    assertThat(ladeSprechtag(id).anmeldefrist()).isEqualTo(Anmeldefrist.vonTagen(0));
    assertThat(sprechtagszugang.oeffne(f.sprechtag().accessToken().wert()).orElseThrow().buchbar())
        .as("die abgelaufene Frist ist wieder offen")
        .isTrue();
  }

  @Test
  void anlegen_mitFristAusserhalbDesBereichs_wirdNichtGespeichert() {
    assertThatThrownBy(() -> anlegen.lege(formular(29)))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(jdbc.queryForObject("select count(*) from sprechtage", Long.class)).isZero();
  }

  @Test
  void duplizieren_uebernimmtDieFrist() {
    UUID original = anlegen.lege(formular(4));

    UUID kopie = duplizieren.dupliziere(original);

    assertThat(bearbeiten.ladeFormular(kopie).orElseThrow().getAnmeldefristTage()).isEqualTo(4);
  }
}
