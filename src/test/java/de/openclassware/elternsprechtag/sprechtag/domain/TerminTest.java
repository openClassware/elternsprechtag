package de.openclassware.elternsprechtag.sprechtag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Die Kernregel <em>ein Slot, höchstens eine aktive Buchung</em> — ohne Spring-Kontext und ohne
 * Datenbank. Dass das geht, ist der eigentliche Gewinn des Aggregats: Die wichtigste Regel der
 * Anwendung hing bisher an einem {@code @SpringBootTest} mit laufender Postgres.
 */
class TerminTest {

  private static final LocalDateTime JETZT = LocalDateTime.of(2026, 7, 20, 14, 0);
  private static final String EMAIL = "anonym@schule.example";

  private final LehrkraftId lehrkraft = LehrkraftId.neu();
  private final SprechtagId sprechtag = SprechtagId.neu();

  private Termin freierTermin() {
    return Termin.neu(
        sprechtag, lehrkraft, new Zeitraum(JETZT, JETZT.plusMinutes(15)));
  }

  private Familie familie(String name) {
    return new Familie("Eltern " + name, "Kind " + name, name + "@example.com");
  }

  private Buchungsziel ziel() {
    return new Buchungsziel(
        LehrauftragId.neu(), lehrkraft, "Anna Berg", "BER", "5a", "Deutsch");
  }

  @Test
  void neuerTermin_istBuchbar() {
    assertThat(freierTermin().istBuchbar()).isTrue();
  }

  @Test
  void buche_legtAktiveBuchungAn() {
    Termin termin = freierTermin();

    BuchungId id = termin.buche(familie("mueller"), ziel(), new Notiz("Anliegen"), JETZT);

    assertThat(termin.aktiveBuchung()).isPresent();
    assertThat(termin.aktiveBuchung().orElseThrow().id()).isEqualTo(id);
    assertThat(termin.istBuchbar()).isFalse();
  }

  @Test
  void buche_zweiteBuchung_wirdAbgewiesen() {
    Termin termin = freierTermin();
    termin.buche(familie("mueller"), ziel(), null, JETZT);

    assertThatThrownBy(() -> termin.buche(familie("schmidt"), ziel(), null, JETZT))
        .isInstanceOf(TerminBelegtException.class);
    assertThat(termin.buchungen()).hasSize(1);
  }

  @Test
  void buche_nachStorno_wiederMoeglich() {
    Termin termin = freierTermin();
    BuchungId erste = termin.buche(familie("mueller"), ziel(), null, JETZT);
    termin.storniere(erste);

    assertThat(termin.istBuchbar()).isTrue();
    BuchungId zweite = termin.buche(familie("schmidt"), ziel(), null, JETZT);

    // Die stornierte Buchung bleibt liegen — der Slot trägt über die Zeit mehrere, aktiv ist eine.
    assertThat(termin.buchungen()).hasSize(2);
    assertThat(termin.aktiveBuchung().orElseThrow().id()).isEqualTo(zweite);
  }

  @Test
  void buche_entfallenerTermin_wirdAbgewiesen() {
    Termin termin = freierTermin();
    termin.lassEntfallen();

    assertThatThrownBy(() -> termin.buche(familie("mueller"), ziel(), null, JETZT))
        .isInstanceOf(TerminEntfaelltException.class);
    assertThat(termin.istBuchbar()).isFalse();
  }

  @Test
  void lassEntfallen_mitAktiverBuchung_storniertSieUndMeldetBeideEreignisse() {
    Termin termin = freierTermin();
    BuchungId buchung = termin.buche(familie("mueller"), ziel(), null, JETZT);
    termin.ereignisseAbholen();

    boolean geaendert = termin.lassEntfallen();

    assertThat(geaendert).isTrue();
    assertThat(termin.verfuegbarkeit()).isEqualTo(Verfuegbarkeit.ENTFAELLT);
    assertThat(termin.istBuchbar()).isFalse();
    assertThat(termin.buchungen())
        .singleElement()
        .satisfies(b -> assertThat(b.status()).isEqualTo(Buchungsstatus.STORNIERT));
    assertThat(termin.ereignisseAbholen())
        .containsExactly(
            new BuchungStorniert(termin.id(), buchung), new TerminEntfallen(termin.id()));
  }

  @Test
  void lassEntfallen_ohneBuchung_meldetNurTerminEntfallen() {
    Termin termin = freierTermin();

    boolean geaendert = termin.lassEntfallen();

    assertThat(geaendert).isTrue();
    assertThat(termin.verfuegbarkeit()).isEqualTo(Verfuegbarkeit.ENTFAELLT);
    assertThat(termin.ereignisseAbholen()).containsExactly(new TerminEntfallen(termin.id()));
  }

  @Test
  void lassEntfallen_bereitsEntfallen_meldetKeineAenderungMehr() {
    Termin termin = freierTermin();
    termin.lassEntfallen();
    termin.ereignisseAbholen();

    boolean geaendert = termin.lassEntfallen();

    assertThat(geaendert).isFalse();
    assertThat(termin.ereignisseAbholen()).isEmpty();
  }

  @Test
  void lassEntfallen_nachStorno_istWiederMoeglich() {
    Termin termin = freierTermin();
    BuchungId buchung = termin.buche(familie("mueller"), ziel(), null, JETZT);
    termin.storniere(buchung);

    termin.lassEntfallen();

    assertThat(termin.verfuegbarkeit()).isEqualTo(Verfuegbarkeit.ENTFAELLT);
    assertThat(termin.istBuchbar()).isFalse();
  }

  @Test
  void buche_zielMitFremderLehrkraft_wirdAbgewiesen() {
    Termin termin = freierTermin();
    Buchungsziel fremd =
        new Buchungsziel(
            LehrauftragId.neu(), LehrkraftId.neu(), "Bob Klein", "KLE", "5a", "Mathe");

    assertThatThrownBy(() -> termin.buche(familie("mueller"), fremd, null, JETZT))
        .isInstanceOf(FremdeLehrkraftException.class)
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buche_meldetBuchungAngelegt() {
    Termin termin = freierTermin();

    BuchungId id = termin.buche(familie("mueller"), ziel(), null, JETZT);

    assertThat(termin.ereignisseAbholen())
        .containsExactly(new BuchungAngelegt(termin.id(), id));
  }

  @Test
  void ereignisseAbholen_leertDenPuffer() {
    Termin termin = freierTermin();
    termin.buche(familie("mueller"), ziel(), null, JETZT);

    assertThat(termin.ereignisseAbholen()).hasSize(1);
    // Zweimal abholen darf nicht zweimal bestätigen.
    assertThat(termin.ereignisseAbholen()).isEmpty();
  }

  @Test
  void storniere_meldetBuchungStorniert_nurBeimErstenMal() {
    Termin termin = freierTermin();
    BuchungId id = termin.buche(familie("mueller"), ziel(), null, JETZT);
    termin.ereignisseAbholen();

    termin.storniere(id);
    assertThat(termin.ereignisseAbholen()).containsExactly(new BuchungStorniert(termin.id(), id));

    termin.storniere(id);
    assertThat(termin.ereignisseAbholen()).isEmpty();
  }

  @Test
  void storniere_fremdeBuchung_wirdAbgewiesen() {
    Termin termin = freierTermin();

    assertThatThrownBy(() -> termin.storniere(BuchungId.neu()))
        .isInstanceOf(BuchungNichtGefundenException.class);
  }

  @Test
  void buchungen_sindNachAussenNichtVeraenderbar() {
    Termin termin = freierTermin();
    termin.buche(familie("mueller"), ziel(), null, JETZT);

    assertThatThrownBy(() -> termin.buchungen().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void notiz_laengerAls500Zeichen_wirdAbgewiesen() {
    assertThatThrownBy(() -> new Notiz("x".repeat(Notiz.MAX_ZEICHEN + 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void notiz_ausLeeremText_istKeineNotiz() {
    assertThat(Notiz.vielleicht("   ")).isEmpty();
    assertThat(Notiz.vielleicht(null)).isEmpty();
    assertThat(Notiz.vielleicht("  Anliegen  ")).contains(new Notiz("Anliegen"));
  }

  @Test
  void familie_ohneEmail_wirdAbgewiesen() {
    assertThatThrownBy(() -> new Familie("Eltern", "Kind", "  "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void zeitraum_ohneDauer_wirdAbgewiesen() {
    assertThatThrownBy(() -> new Zeitraum(JETZT, JETZT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void erinnereBuchung_markiertDieAktiveBuchungUndMeldetEineAenderung() {
    Termin termin = freierTermin();
    BuchungId id = termin.buche(familie("mueller"), ziel(), null, JETZT);

    boolean geaendert = termin.erinnereBuchung(id, JETZT.plusDays(2));

    assertThat(geaendert).isTrue();
    assertThat(termin.aktiveBuchung().orElseThrow().erinnerungVersendetAm())
        .contains(JETZT.plusDays(2));
  }

  @Test
  void erinnereBuchung_meldetBuchungErinnert_nurBeimErstenMal() {
    Termin termin = freierTermin();
    BuchungId id = termin.buche(familie("mueller"), ziel(), null, JETZT);
    termin.ereignisseAbholen();

    termin.erinnereBuchung(id, JETZT.plusDays(2));
    assertThat(termin.ereignisseAbholen())
        .containsExactly(new BuchungErinnert(termin.id(), id));

    termin.erinnereBuchung(id, JETZT.plusDays(3));
    assertThat(termin.ereignisseAbholen()).isEmpty();
  }

  @Test
  void erinnereBuchung_zweitesMal_meldetKeineAenderungMehr() {
    Termin termin = freierTermin();
    BuchungId id = termin.buche(familie("mueller"), ziel(), null, JETZT);
    termin.erinnereBuchung(id, JETZT.plusDays(2));

    boolean geaendert = termin.erinnereBuchung(id, JETZT.plusDays(3));

    assertThat(geaendert).isFalse();
    // Der erste Zeitstempel bleibt stehen — kein zweiter Versand überschreibt ihn.
    assertThat(termin.aktiveBuchung().orElseThrow().erinnerungVersendetAm())
        .contains(JETZT.plusDays(2));
  }

  @Test
  void erinnereBuchung_stornierteBuchung_meldetKeineAenderung() {
    Termin termin = freierTermin();
    BuchungId id = termin.buche(familie("mueller"), ziel(), null, JETZT);
    termin.storniere(id);

    boolean geaendert = termin.erinnereBuchung(id, JETZT.plusDays(2));

    assertThat(geaendert).isFalse();
    assertThat(termin.buchungen()).singleElement()
        .satisfies(b -> assertThat(b.erinnerungVersendetAm()).isEmpty());
  }

  @Test
  void erinnereBuchung_fremdeBuchung_wirdAbgewiesen() {
    Termin termin = freierTermin();

    assertThatThrownBy(() -> termin.erinnereBuchung(BuchungId.neu(), JETZT))
        .isInstanceOf(BuchungNichtGefundenException.class);
  }

  /** Issue #126: alle Buchungen, auch stornierte — die Familie weicht einem Pseudonym. */
  @Test
  void anonymisiere_ersetztFamilieUndLeertNotiz_auchBeiStornierten() {
    Termin termin = freierTermin();
    BuchungId storniert = termin.buche(familie("mueller"), ziel(), new Notiz("Anliegen"), JETZT);
    termin.storniere(storniert);
    termin.buche(familie("schmidt"), ziel(), new Notiz("Frage"), JETZT);
    termin.ereignisseAbholen();

    boolean geaendert = termin.anonymisiere(Pseudonymisierung.mitSeed("ab12", EMAIL), JETZT);

    assertThat(geaendert).isTrue();
    assertThat(termin.buchungen())
        .extracting(Buchung::familie)
        .containsExactly(
            new Familie("Eltern-ab12-001", "Schueler-ab12-001", EMAIL),
            new Familie("Eltern-ab12-002", "Schueler-ab12-002", EMAIL));
    assertThat(termin.buchungen()).allSatisfy(b -> assertThat(b.notiz()).isEmpty());
    assertThat(termin.ereignisseAbholen()).isEmpty();
  }

  /** Die Auslastung ist das Wissen, das bleiben soll: Status, Ziel und Belegung bleiben stehen. */
  @Test
  void anonymisiere_laesstStatusZielUndBelegungStehen() {
    Termin termin = freierTermin();
    Buchungsziel ziel = ziel();
    BuchungId aktiv = termin.buche(familie("mueller"), ziel, null, JETZT);
    termin.erinnereBuchung(aktiv, JETZT.plusDays(1));

    termin.anonymisiere(Pseudonymisierung.mitSeed("ab12", EMAIL), JETZT);

    Buchung buchung = termin.aktiveBuchung().orElseThrow();
    assertThat(buchung.id()).isEqualTo(aktiv);
    assertThat(buchung.status()).isEqualTo(Buchungsstatus.ZUGESAGT);
    assertThat(buchung.ziel()).isEqualTo(ziel);
    assertThat(buchung.erstelltAm()).isEqualTo(JETZT);
    assertThat(buchung.erinnerungVersendetAm()).contains(JETZT.plusDays(1));
    assertThat(termin.istBuchbar()).isFalse();
  }

  @Test
  void anonymisiere_ohneBuchung_aendertNichts() {
    assertThat(freierTermin().anonymisiere(Pseudonymisierung.mitSeed("ab12", EMAIL), JETZT)).isFalse();
  }

  /** Issue #129: das Löschverlangen einer Familie zieht die Anonymisierung für eine Buchung vor. */
  @Test
  void entferneAngaben_stornierteBuchung_ersetztFamilieUndVermerktDenZeitpunkt() {
    Termin termin = freierTermin();
    BuchungId storniert = termin.buche(familie("mueller"), ziel(), new Notiz("Anliegen"), JETZT);
    termin.storniere(storniert);
    termin.ereignisseAbholen();

    boolean geaendert =
        termin.entferneAngaben(
            storniert, Pseudonymisierung.mitSeed("ab12", EMAIL), JETZT.plusDays(2), true);

    assertThat(geaendert).isTrue();
    Buchung buchung = termin.buchungen().getFirst();
    assertThat(buchung.familie()).isEqualTo(new Familie("Eltern-ab12-001", "Schueler-ab12-001", EMAIL));
    assertThat(buchung.notiz()).isEmpty();
    assertThat(buchung.status()).isEqualTo(Buchungsstatus.STORNIERT);
    assertThat(buchung.anonymisiertAm()).contains(JETZT.plusDays(2));
    assertThat(termin.ereignisseAbholen()).isEmpty();
  }

  /** Vor dem Sprechtag hieße es einen Geistertermin: erst stornieren, dann entfernen. */
  @Test
  void entferneAngaben_geltendeZusageAnVeroeffentlichtemSprechtag_wirdAbgewiesen() {
    Termin termin = freierTermin();
    BuchungId aktiv = termin.buche(familie("mueller"), ziel(), new Notiz("Anliegen"), JETZT);

    assertThatThrownBy(
            () ->
                termin.entferneAngaben(
                    aktiv, Pseudonymisierung.mitSeed("ab12", EMAIL), JETZT, true))
        .isInstanceOf(BuchungNochAktivException.class);
    Buchung buchung = termin.aktiveBuchung().orElseThrow();
    assertThat(buchung.familie()).isEqualTo(familie("mueller"));
    assertThat(buchung.notiz()).contains(new Notiz("Anliegen"));
    assertThat(buchung.anonymisiertAm()).isEmpty();
  }

  /** Nach dem Sprechtag bleibt die Zusage stehen — die Belegung ist das Wissen, das bleiben soll. */
  @Test
  void entferneAngaben_geltendeZusageNachDemSprechtag_bleibtBelegt() {
    Termin termin = freierTermin();
    BuchungId aktiv = termin.buche(familie("mueller"), ziel(), null, JETZT);

    termin.entferneAngaben(aktiv, Pseudonymisierung.mitSeed("ab12", EMAIL), JETZT, false);

    Buchung buchung = termin.aktiveBuchung().orElseThrow();
    assertThat(buchung.id()).isEqualTo(aktiv);
    assertThat(buchung.familie()).isEqualTo(new Familie("Eltern-ab12-001", "Schueler-ab12-001", EMAIL));
    assertThat(buchung.anonymisiertAm()).contains(JETZT);
    assertThat(termin.istBuchbar()).isFalse();
  }

  /** Der zweite Klick aus einem alten Tab: still, ohne Fehler, und der erste Zeitpunkt bleibt. */
  @Test
  void entferneAngaben_zweitesMal_aendertNichtsMehr() {
    Termin termin = freierTermin();
    BuchungId aktiv = termin.buche(familie("mueller"), ziel(), null, JETZT);
    termin.entferneAngaben(aktiv, Pseudonymisierung.mitSeed("ab12", EMAIL), JETZT, false);

    boolean geaendert =
        termin.entferneAngaben(
            aktiv, Pseudonymisierung.mitSeed("cd34", EMAIL), JETZT.plusDays(1), false);

    assertThat(geaendert).isFalse();
    Buchung buchung = termin.aktiveBuchung().orElseThrow();
    assertThat(buchung.familie().elternName()).isEqualTo("Eltern-ab12-001");
    assertThat(buchung.anonymisiertAm()).contains(JETZT);
  }

  @Test
  void entferneAngaben_fremdeBuchung_wirdAbgewiesen() {
    Termin termin = freierTermin();

    assertThatThrownBy(
            () ->
                termin.entferneAngaben(
                    BuchungId.neu(), Pseudonymisierung.mitSeed("ab12", EMAIL), JETZT, false))
        .isInstanceOf(BuchungNichtGefundenException.class);
  }

  /** Der Nachtlauf vermerkt an jeder Buchung; eine vorher entfernte behält ihren Zeitpunkt. */
  @Test
  void anonymisiere_vermerktDenZeitpunkt_undBehaeltEinenFrueheren() {
    Termin termin = freierTermin();
    BuchungId frueher = termin.buche(familie("mueller"), ziel(), null, JETZT);
    termin.storniere(frueher);
    termin.entferneAngaben(frueher, Pseudonymisierung.mitSeed("ab12", EMAIL), JETZT, true);
    termin.buche(familie("schmidt"), ziel(), null, JETZT);

    termin.anonymisiere(Pseudonymisierung.mitSeed("cd34", EMAIL), JETZT.plusDays(31));

    assertThat(termin.buchungen())
        .extracting(Buchung::anonymisiertAm)
        .containsExactly(Optional.of(JETZT), Optional.of(JETZT.plusDays(31)));
  }
}
