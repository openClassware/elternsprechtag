package de.openclassware.elternsprechtag.sprechtag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Die Invarianten des Sprechtags — plain JUnit, ohne Spring und ohne Datenbank.
 *
 * <p>Das ist der Punkt des Aggregats: Die fünf offenen {@code muss}-Fälle aus `ABDECKUNG.md`
 * (Z. 87–89, 92, 93) hatten alle dieselbe Ursache — es gab kein Objekt, das die Regel durchsetzt,
 * und damit auch keine Stelle, an der sie sich prüfen ließ. Jetzt gibt es beides.
 */
class SprechtagTest {

  private static final LocalDate DATUM = LocalDate.of(2026, 7, 20);
  private static final Zeitfenster NACHMITTAG =
      new Zeitfenster(LocalTime.of(14, 0), LocalTime.of(15, 0));
  private static final Slotdauer VIERTELSTUNDE = Slotdauer.vonMinuten(15);
  private static final KlasseId KLASSE_5A = KlasseId.von(java.util.UUID.randomUUID());
  private static final KlasseId KLASSE_7B = KlasseId.von(java.util.UUID.randomUUID());

  private static Sprechtag entwurf() {
    return Sprechtag.entwirf(
        "Frühling",
        "Aula",
        "Bitte pünktlich",
        Schulkontakt.von("Sekretariat, Tel. 0123 456789"),
        AccessToken.neu(),
        DATUM,
        NACHMITTAG,
        VIERTELSTUNDE,
        List.of(KLASSE_5A),
        ErinnerungsVorlauf.KEINE,
        Anmeldefrist.STANDARD);
  }

  private static Sprechtag veroeffentlicht() {
    Sprechtag sprechtag = entwurf();
    sprechtag.veroeffentliche();
    sprechtag.ereignisseAbholen();
    return sprechtag;
  }

  /** Abgeschlossen auf dem einzigen Weg, den es gibt: nach der Endzeit (#166). */
  private static Sprechtag abgeschlossen() {
    Sprechtag sprechtag = veroeffentlicht();
    sprechtag.schliesseAbWennVorbei(DATUM.plusDays(1).atStartOfDay());
    return sprechtag;
  }

  @Nested
  class Anlegen {

    @Test
    void jederSprechtagBeginntAlsEntwurf() {
      assertThat(entwurf().status()).isEqualTo(SprechtagStatus.ENTWURF);
    }

    @Test
    void ohneTitelIstErInKeinerListeZuFinden() {
      assertThatThrownBy(
              () ->
                  Sprechtag.entwirf(
                      "  ",
                      null,
                      null,
                      Schulkontakt.von("Sekretariat"),
                      AccessToken.neu(),
                      DATUM,
                      NACHMITTAG,
                      VIERTELSTUNDE,
                      List.of(KLASSE_5A),
                      ErinnerungsVorlauf.KEINE,
                      Anmeldefrist.STANDARD))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class Zeitstruktur {

    /** `ABDECKUNG.md` Z. 87 — Zeitfenster oder Slot-Dauer nach dem Veröffentlichen ändern. */
    @Test
    void zeitfensterNachDemVeroeffentlichenAendern_istGesperrt() {
      Sprechtag sprechtag = veroeffentlicht();

      assertThatThrownBy(
              () ->
                  sprechtag.legeZeitstrukturFest(
                      DATUM,
                      new Zeitfenster(LocalTime.of(16, 0), LocalTime.of(18, 0)),
                      VIERTELSTUNDE,
                      List.of(KLASSE_5A)))
          .isInstanceOf(ZeitstrukturEingefrorenException.class)
          .hasMessageContaining("Termine");

      assertThat(sprechtag.zeitfenster()).isEqualTo(NACHMITTAG);
    }

    /** `ABDECKUNG.md` Z. 87 — dasselbe für die Slot-Dauer. */
    @Test
    void slotdauerNachDemVeroeffentlichenAendern_istGesperrt() {
      Sprechtag sprechtag = veroeffentlicht();

      assertThatThrownBy(
              () ->
                  sprechtag.legeZeitstrukturFest(
                      DATUM, NACHMITTAG, Slotdauer.vonMinuten(30), List.of(KLASSE_5A)))
          .isInstanceOf(ZeitstrukturEingefrorenException.class);

      assertThat(sprechtag.slotdauer()).isEqualTo(VIERTELSTUNDE);
    }

    @Test
    void datumNachDemVeroeffentlichenAendern_istGesperrt() {
      Sprechtag sprechtag = veroeffentlicht();

      assertThatThrownBy(
              () ->
                  sprechtag.legeZeitstrukturFest(
                      DATUM.plusDays(7), NACHMITTAG, VIERTELSTUNDE, List.of(KLASSE_5A)))
          .isInstanceOf(ZeitstrukturEingefrorenException.class);
    }

    /** `ABDECKUNG.md` Z. 88 — Klasse hinzufügen, nachdem veröffentlicht wurde. */
    @Test
    void klasseHinzufuegenNachDemVeroeffentlichen_istGesperrt() {
      Sprechtag sprechtag = veroeffentlicht();

      assertThatThrownBy(
              () ->
                  sprechtag.legeZeitstrukturFest(
                      DATUM, NACHMITTAG, VIERTELSTUNDE, List.of(KLASSE_5A, KLASSE_7B)))
          .isInstanceOf(ZeitstrukturEingefrorenException.class);

      assertThat(sprechtag.klassen()).containsExactly(KLASSE_5A);
    }

    /**
     * `ABDECKUNG.md` Z. 89 — Klasse entfernen, deren Eltern gebucht haben. Gebucht wird erst nach
     * dem Veröffentlichen; dieselbe Sperre deckt den Fall deshalb mit ab.
     */
    @Test
    void klasseEntfernenNachDemVeroeffentlichen_istGesperrt() {
      Sprechtag sprechtag = veroeffentlicht();

      assertThatThrownBy(
              () -> sprechtag.legeZeitstrukturFest(DATUM, NACHMITTAG, VIERTELSTUNDE, List.of()))
          .isInstanceOf(ZeitstrukturEingefrorenException.class);
    }

    /**
     * Die Oberfläche schickt beim Speichern immer das ganze Formular. Wer nur den Titel ändert,
     * legt die unveränderte Zeitstruktur erneut vor — das ist keine Änderung und darf nicht
     * scheitern.
     */
    @Test
    void unveraenderteZeitstrukturErneutVorlegen_istErlaubt() {
      Sprechtag sprechtag = veroeffentlicht();

      assertThatCode(
              () ->
                  sprechtag.legeZeitstrukturFest(
                      DATUM, NACHMITTAG, VIERTELSTUNDE, List.of(KLASSE_5A)))
          .doesNotThrowAnyException();
    }

    /** Die Klassenliste ist fachlich eine Menge — ihre Reihenfolge ist keine Änderung. */
    @Test
    void umsortierteKlassenlisteIstKeineAenderung() {
      Sprechtag sprechtag = entwurf();
      sprechtag.legeZeitstrukturFest(
          DATUM, NACHMITTAG, VIERTELSTUNDE, List.of(KLASSE_5A, KLASSE_7B));
      sprechtag.veroeffentliche();

      assertThatCode(
              () ->
                  sprechtag.legeZeitstrukturFest(
                      DATUM, NACHMITTAG, VIERTELSTUNDE, List.of(KLASSE_7B, KLASSE_5A)))
          .doesNotThrowAnyException();
    }

    @Test
    void imEntwurfIstAllesAenderbar() {
      Sprechtag sprechtag = entwurf();

      sprechtag.legeZeitstrukturFest(
          DATUM.plusDays(1),
          new Zeitfenster(LocalTime.of(16, 0), LocalTime.of(18, 0)),
          Slotdauer.vonMinuten(20),
          List.of(KLASSE_7B));

      assertThat(sprechtag.datum()).isEqualTo(DATUM.plusDays(1));
      assertThat(sprechtag.slotdauer()).isEqualTo(Slotdauer.vonMinuten(20));
      assertThat(sprechtag.klassen()).containsExactly(KLASSE_7B);
    }
  }

  @Nested
  class Beschreibung {

    /** `ABDECKUNG.md` Z. 90 — Titel, Ort und Hinweistext bleiben nach dem Veröffentlichen erlaubt. */
    @Test
    void titelUndOrtNachDemVeroeffentlichenAendern_bleibtErlaubt() {
      Sprechtag sprechtag = veroeffentlicht();

      sprechtag.beschreibeNeu(
          "Frühling (Raumänderung)",
          "Turnhalle",
          "Neuer Hinweis",
          Schulkontakt.von("Sekretariat"),
          sprechtag.accessToken());

      assertThat(sprechtag.titel()).isEqualTo("Frühling (Raumänderung)");
      assertThat(sprechtag.ort()).isEqualTo("Turnhalle");
    }

    /** `ABDECKUNG.md` Z. 93 — ein abgesagter Sprechtag wird nicht über „Speichern" wiederbelebt. */
    @Test
    void abgesagterSprechtagWirdNichtMehrBearbeitet() {
      Sprechtag sprechtag = veroeffentlicht();
      sprechtag.sageAb();

      assertThatThrownBy(
              () ->
                  sprechtag.beschreibeNeu(
                      "Doch wieder da",
                      null,
                      null,
                      Schulkontakt.von("Sekretariat"),
                      sprechtag.accessToken()))
          .isInstanceOf(StatusuebergangException.class);
    }

    @Test
    void abgeschlossenerSprechtagWirdNichtMehrBearbeitet() {
      Sprechtag sprechtag = abgeschlossen();

      assertThatThrownBy(
              () ->
                  sprechtag.legeZeitstrukturFest(
                      DATUM.plusDays(1), NACHMITTAG, VIERTELSTUNDE, List.of(KLASSE_5A)))
          .isInstanceOf(StatusuebergangException.class);
    }
  }

  @Nested
  class Erinnerung {

    @Test
    void standardIstKeineErinnerung() {
      assertThat(entwurf().erinnerungsVorlauf()).isEqualTo(ErinnerungsVorlauf.KEINE);
    }

    /** Nicht Teil der Zeitstruktur — bleibt anders als diese auch nach dem Veröffentlichen offen. */
    @Test
    void bleibtNachDemVeroeffentlichenAenderbar() {
      Sprechtag sprechtag = veroeffentlicht();

      sprechtag.aendereErinnerungsVorlauf(ErinnerungsVorlauf.ZWEI_TAGE);

      assertThat(sprechtag.erinnerungsVorlauf()).isEqualTo(ErinnerungsVorlauf.ZWEI_TAGE);
    }

    @Test
    void abgesagterSprechtagAendertDenErinnerungsVorlaufNichtMehr() {
      Sprechtag sprechtag = veroeffentlicht();
      sprechtag.sageAb();

      assertThatThrownBy(
              () -> sprechtag.aendereErinnerungsVorlauf(ErinnerungsVorlauf.EIN_TAG))
          .isInstanceOf(StatusuebergangException.class);
    }

    @Test
    void abgeschlossenerSprechtagAendertDenErinnerungsVorlaufNichtMehr() {
      Sprechtag sprechtag = abgeschlossen();

      assertThatThrownBy(
              () -> sprechtag.aendereErinnerungsVorlauf(ErinnerungsVorlauf.EIN_TAG))
          .isInstanceOf(StatusuebergangException.class);
    }
  }

  /** Issue #122 — wie lange der Elternlink Buchungen annimmt. */
  @Nested
  class Anmeldung {

    @Test
    void standardIstDerVortag() {
      assertThat(entwurf().anmeldeschluss()).isEqualTo(DATUM.minusDays(1));
    }

    @Test
    void anmeldeschlussIstDasDatumMinusDieFrist() {
      Sprechtag sprechtag = entwurf();
      sprechtag.aendereAnmeldefrist(Anmeldefrist.vonTagen(3));

      assertThat(sprechtag.anmeldeschluss()).isEqualTo(DATUM.minusDays(3));
    }

    /** „Anmeldung bis 19.03." meint den ganzen 19.03. */
    @Test
    void amAnmeldeschlussSelbst_nimmtErElternbuchungenNochAn() {
      Sprechtag sprechtag = veroeffentlicht();

      assertThat(sprechtag.nimmtElternbuchungenAn(sprechtag.anmeldeschluss())).isTrue();
    }

    @Test
    void amTagNachDemAnmeldeschluss_nimmtErKeineElternbuchungenMehrAn() {
      Sprechtag sprechtag = veroeffentlicht();

      assertThat(sprechtag.nimmtElternbuchungenAn(sprechtag.anmeldeschluss().plusDays(1)))
          .isFalse();
    }

    @Test
    void einEntwurfNimmtKeineElternbuchungenAn() {
      assertThat(entwurf().nimmtElternbuchungenAn(DATUM.minusDays(10))).isFalse();
    }

    @Test
    void einAbgesagterSprechtagNimmtKeineElternbuchungenAn() {
      Sprechtag sprechtag = veroeffentlicht();
      sprechtag.sageAb();

      assertThat(sprechtag.nimmtElternbuchungenAn(DATUM.minusDays(10))).isFalse();
    }

    @Test
    void einAbgeschlossenerSprechtagNimmtKeineElternbuchungenAn() {
      assertThat(abgeschlossen().nimmtElternbuchungenAn(DATUM.minusDays(10))).isFalse();
    }

    /** Die Anmeldung verlängern, wenn sich zu wenige eingetragen haben. */
    @Test
    void eineAbgelaufeneFristLaesstSichNachDemVeroeffentlichenWiederOeffnen() {
      Sprechtag sprechtag = veroeffentlicht();
      LocalDate heute = DATUM.minusDays(1);
      sprechtag.aendereAnmeldefrist(Anmeldefrist.vonTagen(5));
      assertThat(sprechtag.nimmtElternbuchungenAn(heute)).isFalse();

      sprechtag.aendereAnmeldefrist(Anmeldefrist.vonTagen(0));

      assertThat(sprechtag.nimmtElternbuchungenAn(heute)).isTrue();
    }

    @Test
    void abgesagterSprechtagAendertDieAnmeldefristNichtMehr() {
      Sprechtag sprechtag = veroeffentlicht();
      sprechtag.sageAb();

      assertThatThrownBy(() -> sprechtag.aendereAnmeldefrist(Anmeldefrist.vonTagen(0)))
          .isInstanceOf(StatusuebergangException.class);
    }

    @Test
    void abgeschlossenerSprechtagAendertDieAnmeldefristNichtMehr() {
      Sprechtag sprechtag = abgeschlossen();

      assertThatThrownBy(() -> sprechtag.aendereAnmeldefrist(Anmeldefrist.vonTagen(0)))
          .isInstanceOf(StatusuebergangException.class);
    }
  }

  @Nested
  class Statusuebergaenge {

    @Test
    void veroeffentlichenMeldetEinEreignis() {
      Sprechtag sprechtag = entwurf();

      sprechtag.veroeffentliche();

      assertThat(sprechtag.status()).isEqualTo(SprechtagStatus.VEROEFFENTLICHT);
      assertThat(sprechtag.ereignisseAbholen())
          .containsExactly(new SprechtagVeroeffentlicht(sprechtag.id()));
    }

    @Test
    void absagenMeldetEinEreignis() {
      Sprechtag sprechtag = veroeffentlicht();

      sprechtag.sageAb();

      assertThat(sprechtag.status()).isEqualTo(SprechtagStatus.ABGESAGT);
      assertThat(sprechtag.ereignisseAbholen())
          .containsExactly(new SprechtagAbgesagt(sprechtag.id()));
    }

    @Test
    void abgesagterSprechtagLaesstSichNichtErneutVeroeffentlichen() {
      Sprechtag sprechtag = veroeffentlicht();
      sprechtag.sageAb();

      assertThatThrownBy(sprechtag::veroeffentliche).isInstanceOf(StatusuebergangException.class);
    }

    /** `ABDECKUNG.md` Z. 91 — zu früh veröffentlicht, noch keine Buchung. */
    @Test
    void zurueckAufEntwurfOhneBuchung_istErlaubt() {
      Sprechtag sprechtag = veroeffentlicht();

      sprechtag.nimmVeroeffentlichungZurueck(false);

      assertThat(sprechtag.status()).isEqualTo(SprechtagStatus.ENTWURF);
    }

    /** `ABDECKUNG.md` Z. 92 — zurück auf Entwurf, obwohl gebucht wurde. */
    @Test
    void zurueckAufEntwurfMitBuchung_verweistAufDieAbsage() {
      Sprechtag sprechtag = veroeffentlicht();

      assertThatThrownBy(() -> sprechtag.nimmVeroeffentlichungZurueck(true))
          .isInstanceOf(SprechtagHatBuchungenException.class)
          .hasMessageContaining("sagt ihn ab");

      assertThat(sprechtag.status()).isEqualTo(SprechtagStatus.VEROEFFENTLICHT);
    }

    @Test
    void einEntwurfIstNichtZurueckzunehmen() {
      Sprechtag sprechtag = entwurf();

      assertThatThrownBy(() -> sprechtag.nimmVeroeffentlichungZurueck(false))
          .isInstanceOf(StatusuebergangException.class);
    }
  }

  /** `ABDECKUNG.md` Z. 431 — der Sprechtag schließt sich nach Ablauf der Endzeit selbst ab. */
  @Nested
  class AutomatischerAbschluss {

    @Test
    void nachDerEndzeit_wirdAbgeschlossenOhneEreignis() {
      Sprechtag sprechtag = veroeffentlicht();

      boolean abgeschlossen = sprechtag.schliesseAbWennVorbei(DATUM.atTime(15, 1));

      assertThat(abgeschlossen).isTrue();
      assertThat(sprechtag.status()).isEqualTo(SprechtagStatus.ABGESCHLOSSEN);
      assertThat(sprechtag.ereignisseAbholen()).isEmpty();
    }

    /**
     * Vor der Endzeit gibt es keinen Abschluss — und weil es keinen Handabschluss gibt, auch sonst
     * keinen Weg dorthin (#166).
     */
    @Test
    void amTagSelbstVorDerEndzeit_bleibtVeroeffentlicht() {
      Sprechtag sprechtag = veroeffentlicht();

      boolean abgeschlossen = sprechtag.schliesseAbWennVorbei(DATUM.atTime(14, 59));

      assertThat(abgeschlossen).isFalse();
      assertThat(sprechtag.status()).isEqualTo(SprechtagStatus.VEROEFFENTLICHT);
    }

    /** Das Read-Modell darf veraltet sein — was nicht mehr veröffentlicht ist, übergeht der Job. */
    @Test
    void nurVeroeffentlichteWerdenAbgeschlossen_derRestBleibtOhneFehler() {
      Sprechtag entwurf = entwurf();
      Sprechtag abgesagt = veroeffentlicht();
      abgesagt.sageAb();
      Sprechtag abgeschlossen = abgeschlossen();
      var danach = DATUM.plusDays(1).atStartOfDay();

      assertThat(entwurf.schliesseAbWennVorbei(danach)).isFalse();
      assertThat(abgesagt.schliesseAbWennVorbei(danach)).isFalse();
      assertThat(abgeschlossen.schliesseAbWennVorbei(danach)).isFalse();
      assertThat(entwurf.status()).isEqualTo(SprechtagStatus.ENTWURF);
      assertThat(abgesagt.status()).isEqualTo(SprechtagStatus.ABGESAGT);
      assertThat(abgeschlossen.status()).isEqualTo(SprechtagStatus.ABGESCHLOSSEN);
    }
  }

  @Nested
  class Slots {

    @Test
    void jeSlotEinZeitraum() {
      List<Zeitraum> slots = entwurf().slots();

      assertThat(slots).hasSize(4);
      assertThat(slots.get(0).beginn()).isEqualTo(DATUM.atTime(14, 0));
      assertThat(slots.get(0).ende()).isEqualTo(DATUM.atTime(14, 15));
      assertThat(slots.get(3).beginn()).isEqualTo(DATUM.atTime(14, 45));
    }

    /** `ABDECKUNG.md` Z. 96 — ein Rest-Slot, der nicht mehr voll passt, entfällt. */
    @Test
    void restSlotDerNichtMehrVollPasstEntfaellt() {
      Sprechtag sprechtag = entwurf();
      sprechtag.legeZeitstrukturFest(
          DATUM,
          new Zeitfenster(LocalTime.of(14, 0), LocalTime.of(14, 50)),
          Slotdauer.vonMinuten(20),
          List.of(KLASSE_5A));

      // 14:00 und 14:20 passen; 14:40 + 20 = 15:00 liegt hinter 14:50.
      assertThat(sprechtag.slots()).hasSize(2);
    }

    @Test
    void zeitfensterKuerzerAlsEinSlot_ergibtKeinenSlot() {
      Sprechtag sprechtag = entwurf();
      sprechtag.legeZeitstrukturFest(
          DATUM,
          new Zeitfenster(LocalTime.of(14, 0), LocalTime.of(14, 10)),
          VIERTELSTUNDE,
          List.of(KLASSE_5A));

      assertThat(sprechtag.slots()).isEmpty();
    }
  }

  @Nested
  class Duplizieren {

    @Test
    void kopieIstEinEntwurfMitEigenemToken() {
      Sprechtag original = veroeffentlicht();

      Sprechtag kopie = original.dupliziere(AccessToken.neu());

      assertThat(kopie.status()).isEqualTo(SprechtagStatus.ENTWURF);
      assertThat(kopie.id()).isNotEqualTo(original.id());
      assertThat(kopie.accessToken()).isNotEqualTo(original.accessToken());
      assertThat(kopie.titel()).isEqualTo(original.titel());
      assertThat(kopie.klassen()).isEqualTo(original.klassen());
      assertThat(kopie.schulkontakt()).isEqualTo(original.schulkontakt());
    }

    /** Relativ gespeichert, ist die Frist für jedes neue Datum richtig — keine tot geborene Kopie. */
    @Test
    void kopieUebernimmtDieAnmeldefrist() {
      Sprechtag original = entwurf();
      original.aendereAnmeldefrist(Anmeldefrist.vonTagen(4));

      Sprechtag kopie = original.dupliziere(AccessToken.neu());

      assertThat(kopie.anmeldefrist()).isEqualTo(Anmeldefrist.vonTagen(4));
    }
  }

  @Nested
  class Werte {

    @Test
    void zeitfensterOhneDauerGibtEsNicht() {
      assertThatThrownBy(() -> new Zeitfenster(LocalTime.of(15, 0), LocalTime.of(15, 0)))
          .isInstanceOf(IllegalArgumentException.class);
    }

    /** `ABDECKUNG.md` Z. 95 — eine leere Slot-Dauer kommt gar nicht erst durch. */
    @Test
    void slotdauerMussPositivSein() {
      assertThatThrownBy(() -> Slotdauer.vonMinuten(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void schulkontaktDarfNichtLeerSein() {
      assertThatThrownBy(() -> Schulkontakt.von("   \n  "))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void schulkontaktWirdGetrimmtGespeichert() {
      assertThat(Schulkontakt.von("  Sekretariat  ").text()).isEqualTo("Sekretariat");
    }

    @Test
    void leeresZugangsTokenGibtKeinenZugang() {
      assertThatThrownBy(() -> AccessToken.von(" ")).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
