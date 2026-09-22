package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.components;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.AusfallSlot;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.Slotzustand;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit-Tests des Vaadin-freien Auswahl-Modells hinter dem Ausfall-Dialog. Hier liegt die
 * Entscheidungslogik, die sonst im Dialog säße: was wählbar ist, was „alle" bedeutet und wie aus
 * einer Auswahl „N Termine, etwa M Familien" wird.
 */
class AusfallAuswahlTest {

  private static AusfallSlot frei(int minute) {
    return new AusfallSlot(
        UUID.randomUUID(), LocalTime.of(14, minute), Slotzustand.FREI, null, null, null);
  }

  private static AusfallSlot gebucht(int minute, String kind, String adresse) {
    return new AusfallSlot(
        UUID.randomUUID(),
        LocalTime.of(14, minute),
        Slotzustand.GEBUCHT,
        kind,
        "Familie " + kind,
        adresse);
  }

  private static AusfallSlot entfallen(int minute) {
    return new AusfallSlot(
        UUID.randomUUID(), LocalTime.of(14, minute), Slotzustand.ENTFALLEN, null, null, null);
  }

  @Test
  void frischeAuswahl_istLeer() {
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(frei(0), gebucht(15, "Lena", "a@b.de")));

    assertThat(auswahl.hatAuswahl()).isFalse();
    assertThat(auswahl.anzahlTermine()).isZero();
    assertThat(auswahl.anzahlFamilien()).isZero();
    assertThat(auswahl.terminIds()).isEmpty();
  }

  @Test
  void entfalleneSlots_sindNichtWaehlbar() {
    AusfallSlot schonWeg = entfallen(0);
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(schonWeg, frei(15)));

    assertThat(auswahl.istWaehlbar(schonWeg)).isFalse();
    assertThat(auswahl.istWaehlbar(auswahl.slots().get(1))).isTrue();
  }

  @Test
  void waehle_aufEinemEntfallenenSlot_verpufft() {
    AusfallSlot schonWeg = entfallen(0);
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(schonWeg));

    auswahl.waehle(schonWeg.terminId(), true);

    assertThat(auswahl.hatAuswahl())
        .as("ein entfallener Termin kommt auch über einen zweiten Weg nicht in die Auswahl")
        .isFalse();
  }

  @Test
  void anzahlTermine_zaehltJedenGewaehltenSlot_auchFreie() {
    AusfallSlot einFreier = frei(0);
    AusfallSlot eineBuchung = gebucht(15, "Lena", "mueller@example.com");
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(einFreier, eineBuchung));

    auswahl.waehle(einFreier.terminId(), true);
    auswahl.waehle(eineBuchung.terminId(), true);

    assertThat(auswahl.anzahlTermine()).isEqualTo(2);
    assertThat(auswahl.anzahlFamilien()).as("ein freier Slot trifft keine Familie").isEqualTo(1);
  }

  @Test
  void anzahlFamilien_zaehltZweiSlotsDerselbenAdresseEinmal() {
    AusfallSlot vormittag = gebucht(0, "Lena", "mueller@example.com");
    AusfallSlot nachmittag = gebucht(15, "Jonas", "mueller@example.com");
    AusfallSlot andere = gebucht(30, "Pia", "schmidt@example.com");
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(vormittag, nachmittag, andere));

    auswahl.waehle(vormittag.terminId(), true);
    auswahl.waehle(nachmittag.terminId(), true);
    auswahl.waehle(andere.terminId(), true);

    assertThat(auswahl.anzahlTermine()).isEqualTo(3);
    assertThat(auswahl.anzahlFamilien())
        .as("zwei Geschwister teilen sich die Adresse — eine Familie, eine Nachricht")
        .isEqualTo(2);
  }

  @Test
  void abwaehlen_nimmtDenSlotWiederAusDerAuswahl() {
    AusfallSlot slot = gebucht(0, "Lena", "mueller@example.com");
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(slot, frei(15)));

    auswahl.waehle(slot.terminId(), true);
    auswahl.waehle(slot.terminId(), false);

    assertThat(auswahl.istGewaehlt(slot.terminId())).isFalse();
    assertThat(auswahl.anzahlTermine()).isZero();
    assertThat(auswahl.anzahlFamilien()).isZero();
  }

  @Test
  void waehleAlle_nimmtJedenWaehlbarenSlot_aberKeinenEntfallenen() {
    AusfallSlot einFreier = frei(0);
    AusfallSlot eineBuchung = gebucht(15, "Lena", "mueller@example.com");
    AusfallSlot schonWeg = entfallen(30);
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(einFreier, eineBuchung, schonWeg));

    auswahl.waehleAlle(true);

    assertThat(auswahl.terminIds()).containsExactly(einFreier.terminId(), eineBuchung.terminId());
    assertThat(auswahl.alleWaehlbarenGewaehlt()).isTrue();
  }

  @Test
  void waehleAlle_mitFalse_raeumtDieAuswahlAb() {
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(frei(0), frei(15)));
    auswahl.waehleAlle(true);

    auswahl.waehleAlle(false);

    assertThat(auswahl.hatAuswahl()).isFalse();
  }

  @Test
  void alleWaehlbarenGewaehlt_ohneWaehlbareSlots_istFalsch() {
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(entfallen(0)));

    assertThat(auswahl.hatWaehlbare()).isFalse();
    assertThat(auswahl.alleWaehlbarenGewaehlt())
        .as("wo nichts zu wählen ist, ist auch nicht alles gewählt — der Knopf bliebe sonst an")
        .isFalse();
  }

  @Test
  void terminIds_stehenInDerReihenfolgeDerSlots_nichtDerKlicks() {
    AusfallSlot frueh = frei(0);
    AusfallSlot spaet = frei(30);
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(frueh, spaet));

    auswahl.waehle(spaet.terminId(), true);
    auswahl.waehle(frueh.terminId(), true);

    assertThat(auswahl.terminIds()).containsExactly(frueh.terminId(), spaet.terminId());
  }

  @Test
  void unbekannteTerminId_verpufft() {
    AusfallAuswahl auswahl = new AusfallAuswahl(List.of(frei(0)));

    auswahl.waehle(UUID.randomUUID(), true);

    assertThat(auswahl.hatAuswahl()).isFalse();
  }
}
