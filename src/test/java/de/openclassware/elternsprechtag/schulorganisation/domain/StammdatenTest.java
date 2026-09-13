package de.openclassware.elternsprechtag.schulorganisation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Die Regeln der vier Stammdaten-Aggregate — plain JUnit, ohne Spring und ohne Datenbank.
 *
 * <p>Vier Wurzeln in einer Testklasse, weil sie dieselben zwei Regeln tragen (Pflichtangaben,
 * stillgelegt ist endgültig) und getrennte Dateien nur dieselbe Prüfung viermal buchstabierten.
 * Was sich je Aggregat unterscheidet, steht in der jeweiligen {@code @Nested}-Klasse.
 */
class StammdatenTest {

  @Nested
  class EineLehrkraft {

    @Test
    void brauchtVornameNachnameUndKuerzel() {
      assertThatThrownBy(() -> Lehrkraft.stelleEin(" ", "Berg", "BER"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> Lehrkraft.stelleEin("Anna", null, "BER"))
          .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> Lehrkraft.stelleEin("Anna", "Berg", ""))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wirdGetrimmtGespeichert() {
      Lehrkraft lehrkraft = Lehrkraft.stelleEin("  Anna ", " Berg", " BER ");

      assertThat(lehrkraft.vorname()).isEqualTo("Anna");
      assertThat(lehrkraft.nachname()).isEqualTo("Berg");
      assertThat(lehrkraft.kuerzel()).isEqualTo("BER");
    }

    @Test
    void heisstNachAussenVornameDannNachname() {
      assertThat(Lehrkraft.stelleEin("Anna", "Berg", "BER").anzeigeName()).isEqualTo("Anna Berg");
    }

    @Test
    void laesstSichUmbenennen() {
      Lehrkraft lehrkraft = Lehrkraft.stelleEin("Anna", "Berg", "BER");

      lehrkraft.benenne("Anna", "Berg-Adler", "BEA");

      assertThat(lehrkraft.nachname()).isEqualTo("Berg-Adler");
      assertThat(lehrkraft.kuerzel()).isEqualTo("BEA");
    }

    @Test
    void aendertSichNichtMehr_wennSieStillgelegtIst() {
      Lehrkraft lehrkraft = Lehrkraft.stelleEin("Anna", "Berg", "BER");
      lehrkraft.legeStill();

      assertThat(lehrkraft.istStillgelegt()).isTrue();
      assertThatThrownBy(() -> lehrkraft.benenne("Anna", "Adler", "ADL"))
          .isInstanceOf(StammdatumStillgelegtException.class);
    }

    @Test
    void laesstSichZweimalStilllegen_ohneDassEtwasPassiert() {
      Lehrkraft lehrkraft = Lehrkraft.stelleEin("Anna", "Berg", "BER");

      lehrkraft.legeStill();
      lehrkraft.legeStill();

      assertThat(lehrkraft.istStillgelegt()).isTrue();
    }

    @Test
    void beginntMitVersionNull_weilSieNochNieGespeichertWurde() {
      assertThat(Lehrkraft.stelleEin("Anna", "Berg", "BER").version()).isZero();
    }
  }

  @Nested
  class EineKlasse {

    @Test
    void brauchtEinenNamen() {
      assertThatThrownBy(() -> Klasse.richteEin("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laesstSichUmbenennen() {
      Klasse klasse = Klasse.richteEin("5a");

      klasse.benenne("6a");

      assertThat(klasse.name()).isEqualTo("6a");
    }

    @Test
    void aendertSichNichtMehr_wennSieStillgelegtIst() {
      Klasse klasse = Klasse.richteEin("5a");
      klasse.legeStill();

      assertThatThrownBy(() -> klasse.benenne("6a"))
          .isInstanceOf(StammdatumStillgelegtException.class);
    }
  }

  @Nested
  class EinFach {

    @Test
    void brauchtNameUndKuerzel() {
      assertThatThrownBy(() -> Fach.fuehreEin("", "D")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> Fach.fuehreEin("Deutsch", " "))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laesstSichUmbenennen() {
      Fach fach = Fach.fuehreEin("Deutsch", "D");

      fach.benenne("Deutsch als Zweitsprache", "DaZ");

      assertThat(fach.name()).isEqualTo("Deutsch als Zweitsprache");
      assertThat(fach.kuerzel()).isEqualTo("DaZ");
    }

    @Test
    void aendertSichNichtMehr_wennEsStillgelegtIst() {
      Fach fach = Fach.fuehreEin("Deutsch", "D");
      fach.legeStill();

      assertThatThrownBy(() -> fach.benenne("Englisch", "E"))
          .isInstanceOf(StammdatumStillgelegtException.class);
    }
  }

  @Nested
  class EinLehrauftrag {

    private final LehrkraftId lehrkraft = LehrkraftId.neu();
    private final KlasseId klasse = KlasseId.neu();
    private final FachId fach = FachId.neu();

    @Test
    void verweistAufLehrkraftKlasseUndFach() {
      Lehrauftrag auftrag = Lehrauftrag.erteile(lehrkraft, klasse, fach);

      assertThat(auftrag.lehrkraft()).isEqualTo(lehrkraft);
      assertThat(auftrag.klasse()).isEqualTo(klasse);
      assertThat(auftrag.fach()).isEqualTo(fach);
    }

    @Test
    void brauchtAlleDreiVerweise() {
      assertThatThrownBy(() -> Lehrauftrag.erteile(null, klasse, fach))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> Lehrauftrag.erteile(lehrkraft, null, fach))
          .isInstanceOf(NullPointerException.class);
      assertThatThrownBy(() -> Lehrauftrag.erteile(lehrkraft, klasse, null))
          .isInstanceOf(NullPointerException.class);
    }

    /**
     * Die Eindeutigkeit je (Lehrkraft, Klasse, Fach) ist ausdrücklich <b>keine</b> Invariante dieses
     * Aggregats: Sie spannt über alle Lehraufträge, und ein Aggregat, das sie prüfen wollte, müsste
     * dafür alle laden. Sie steht als Constraint in der Datenbank (Issue #140).
     */
    @Test
    void kenntDieEindeutigkeitNicht() {
      Lehrauftrag einer = Lehrauftrag.erteile(lehrkraft, klasse, fach);
      Lehrauftrag derselbeInhalt = Lehrauftrag.erteile(lehrkraft, klasse, fach);

      assertThat(derselbeInhalt.id()).isNotEqualTo(einer.id());
    }

    @Test
    void laesstSichStilllegen() {
      Lehrauftrag auftrag = Lehrauftrag.erteile(lehrkraft, klasse, fach);

      auftrag.legeStill();

      assertThat(auftrag.istStillgelegt()).isTrue();
    }
  }

  @Nested
  class EineWiederhergestellteLehrkraft {

    @Test
    void traegtIdUndVersionUnveraendert() {
      UUID id = UUID.randomUUID();

      Lehrkraft lehrkraft =
          Lehrkraft.rekonstruiere(LehrkraftId.von(id), 7L, "Anna", "Berg", "BER", false);

      assertThat(lehrkraft.id().wert()).isEqualTo(id);
      assertThat(lehrkraft.version()).isEqualTo(7L);
    }

    /** Rekonstruieren prüft keine Übergänge — sonst käme ein stillgelegter Datensatz nie zurück. */
    @Test
    void kommtAuchStillgelegtZurueck() {
      Lehrkraft lehrkraft =
          Lehrkraft.rekonstruiere(LehrkraftId.neu(), 3L, "Anna", "Berg", "BER", true);

      assertThat(lehrkraft.istStillgelegt()).isTrue();
    }
  }
}
