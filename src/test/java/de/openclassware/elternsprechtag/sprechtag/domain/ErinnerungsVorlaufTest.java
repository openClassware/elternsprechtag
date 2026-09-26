package de.openclassware.elternsprechtag.sprechtag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Die Fälligkeitsregel des Erinnerungs-Schedulers (Issue #107): „verfallen statt nachholen" — ein
 * verpasster Lauf holt den Versand nicht am nächsten Tag nach.
 */
class ErinnerungsVorlaufTest {

  private static final LocalDate SPRECHTAG = LocalDate.of(2026, 7, 20);

  @Test
  void einTag_istGenauAmVortagFaellig() {
    assertThat(ErinnerungsVorlauf.EIN_TAG.istFaelligAm(SPRECHTAG, SPRECHTAG.minusDays(1)))
        .isTrue();
  }

  @Test
  void einTag_amSprechtagSelbst_istNichtMehrFaellig() {
    // Verfallen statt nachholen: Ein verpasster Lauf am Vortag liefert die Erinnerung nicht
    // nachträglich am Sprechtagmorgen.
    assertThat(ErinnerungsVorlauf.EIN_TAG.istFaelligAm(SPRECHTAG, SPRECHTAG)).isFalse();
  }

  @Test
  void einTag_zweiTageVorher_istNochNichtFaellig() {
    assertThat(ErinnerungsVorlauf.EIN_TAG.istFaelligAm(SPRECHTAG, SPRECHTAG.minusDays(2)))
        .isFalse();
  }

  @Test
  void dreiTage_istGenauDreiTageVorherFaellig() {
    assertThat(ErinnerungsVorlauf.DREI_TAGE.istFaelligAm(SPRECHTAG, SPRECHTAG.minusDays(3)))
        .isTrue();
  }

  @Test
  void vonTagen_findetDieOptionZuJedemAbstand() {
    for (ErinnerungsVorlauf vorlauf : ErinnerungsVorlauf.values()) {
      assertThat(ErinnerungsVorlauf.vonTagen(vorlauf.tageVorher())).isEqualTo(vorlauf);
    }
  }

  /** Die Optionen bleiben fest (#106) — auch wenn das Formular sie als Zahl anbietet. */
  @Test
  void vonTagen_ausserhalbDerOptionen_gibtEsNicht() {
    assertThat(ErinnerungsVorlauf.istZulaessig(4)).isFalse();
    assertThat(ErinnerungsVorlauf.istZulaessig(-1)).isFalse();
    assertThatThrownBy(() -> ErinnerungsVorlauf.vonTagen(4))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void keine_istNieFaellig() {
    for (int tage = 0; tage <= 3; tage++) {
      assertThat(ErinnerungsVorlauf.KEINE.istFaelligAm(SPRECHTAG, SPRECHTAG.minusDays(tage)))
          .isFalse();
    }
  }
}
