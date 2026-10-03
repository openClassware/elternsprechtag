package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.EmailWiederholung.Abgleich;
import org.junit.jupiter.api.Test;

/**
 * Unit-Tests der Vaadin-freien Regel für die zweite E-Mail-Eingabe (Issue #111): Randleerzeichen
 * und Groß-/Kleinschreibung zählen nicht, ein echter Dreher schon.
 */
class EmailWiederholungTest {

  @Test
  void ohneWiederholung_istLeer() {
    for (String wiederholung : new String[] {null, "", "   "}) {
      assertThat(EmailWiederholung.vergleiche("anna@example.com", wiederholung))
          .isEqualTo(Abgleich.LEER);
    }
  }

  @Test
  void identischeEingaben_sindGleich() {
    assertThat(EmailWiederholung.vergleiche("anna@example.com", "anna@example.com"))
        .isEqualTo(Abgleich.GLEICH);
  }

  @Test
  void randleerzeichen_zaehlenNicht() {
    assertThat(EmailWiederholung.vergleiche(" anna@example.com", "anna@example.com  "))
        .isEqualTo(Abgleich.GLEICH);
  }

  @Test
  void grossUndKleinschreibung_zaehltNicht() {
    assertThat(EmailWiederholung.vergleiche("Anna@Example.com", "anna@EXAMPLE.COM"))
        .isEqualTo(Abgleich.GLEICH);
  }

  @Test
  void buchstabendreher_weichtAb() {
    assertThat(EmailWiederholung.vergleiche("anna@example.com", "anan@example.com"))
        .isEqualTo(Abgleich.ABWEICHEND);
  }

  @Test
  void wiederholungOhneErsteEingabe_weichtAb() {
    assertThat(EmailWiederholung.vergleiche(null, "anna@example.com"))
        .isEqualTo(Abgleich.ABWEICHEND);
    assertThat(EmailWiederholung.vergleiche("", "anna@example.com"))
        .isEqualTo(Abgleich.ABWEICHEND);
  }
}
