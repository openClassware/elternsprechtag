package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * Der tägliche Abschluss-Lauf (Issue #124) gegen eine echte Datenbank: Kandidaten kommen aus dem
 * Query-Port, entschieden wird am {@code Sprechtag}-Aggregat. Die Grenzfälle der Regel — Endzeit
 * am selben Tag, Endzustände — stehen ohne Spring in {@code SprechtagTest}.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class AbschliessenTest extends AbstractServiceTest {

  private Sprechtag sprechtag(LocalDate datum, SprechtagStatus status) {
    return persistSprechtag(
        "Frühling",
        datum,
        LocalTime.of(14, 0),
        LocalTime.of(15, 0),
        15,
        status,
        persistKlasse("5a-" + datum + "-" + status));
  }

  @Test
  void schliesseVorbeiAb_schliesstDenVergangenenSprechtagAbUndZaehltIhn() {
    Sprechtag gestern = sprechtag(LocalDate.now().minusDays(1), SprechtagStatus.VEROEFFENTLICHT);

    int anzahl = abschliessen.schliesseVorbeiAb();

    assertThat(anzahl).isEqualTo(1);
    assertThat(ladeSprechtag(gestern.id().wert()).status()).isEqualTo(SprechtagStatus.ABGESCHLOSSEN);
  }

  @Test
  void schliesseVorbeiAb_laesstKuenftigeUndAbgesagteSprechtageStehen() {
    Sprechtag morgen = sprechtag(LocalDate.now().plusDays(1), SprechtagStatus.VEROEFFENTLICHT);
    Sprechtag abgesagt = sprechtag(LocalDate.now().minusDays(1), SprechtagStatus.ABGESAGT);

    int anzahl = abschliessen.schliesseVorbeiAb();

    assertThat(anzahl).isZero();
    assertThat(ladeSprechtag(morgen.id().wert()).status()).isEqualTo(SprechtagStatus.VEROEFFENTLICHT);
    assertThat(ladeSprechtag(abgesagt.id().wert()).status()).isEqualTo(SprechtagStatus.ABGESAGT);
  }
}
