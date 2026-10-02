package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.BuchungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit-Tests der Vaadin-freien Namenssuche der Auswertung (Issue #121): Wortzerlegung, Treffer in
 * Schüler- oder Elternname, ausgeblendete Pläne ohne Treffer und die Zählung stornierter Treffer.
 */
class NamenssucheTest {

  private static BuchungsZeile zeile(String schueler, String eltern) {
    return new BuchungsZeile(
        UUID.randomUUID(), LocalTime.of(16, 0), schueler, "5a", "Ma", eltern, null, false, false, null);
  }

  private static BuchungsZeile storniert(String schueler, String eltern) {
    return new BuchungsZeile(
        UUID.randomUUID(), LocalTime.of(16, 0), schueler, "5a", "Ma", eltern, null, true, false, null);
  }

  private static LehrkraftPlan plan(
      String name, List<BuchungsZeile> zeilen, List<BuchungsZeile> stornierte) {
    return new LehrkraftPlan(UUID.randomUUID(), "XY", name, zeilen.size(), 1, zeilen, stornierte);
  }

  @Test
  void ohneBegriff_istInaktivUndLaesstAllesStehen() {
    LehrkraftPlan leer = plan("Frau Leer", List.of(), List.of());
    List<LehrkraftPlan> plaene = List.of(leer);

    for (String eingabe : new String[] {null, "", "   "}) {
      Namenssuche suche = Namenssuche.nach(eingabe);
      assertThat(suche.istAktiv()).isFalse();
      assertThat(suche.filtere(plaene, false)).isSameAs(plaene);
      assertThat(suche.stornierteTreffer(plaene)).isZero();
    }
  }

  @Test
  void begriff_wirdAnDenRaendernBereinigt() {
    assertThat(Namenssuche.nach("  Müller ").begriff()).isEqualTo("Müller");
  }

  @Test
  void teilstring_ohneUnterschiedDerSchreibung_inSchuelerOderElternname() {
    BuchungsZeile zeile = zeile("Lena Müller", "Sabine Schmidt");

    assertThat(Namenssuche.nach("mül").passt(zeile)).isTrue();
    assertThat(Namenssuche.nach("SCHMI").passt(zeile)).isTrue();
    assertThat(Namenssuche.nach("Meier").passt(zeile)).isFalse();
  }

  @Test
  void jedesWortMussPassen_inBeliebigerReihenfolge() {
    BuchungsZeile zeile = zeile("Müller, Lena", "Sabine Müller");

    assertThat(Namenssuche.nach("Lena Müller").passt(zeile)).isTrue();
    assertThat(Namenssuche.nach("Lena   Sabine").passt(zeile)).isTrue();
    assertThat(Namenssuche.nach("Lena Meier").passt(zeile)).isFalse();
  }

  @Test
  void umlaute_werdenNichtAngeglichen() {
    assertThat(Namenssuche.nach("Muell").passt(zeile("Lena Müller", "Sabine Müller"))).isFalse();
  }

  @Test
  void filtere_behaeltNurPassendeZeilenUndPlaeneMitTreffer() {
    BuchungsZeile lena = zeile("Lena Müller", "Sabine Müller");
    BuchungsZeile tom = zeile("Tom Meier", "Kai Meier");
    LehrkraftPlan mitTreffer = plan("Frau A", List.of(lena, tom), List.of());
    LehrkraftPlan ohneTreffer = plan("Herr B", List.of(tom), List.of());

    List<LehrkraftPlan> ergebnis =
        Namenssuche.nach("müller").filtere(List.of(mitTreffer, ohneTreffer), false);

    assertThat(ergebnis).singleElement().satisfies(plan -> {
      assertThat(plan.anzeigeName()).isEqualTo("Frau A");
      assertThat(plan.zeilen()).containsExactly(lena);
      // Die Kopfzahlen beschreiben die Lehrkraft, nicht die Treffer.
      assertThat(plan.anzahl()).isEqualTo(2);
      assertThat(plan.entfalleneAnzahl()).isEqualTo(1);
    });
  }

  @Test
  void stornierterTreffer_haeltDenPlanNurBeiAngezeigtenStornierten() {
    BuchungsZeile alt = storniert("Lena Müller", "Sabine Müller");
    LehrkraftPlan plan = plan("Frau A", List.of(zeile("Tom Meier", "Kai Meier")), List.of(alt));
    Namenssuche suche = Namenssuche.nach("Müller");

    assertThat(suche.filtere(List.of(plan), false)).isEmpty();
    assertThat(suche.filtere(List.of(plan), true))
        .singleElement()
        .satisfies(p -> {
          assertThat(p.zeilen()).isEmpty();
          assertThat(p.stornierte()).containsExactly(alt);
        });
  }

  @Test
  void stornierteTreffer_zaehltUeberAllePlaene() {
    LehrkraftPlan a =
        plan("Frau A", List.of(), List.of(storniert("Lena Müller", "X"), storniert("Tom", "Y")));
    LehrkraftPlan b = plan("Herr B", List.of(zeile("Lena Müller", "X")), List.of(storniert("Z", "Müller")));

    assertThat(Namenssuche.nach("Müller").stornierteTreffer(List.of(a, b))).isEqualTo(2);
  }
}
