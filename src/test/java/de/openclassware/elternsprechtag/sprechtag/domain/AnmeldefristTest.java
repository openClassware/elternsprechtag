package de.openclassware.elternsprechtag.sprechtag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Der Wertebereich der Anmeldefrist (Issue #122): 0 bis 28 Tage vor dem Sprechtag. Die Obergrenze
 * fängt den Tippfehler ab — 40 statt 4 schlösse die Anmeldung wochenlang vorher.
 */
class AnmeldefristTest {

  @Test
  void amTagSelbst_istErlaubt() {
    assertThat(Anmeldefrist.vonTagen(0).tageVorher()).isZero();
  }

  @Test
  void vierWochenVorher_istErlaubt() {
    assertThat(Anmeldefrist.vonTagen(28).tageVorher()).isEqualTo(28);
  }

  @Test
  void nachDemSprechtag_gibtEsNicht() {
    assertThatThrownBy(() -> Anmeldefrist.vonTagen(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void mehrAlsVierWochenVorher_gibtEsNicht() {
    assertThatThrownBy(() -> Anmeldefrist.vonTagen(29))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void standardIstDerVortag() {
    assertThat(Anmeldefrist.STANDARD.tageVorher()).isEqualTo(1);
  }

  @Test
  void anmeldeschlussIstDasDatumMinusDieTage() {
    assertThat(Anmeldefrist.vonTagen(4).anmeldeschlussFuer(LocalDate.of(2026, 3, 24)))
        .isEqualTo(LocalDate.of(2026, 3, 20));
  }
}
