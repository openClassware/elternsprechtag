package de.openclassware.elternsprechtag.sprechtag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Die Kernregel <em>ein Slot, höchstens eine aktive Buchung</em> — ohne Spring-Kontext und ohne
 * Datenbank. Dass das geht, ist der eigentliche Gewinn des Aggregats: Die wichtigste Regel der
 * Anwendung hing bisher an einem {@code @SpringBootTest} mit laufender Postgres.
 */
class TerminTest {

  private static final LocalDateTime JETZT = LocalDateTime.of(2026, 7, 20, 14, 0);

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
}
