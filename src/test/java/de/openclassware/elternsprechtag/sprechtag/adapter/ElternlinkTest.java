package de.openclassware.elternsprechtag.sprechtag.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Der Elternlink aus der konfigurierten öffentlichen Adresse (Issue #109). Die Prüfung im
 * Konstruktor ist zugleich die Startprüfung: {@link Elternlink} ist ein Singleton, und ein Fehler
 * hier lässt den Spring-Kontext nicht hochkommen.
 */
class ElternlinkTest {

  @Test
  void zu_bautDenAbsolutenLinkAusAdresseUndToken() {
    Elternlink link = new Elternlink("https://elternsprechtag.schule.de");

    assertThat(link.zu("Ab3x")).isEqualTo("https://elternsprechtag.schule.de/elternsprechtag/Ab3x");
  }

  @Test
  void zu_abschliessenderSchraegstrich_faelltWeg() {
    Elternlink link = new Elternlink(" https://schule.de/ ");

    assertThat(link.zu("Ab3x")).isEqualTo("https://schule.de/elternsprechtag/Ab3x");
  }

  @Test
  void zu_unterpfadUndPort_bleibenErhalten() {
    Elternlink link = new Elternlink("http://localhost:8080/sprechtag");

    assertThat(link.zu("Ab3x")).isEqualTo("http://localhost:8080/sprechtag/elternsprechtag/Ab3x");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void ohneAdresse_scheitertMitHinweisAufDieEinstellung(String adresse) {
    assertThatThrownBy(() -> new Elternlink(adresse))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("elternsprechtag.oeffentliche-url")
        .hasMessageContaining("ELTERNSPRECHTAG_OEFFENTLICHE_URL");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "elternsprechtag.schule.de",
        "/elternsprechtag",
        "ftp://schule.de",
        "https://",
        "https://schule.de?x=1",
        "https://schule.de#anker",
        "https://schule de"
      })
  void unbrauchbareAdresse_scheitert(String adresse) {
    assertThatThrownBy(() -> new Elternlink(adresse))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("elternsprechtag.oeffentliche-url");
  }
}
