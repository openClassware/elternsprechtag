package de.openclassware.elternsprechtag.sprechtag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Die Ersatzwerte eines Anonymisierungslaufs (Issue #126) — Feldkennung, Seed, laufende Nummer. */
class PseudonymisierungTest {

  private static final String EMAIL = "anonym@schule.example";

  @Test
  void zaehltInnerhalbDesLaufsHoch() {
    Pseudonymisierung lauf = Pseudonymisierung.mitSeed("ab12", EMAIL);

    assertThat(lauf.naechsteFamilie())
        .isEqualTo(new Familie("Eltern-ab12-001", "Schueler-ab12-001", EMAIL));
    assertThat(lauf.naechsteFamilie())
        .isEqualTo(new Familie("Eltern-ab12-002", "Schueler-ab12-002", EMAIL));
  }

  /** Über 999 hinaus wächst die Nummer einfach weiter, statt umzubrechen. */
  @Test
  void jenseitsDerDreistelligkeit_bleibtDieNummerEindeutig() {
    Pseudonymisierung lauf = Pseudonymisierung.mitSeed("ab12", EMAIL);
    for (int i = 0; i < 999; i++) {
      lauf.naechsteFamilie();
    }

    assertThat(lauf.naechsteFamilie().elternName()).isEqualTo("Eltern-ab12-1000");
  }

  @Test
  void jederLaufBekommtEinenEigenenSeed() {
    assertThat(Pseudonymisierung.neu(EMAIL).seed()).isNotEqualTo(Pseudonymisierung.neu(EMAIL).seed());
  }

  @Test
  void ohneSeedGibtEsKeinenLauf() {
    assertThatThrownBy(() -> Pseudonymisierung.mitSeed(" ", EMAIL))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Die Adresse ist Betriebseinstellung — leer eingestellt, soll der Lauf gar nicht erst beginnen. */
  @Test
  void ohneEmailGibtEsKeinenLauf() {
    assertThatThrownBy(() -> Pseudonymisierung.neu(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
