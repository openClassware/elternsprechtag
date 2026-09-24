package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.SlotZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.SlotZustand;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit-Tests des Vaadin-freien Auswahl-Modells des Ausfall-Dialogs (Issue #156): Auswählen und
 * Abwählen, „alle auswählen", bereits entfallene Slots sind nicht wählbar, und eine Familie mit
 * zwei betroffenen Slots zählt als eine Familie.
 */
class AusfallAuswahlTest {

  private static SlotZeile frei(LocalTime zeit) {
    return new SlotZeile(UUID.randomUUID(), zeit, SlotZustand.FREI, null, null, null);
  }

  private static SlotZeile gebucht(LocalTime zeit, String familie) {
    return new SlotZeile(
        UUID.randomUUID(), zeit, SlotZustand.GEBUCHT, "Kind " + familie, "Eltern " + familie, familie);
  }

  private static SlotZeile entfallen(LocalTime zeit) {
    return new SlotZeile(UUID.randomUUID(), zeit, SlotZustand.ENTFALLEN, null, null, null);
  }

  @Test
  void neueAuswahl_istLeer() {
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(frei(LocalTime.of(14, 0))));

    assertThat(auswahl.hatAuswahl()).isFalse();
    assertThat(auswahl.anzahlTermine()).isZero();
    assertThat(auswahl.anzahlFamilien()).isZero();
  }

  @Test
  void toggle_waehltUndWiderruft() {
    SlotZeile slot = frei(LocalTime.of(14, 0));
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(slot));

    auswahl.toggle(slot);
    assertThat(auswahl.istGewaehlt(slot)).isTrue();
    assertThat(auswahl.anzahlTermine()).isEqualTo(1);

    auswahl.toggle(slot);
    assertThat(auswahl.istGewaehlt(slot)).isFalse();
    assertThat(auswahl.hatAuswahl()).isFalse();
  }

  @Test
  void toggle_entfallenerSlot_bleibtUnberuehrt() {
    SlotZeile slot = entfallen(LocalTime.of(14, 0));
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(slot));

    auswahl.toggle(slot);

    assertThat(auswahl.istWaehlbar(slot)).isFalse();
    assertThat(auswahl.istGewaehlt(slot)).isFalse();
  }

  @Test
  void waehleAlle_waehltNurWaehlbareSlots() {
    SlotZeile freierSlot = frei(LocalTime.of(14, 0));
    SlotZeile gebuchterSlot = gebucht(LocalTime.of(14, 15), "mueller@example.com");
    SlotZeile entfallenerSlot = entfallen(LocalTime.of(14, 30));
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(freierSlot, gebuchterSlot, entfallenerSlot));

    auswahl.waehleAlle();

    assertThat(auswahl.istGewaehlt(freierSlot)).isTrue();
    assertThat(auswahl.istGewaehlt(gebuchterSlot)).isTrue();
    assertThat(auswahl.istGewaehlt(entfallenerSlot)).isFalse();
    assertThat(auswahl.anzahlTermine()).isEqualTo(2);
  }

  @Test
  void waehleKeine_leertDieAuswahl() {
    SlotZeile slot = frei(LocalTime.of(14, 0));
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(slot));
    auswahl.waehleAlle();

    auswahl.waehleKeine();

    assertThat(auswahl.hatAuswahl()).isFalse();
  }

  @Test
  void anzahlFamilien_zweiSlotsDerselbenFamilie_zaehlenAlsEine() {
    SlotZeile ersterSlot = gebucht(LocalTime.of(14, 0), "mueller@example.com");
    SlotZeile zweiterSlot = gebucht(LocalTime.of(14, 15), "mueller@example.com");
    SlotZeile andereFamilie = gebucht(LocalTime.of(14, 30), "schmidt@example.com");
    AusfallAuswahl auswahl =
        new AusfallAuswahl(List.of(ersterSlot, zweiterSlot, andereFamilie));

    auswahl.toggle(ersterSlot);
    auswahl.toggle(zweiterSlot);

    assertThat(auswahl.anzahlTermine()).isEqualTo(2);
    assertThat(auswahl.anzahlFamilien()).isEqualTo(1);
  }

  @Test
  void anzahlFamilien_freierSlotOhneFamilienSchluessel_zaehltNicht() {
    SlotZeile slot = frei(LocalTime.of(14, 0));
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(slot));

    auswahl.toggle(slot);

    assertThat(auswahl.anzahlTermine()).isEqualTo(1);
    assertThat(auswahl.anzahlFamilien()).isZero();
  }

  @Test
  void gewaehlteTerminIds_liefertGenauDieAusgewaehlten() {
    SlotZeile a = frei(LocalTime.of(14, 0));
    SlotZeile b = frei(LocalTime.of(14, 15));
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(a, b));

    auswahl.toggle(a);

    assertThat(auswahl.gewaehlteTerminIds()).containsExactly(a.terminId());
  }
}
