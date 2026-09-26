package de.openclassware.elternsprechtag.sprechtag.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SprechtagStatusTest {

  /** Den Abschluss stellt der Tagesjob fest, nicht der Organizer (#166). */
  @ParameterizedTest
  @EnumSource(SprechtagStatus.class)
  void abschliessenIstNieVonHandWaehlbar(SprechtagStatus status) {
    assertThat(status.waehlbareUebergaenge()).doesNotContain(SprechtagStatus.ABGESCHLOSSEN);
  }

  @Test
  void einVeroeffentlichterSprechtagBietetZuruecknehmenUndAbsagen() {
    assertThat(SprechtagStatus.VEROEFFENTLICHT.waehlbareUebergaenge())
        .containsExactlyInAnyOrder(SprechtagStatus.ENTWURF, SprechtagStatus.ABGESAGT);
  }

  /** Der Lebenszyklus selbst kennt den Abschluss weiter — sonst könnte ihn der Tagesjob nicht. */
  @Test
  void derLebenszyklusKenntDenAbschlussWeiter() {
    assertThat(SprechtagStatus.VEROEFFENTLICHT.erlaubteUebergaenge())
        .contains(SprechtagStatus.ABGESCHLOSSEN);
  }
}
