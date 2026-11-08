package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts.Rechtslinks.Rechtslink;
import org.junit.jupiter.api.Test;

/** Welche Footer-Links erscheinen — ohne Spring, ohne Vaadin (Issue #230). */
class RechtslinksTest {

  private static final String IMPRESSUM = "https://schule.example/impressum";
  private static final String DATENSCHUTZ = "https://schule.example/datenschutz";

  @Test
  void ohneBeideAdressen_keinLink() {
    assertThat(Rechtslinks.aus(null, null)).isEmpty();
  }

  @Test
  void leereOderBlankeAdresse_giltAlsNichtGesetzt() {
    assertThat(Rechtslinks.aus("", "   ")).isEmpty();
  }

  @Test
  void nurImpressum_nurDessenLink() {
    assertThat(Rechtslinks.aus(IMPRESSUM, null))
        .containsExactly(new Rechtslink(Rechtslinks.IMPRESSUM, IMPRESSUM));
  }

  @Test
  void nurDatenschutzerklaerung_nurDerenLink() {
    assertThat(Rechtslinks.aus(null, DATENSCHUTZ))
        .containsExactly(new Rechtslink(Rechtslinks.DATENSCHUTZERKLAERUNG, DATENSCHUTZ));
  }

  @Test
  void beide_inFesterReihenfolgeImpressumZuerst() {
    assertThat(Rechtslinks.aus(IMPRESSUM, DATENSCHUTZ))
        .containsExactly(
            new Rechtslink(Rechtslinks.IMPRESSUM, IMPRESSUM),
            new Rechtslink(Rechtslinks.DATENSCHUTZERKLAERUNG, DATENSCHUTZ));
  }

  @Test
  void adresseWirdGetrimmt() {
    assertThat(Rechtslinks.aus("  " + IMPRESSUM + " ", null))
        .containsExactly(new Rechtslink(Rechtslinks.IMPRESSUM, IMPRESSUM));
  }
}
