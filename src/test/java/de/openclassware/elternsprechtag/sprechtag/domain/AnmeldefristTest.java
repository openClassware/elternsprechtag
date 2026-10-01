package de.openclassware.elternsprechtag.sprechtag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * Der Wertebereich der Anmeldefrist (Issue #122): 0 bis 28 Tage vor dem Sprechtag. Die Obergrenze
 * fängt den Tippfehler ab — 40 statt 4 schlösse die Anmeldung wochenlang vorher.
 */
class AnmeldefristTest {

  private static final LocalDate DATUM = LocalDate.of(2026, 3, 24);
  private static final LocalTime BEGINN = LocalTime.of(15, 0);

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

  /** Dieselbe Grenze, an der die Formularvalidierung fragt, bevor der Konstruktor wirft. */
  @Test
  void istZulaessig_folgtDemKonstruktor() {
    assertThat(Anmeldefrist.istZulaessig(0)).isTrue();
    assertThat(Anmeldefrist.istZulaessig(28)).isTrue();
    assertThat(Anmeldefrist.istZulaessig(-1)).isFalse();
    assertThat(Anmeldefrist.istZulaessig(29)).isFalse();
  }

  @Test
  void standardIstDerVortag() {
    assertThat(Anmeldefrist.STANDARD.tageVorher()).isEqualTo(1);
  }

  @Test
  void letzterTagIstDasDatumMinusDieTage() {
    assertThat(Anmeldefrist.vonTagen(4).letzterTagFuer(DATUM)).isEqualTo(LocalDate.of(2026, 3, 20));
  }

  /** Ab 1 zählt der letzte Tag ganz — der Schluss ist die Mitternacht danach. */
  @Test
  void abEinemTag_schliesstDieAnmeldungMitDemEndeDesLetztenTags() {
    assertThat(Anmeldefrist.vonTagen(4).anmeldeschlussFuer(DATUM, BEGINN))
        .isEqualTo(LocalDateTime.of(2026, 3, 21, 0, 0));
    assertThat(Anmeldefrist.vonTagen(1).anmeldeschlussFuer(DATUM, BEGINN))
        .isEqualTo(DATUM.atStartOfDay());
  }

  /** Issue #118 — bei 0 stünden sonst am Tag selbst Slots zur Wahl, die schon begonnen haben. */
  @Test
  void beiNull_schliesstDieAnmeldungMitDemBeginn() {
    assertThat(Anmeldefrist.vonTagen(0).anmeldeschlussFuer(DATUM, BEGINN))
        .isEqualTo(DATUM.atTime(BEGINN));
  }

  @Test
  void nurBeiNull_schliesstDieAnmeldungMitDemBeginn() {
    assertThat(Anmeldefrist.vonTagen(0).schliesstMitBeginn()).isTrue();
    assertThat(Anmeldefrist.vonTagen(1).schliesstMitBeginn()).isFalse();
  }
}
