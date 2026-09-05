package de.openclassware.elternsprechtag.ui;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.services.BuchungService.BuchungsWunsch;
import de.openclassware.elternsprechtag.services.BuchungService.LehrkraftOption;
import de.openclassware.elternsprechtag.services.BuchungService.SlotOption;
import de.openclassware.elternsprechtag.ui.BookingSession.SlotState;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit-Tests des Vaadin-freien Buchungs-„Warenkorbs". Deckt die aus dem View gezogene
 * Entscheidungslogik ab: Slot-Zustand inkl. Zeitkonflikt, Aufräumen nach Konflikt, Anfrage-Bau.
 */
class BookingSessionTest {

  private static SlotOption slot(LocalTime zeit, boolean belegt) {
    return new SlotOption(UUID.randomUUID(), zeit, belegt);
  }

  private static LehrkraftOption lehrkraft(String kuerzel, SlotOption... slots) {
    return new LehrkraftOption(
        UUID.randomUUID(),
        UUID.randomUUID(),
        kuerzel,
        "Lehrkraft " + kuerzel,
        List.of("Fach"),
        List.of(slots));
  }

  @Test
  void slotState_belegterSlot_istBelegt() {
    SlotOption belegt = slot(LocalTime.of(14, 0), true);
    LehrkraftOption a = lehrkraft("A", belegt);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);

    assertThat(session.slotState(belegt)).isEqualTo(SlotState.BELEGT);
  }

  @Test
  void slotState_gewaehlterSlot_istGewaehlt() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);
    session.waehle(frei);

    assertThat(session.slotState(frei)).isEqualTo(SlotState.GEWAEHLT);
  }

  @Test
  void slotState_gleicheZeitBeiAndererLehrkraft_istKonflikt() {
    SlotOption aSlot = slot(LocalTime.of(14, 0), false);
    SlotOption bSlot = slot(LocalTime.of(14, 0), false); // andere Lehrkraft, gleiche Zeit
    SlotOption bSlotSpaeter = slot(LocalTime.of(14, 15), false);
    LehrkraftOption a = lehrkraft("A", aSlot);
    LehrkraftOption b = lehrkraft("B", bSlot, bSlotSpaeter);
    BookingSession session = new BookingSession();
    session.reset(List.of(a, b));

    // Bei A den 14:00-Slot wählen ...
    session.setActive(a);
    session.waehle(aSlot);

    // ... dann bei B: 14:00 kollidiert, 14:15 ist frei.
    session.setActive(b);
    assertThat(session.slotState(bSlot)).isEqualTo(SlotState.KONFLIKT);
    assertThat(session.slotState(bSlotSpaeter)).isEqualTo(SlotState.FREI);
  }

  @Test
  void reload_wirftZwischenzeitlichBelegtenSlotAusDerAuswahl() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);
    session.waehle(frei);
    assertThat(session.hatAuswahl()).isTrue();

    // Derselbe Termin ist jetzt belegt -> Auswahl muss verworfen werden.
    SlotOption belegt = new SlotOption(frei.terminId(), frei.zeit(), true);
    session.reload(List.of(lehrkraftMitId(a, belegt)));

    assertThat(session.hatAuswahl()).isFalse();
  }

  @Test
  void reload_wirftVerschwundeneLehrkraftAusDerAuswahl() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    LehrkraftOption b = lehrkraft("B", slot(LocalTime.of(14, 0), false));
    BookingSession session = new BookingSession();
    session.reset(List.of(a, b));
    session.setActive(a);
    session.waehle(frei);

    // A fehlt in den neuen Optionen -> ihre Auswahl fällt raus, active wird null.
    session.reload(List.of(b));

    assertThat(session.hatAuswahl()).isFalse();
    assertThat(session.active()).isNull();
  }

  @Test
  void toWuensche_gibtJedemWunschDieNotizSeinerLehrkraft() {
    SlotOption aSlot = slot(LocalTime.of(14, 0), false);
    SlotOption bSlot = slot(LocalTime.of(14, 15), false);
    LehrkraftOption a = lehrkraft("A", aSlot);
    LehrkraftOption b = lehrkraft("B", bSlot);
    BookingSession session = new BookingSession();
    session.reset(List.of(a, b));
    session.setActive(a);
    session.waehle(aSlot);
    session.setNotiz(a.lehrauftragId(), "Anliegen A");
    session.setActive(b);
    session.waehle(bSlot);

    assertThat(session.auswahlAnzahl()).isEqualTo(2);
    // Jede Notiz hängt an ihrer Lehrkraft; wer keine geschrieben hat, bucht ohne.
    assertThat(session.toWuensche())
        .containsExactly(
            new BuchungsWunsch(a.lehrauftragId(), aSlot.terminId(), "Anliegen A"),
            new BuchungsWunsch(b.lehrauftragId(), bSlot.terminId(), null));
  }

  @Test
  void setNotiz_leererTextZaehltAlsKeineNotiz() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);
    session.waehle(frei);
    session.setNotiz(a.lehrauftragId(), "   ");

    assertThat(session.notiz(a.lehrauftragId())).isEmpty();
    assertThat(session.toWuensche())
        .containsExactly(new BuchungsWunsch(a.lehrauftragId(), frei.terminId(), null));
  }

  @Test
  void setNotiz_laesstDenTextStehen_undBeschneidetErstBeimBuchen() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);
    session.waehle(frei);

    // Während des Tippens bleibt der Text unangetastet, sonst liefen Feld und Modell auseinander.
    session.setNotiz(a.lehrauftragId(), "  Anliegen A  ");
    assertThat(session.notiz(a.lehrauftragId())).isEqualTo("  Anliegen A  ");

    // Beschnitten wird erst an der Grenze zum Service.
    assertThat(session.toWuensche())
        .containsExactly(new BuchungsWunsch(a.lehrauftragId(), frei.terminId(), "Anliegen A"));
  }

  @Test
  void setNotiz_ohneWahl_verpufft() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));

    session.setNotiz(a.lehrauftragId(), "Anliegen ohne Termin");

    assertThat(session.notiz(a.lehrauftragId())).isEmpty();
    assertThat(session.hatAuswahl()).isFalse();
  }

  @Test
  void waehle_slotWechselInnerhalbDerselbenLehrkraft_erhaeltDieNotiz() {
    SlotOption frueh = slot(LocalTime.of(14, 0), false);
    SlotOption spaet = slot(LocalTime.of(14, 15), false);
    LehrkraftOption a = lehrkraft("A", frueh, spaet);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);
    session.waehle(frueh);
    session.setNotiz(a.lehrauftragId(), "Anliegen A");

    session.waehle(spaet);

    assertThat(session.gewaehlterSlot(a.lehrauftragId())).isEqualTo(spaet);
    assertThat(session.notiz(a.lehrauftragId())).isEqualTo("Anliegen A");
  }

  @Test
  void abwaehlen_verwirftDieNotizStill() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);
    session.waehle(frei);
    session.setNotiz(a.lehrauftragId(), "Anliegen A");

    session.abwaehlen();

    assertThat(session.notiz(a.lehrauftragId())).isEmpty();
  }

  @Test
  void entferne_verwirftDieNotizDerZeile() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);
    session.waehle(frei);
    session.setNotiz(a.lehrauftragId(), "Anliegen A");

    session.entferne(a.lehrauftragId());

    assertThat(session.notiz(a.lehrauftragId())).isEmpty();
  }

  @Test
  void reset_verwirftAlleNotizen() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);
    session.waehle(frei);
    session.setNotiz(a.lehrauftragId(), "Anliegen A");

    session.reset(List.of(a));

    assertThat(session.notiz(a.lehrauftragId())).isEmpty();
  }

  @Test
  void reload_behaeltNotizenGueltigerWahlenUndVerwirftDieDerWeggefallenen() {
    SlotOption aSlot = slot(LocalTime.of(14, 0), false);
    SlotOption bSlot = slot(LocalTime.of(14, 15), false);
    LehrkraftOption a = lehrkraft("A", aSlot);
    LehrkraftOption b = lehrkraft("B", bSlot);
    BookingSession session = new BookingSession();
    session.reset(List.of(a, b));
    session.setActive(a);
    session.waehle(aSlot);
    session.setNotiz(a.lehrauftragId(), "Anliegen A");
    session.setActive(b);
    session.waehle(bSlot);
    session.setNotiz(b.lehrauftragId(), "Anliegen B");

    // A ist zwischenzeitlich vergeben, B bleibt frei.
    session.reload(
        List.of(
            lehrkraftMitId(a, new SlotOption(aSlot.terminId(), aSlot.zeit(), true)),
            lehrkraftMitId(b, bSlot)));

    assertThat(session.notiz(a.lehrauftragId())).isEmpty();
    assertThat(session.notiz(b.lehrauftragId())).isEqualTo("Anliegen B");
  }

  @Test
  void reset_verwirftAuswahlUndAktiveLehrkraft() {
    SlotOption frei = slot(LocalTime.of(14, 0), false);
    LehrkraftOption a = lehrkraft("A", frei);
    BookingSession session = new BookingSession();
    session.reset(List.of(a));
    session.setActive(a);
    session.waehle(frei);

    session.reset(List.of());

    assertThat(session.hatAuswahl()).isFalse();
    assertThat(session.active()).isNull();
    assertThat(session.hatOptionen()).isFalse();
  }

  /** Erzeugt eine Lehrkraft mit derselben lehrauftragId wie {@code vorlage}, aber neuen Slots. */
  private static LehrkraftOption lehrkraftMitId(LehrkraftOption vorlage, SlotOption... slots) {
    return new LehrkraftOption(
        vorlage.lehrauftragId(),
        vorlage.lehrerId(),
        vorlage.kuerzel(),
        vorlage.lehrerName(),
        vorlage.faecher(),
        List.of(slots));
  }
}
