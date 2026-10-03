package de.openclassware.elternsprechtag.sprechtag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ZustellungTest {

  private static final LocalDateTime JETZT = LocalDateTime.of(2026, 7, 1, 9, 30);

  @Test
  void fehlgeschlagen_heisst_dieFamilieHatNichtsBekommen() {
    Zustellung zustellung =
        Zustellung.vermerke(
            BuchungId.neu(), Mailart.ABSAGE, Zustellergebnis.FEHLGESCHLAGEN, JETZT);

    assertThat(zustellung.istFehlgeschlagen()).isTrue();
  }

  @Test
  void abgeschickt_istKeinFehlschlag() {
    Zustellung zustellung =
        Zustellung.vermerke(
            BuchungId.neu(), Mailart.BESTAETIGUNG, Zustellergebnis.ABGESCHICKT, JETZT);

    assertThat(zustellung.istFehlgeschlagen()).isFalse();
  }

  @Test
  void vermerk_verlangtAlleAngaben() {
    BuchungId buchung = BuchungId.neu();

    assertThatThrownBy(
            () -> Zustellung.vermerke(null, Mailart.ABSAGE, Zustellergebnis.ABGESCHICKT, JETZT))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> Zustellung.vermerke(buchung, null, Zustellergebnis.ABGESCHICKT, JETZT))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> Zustellung.vermerke(buchung, Mailart.ABSAGE, null, JETZT))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(
            () -> Zustellung.vermerke(buchung, Mailart.ABSAGE, Zustellergebnis.ABGESCHICKT, null))
        .isInstanceOf(NullPointerException.class);
  }
}
