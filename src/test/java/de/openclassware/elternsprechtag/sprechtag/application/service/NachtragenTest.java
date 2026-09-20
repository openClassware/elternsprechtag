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
import de.openclassware.elternsprechtag.sprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagNichtVeroeffentlichtException;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import de.openclassware.elternsprechtag.sprechtag.domain.TerminBelegtException;
import de.openclassware.elternsprechtag.sprechtag.domain.ZeitkonfliktException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Der Organizer-Nachtrag gegen eine echte Datenbank — Nachfolger-Gegenstück zu
 * {@link BuchenUndAuswertenTest}: fachlich derselbe Vorgang, nur über den eigenen Port {@code
 * Nachtragen} angestoßen. Die Kernregeln (alles-oder-nichts, Zeitkonflikt, Rollback) sind dort
 * bereits erschöpfend geprüft, weil sie aus der geteilten {@link BuchungsVorgangService} kommen;
 * hier steht nur, was den Nachtrag von ihm unterscheidet.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class NachtragenTest extends AbstractServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 7, 20);

  private record Fixture(Sprechtag sprechtag, UUID klasse, UUID lehrauftrag, UUID lehrkraft) {}

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
    return new Fixture(sprechtag, klasse, lehrauftrag, lehrkraft);
  }

  private NachtragsAnfrage anfrage(UUID lehrauftrag, Termin... termine) {
    List<NachtragsWunsch> wuensche =
        Arrays.stream(termine)
            .map(t -> new NachtragsWunsch(lehrauftrag, t.id().wert(), "Bitte pünktlich"))
            .toList();
    return new NachtragsAnfrage(
        "Eltern Müller", "Kind Müller", "eltern.mueller@example.com", wuensche);
  }

  @Test
  void trageNach_happyPath_createsBuchungAndMarksTerminBelegt() {
    Fixture f = publishedSprechtag();
    Termin termin = alleTermine().get(0);

    int gebucht = nachtragen.trageNach(anfrage(f.lehrauftrag(), termin));

    assertThat(gebucht).isEqualTo(1);
    assertThat(alleBuchungen()).hasSize(1);
    Buchung persisted = alleBuchungen().get(0);
    assertThat(persisted.istAktiv()).isTrue();
    assertThat(persisted.familie().elternName()).isEqualTo("Eltern Müller");
    assertThat(termine.lade(termin.id()).orElseThrow().istBuchbar()).isFalse();
  }

  @Test
  void trageNach_multipleWuensche_allOrNothing() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = alleTermine();

    int gebucht = nachtragen.trageNach(anfrage(f.lehrauftrag(), frei.get(0), frei.get(1)));

    assertThat(gebucht).isEqualTo(2);
    assertThat(alleBuchungen()).hasSize(2);
  }

  @Test
  void trageNach_slotAlreadyBelegt_throwsAndPersistsNothing() {
    Fixture f = publishedSprechtag();
    Termin termin = alleTermine().get(0);
    nachtragen.trageNach(anfrage(f.lehrauftrag(), termin));

    Termin veraltet = termine.lade(termin.id()).orElseThrow();
    assertThatThrownBy(() -> nachtragen.trageNach(anfrage(f.lehrauftrag(), veraltet)))
        .isInstanceOf(TerminBelegtException.class);
    assertThat(alleBuchungen()).hasSize(1);
  }

  @Test
  void trageNach_allOrNothing_rollsBackEarlierSlotWhenLaterSlotTaken() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = alleTermine();
    Termin first = frei.get(0);
    Termin second = frei.get(1);
    // Zweiter Slot wird zwischenzeitlich vergeben — von einer anderen Familie.
    nachtragen.trageNach(
        new NachtragsAnfrage(
            "Eltern Schmidt",
            "Kind Schmidt",
            "schmidt@example.com",
            List.of(new NachtragsWunsch(f.lehrauftrag(), second.id().wert(), null))));

    assertThatThrownBy(() -> nachtragen.trageNach(anfrage(f.lehrauftrag(), first, second)))
        .isInstanceOf(TerminBelegtException.class);

    // Kronjuwel: der bereits verarbeitete erste Slot wurde zurückgerollt, keine Buchung übrig.
    assertThat(termine.lade(first.id()).orElseThrow().istBuchbar())
        .as("erster Slot muss nach Rollback wieder buchbar sein")
        .isTrue();
    assertThat(alleBuchungen()).hasSize(1);
  }

  @Test
  void trageNach_zweiWuenscheZurSelbenUhrzeit_wirdAbgewiesen() {
    UUID klasse = persistKlasse("5a");
    UUID berg = persistLehrkraft("Anna", "Berg", "BER");
    UUID adler = persistLehrkraft("Carl", "Adler", "ADL");
    UUID bergAuftrag = persistLehrauftrag(berg, klasse, persistFach("Deutsch", "D"));
    UUID adlerAuftrag = persistLehrauftrag(adler, klasse, persistFach("Mathe", "M"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    Termin bergUmVierzehnUhr = termineVon(berg).get(0);
    Termin adlerUmVierzehnUhr = termineVon(adler).get(0);

    assertThatThrownBy(
            () ->
                nachtragen.trageNach(
                    new NachtragsAnfrage(
                        "Eltern Müller",
                        "Kind Müller",
                        "eltern.mueller@example.com",
                        List.of(
                            new NachtragsWunsch(bergAuftrag, bergUmVierzehnUhr.id().wert(), null),
                            new NachtragsWunsch(
                                adlerAuftrag, adlerUmVierzehnUhr.id().wert(), null)))))
        .isInstanceOf(ZeitkonfliktException.class);

    assertThat(alleBuchungen()).isEmpty();
  }

  @Test
  void trageNach_amAbgeschlossenenSprechtag_wirdAbgewiesen() {
    Fixture f = publishedSprechtag();
    Termin termin = alleTermine().get(0);
    abschliessen.schliesseAb(f.sprechtag().id().wert());

    // Am Service vorbei an der Oberfläche: Die Route ist per URL für jeden Status erreichbar.
    assertThatThrownBy(() -> nachtragen.trageNach(anfrage(f.lehrauftrag(), termin)))
        .isInstanceOf(SprechtagNichtVeroeffentlichtException.class);

    assertThat(alleBuchungen()).isEmpty();
  }

  @Test
  void trageNach_amAbgesagtenSprechtag_wirdAbgewiesen() {
    Fixture f = publishedSprechtag();
    Termin termin = alleTermine().get(0);
    absagen.sageAb(f.sprechtag().id().wert());

    assertThatThrownBy(() -> nachtragen.trageNach(anfrage(f.lehrauftrag(), termin)))
        .isInstanceOf(SprechtagNichtVeroeffentlichtException.class);

    assertThat(alleBuchungen()).isEmpty();
  }

  @Test
  void trageNach_amAbgeschlossenenSprechtagMitZeitkonflikt_meldetDenVeroeffentlichungsfehler() {
    UUID klasse = persistKlasse("5a");
    UUID berg = persistLehrkraft("Anna", "Berg", "BER");
    UUID adler = persistLehrkraft("Carl", "Adler", "ADL");
    UUID bergAuftrag = persistLehrauftrag(berg, klasse, persistFach("Deutsch", "D"));
    UUID adlerAuftrag = persistLehrauftrag(adler, klasse, persistFach("Mathe", "M"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Frühling", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    Termin bergUmVierzehnUhr = termineVon(berg).get(0);
    Termin adlerUmVierzehnUhr = termineVon(adler).get(0);
    abschliessen.schliesseAb(sprechtag.id().wert());

    // Beides trifft zu — Zeitkonflikt und nicht veröffentlicht. Der grundsätzlichere Fehler muss
    // gewinnen, sonst würde das Beheben des Zeitkonflikts den Nachtrag nicht durchlassen.
    assertThatThrownBy(
            () ->
                nachtragen.trageNach(
                    new NachtragsAnfrage(
                        "Eltern Müller",
                        "Kind Müller",
                        "eltern.mueller@example.com",
                        List.of(
                            new NachtragsWunsch(bergAuftrag, bergUmVierzehnUhr.id().wert(), null),
                            new NachtragsWunsch(
                                adlerAuftrag, adlerUmVierzehnUhr.id().wert(), null)))))
        .isInstanceOf(SprechtagNichtVeroeffentlichtException.class);

    assertThat(alleBuchungen()).isEmpty();
  }

  @Test
  void buchenUndNachtragen_nutzenDenselbenZeitkonflikt_amGleichenSprechtag() {
    Fixture f = publishedSprechtag();
    List<Termin> frei = alleTermine();
    // Ein Eltern-Submit belegt den ersten Slot …
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern Schmidt",
            "Kind Schmidt",
            "schmidt@example.com",
            List.of(new BuchungsWunsch(f.lehrauftrag(), frei.get(0).id().wert(), null))));

    // … der Organizer-Nachtrag sieht denselben belegten Slot und wird ebenso abgewiesen.
    Termin belegt = termine.lade(frei.get(0).id()).orElseThrow();
    assertThatThrownBy(() -> nachtragen.trageNach(anfrage(f.lehrauftrag(), belegt)))
        .isInstanceOf(TerminBelegtException.class);
  }
}
