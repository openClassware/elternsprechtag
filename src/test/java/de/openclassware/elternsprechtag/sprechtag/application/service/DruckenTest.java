package de.openclassware.elternsprechtag.sprechtag.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.SprechtagKontextTestConfig;
import de.openclassware.elternsprechtag.sprechtag.AbstractServiceTest;
import de.openclassware.elternsprechtag.sprechtag.ServiceTest;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsAnfrage;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Buchen.BuchungsWunsch;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Drucken;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Drucken.Datei;
import de.openclassware.elternsprechtag.sprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.domain.Termin;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Der Use Case Drucken gegen eine echte Datenbank und mit echtem PDF-Adapter (Issue #120): Er lädt
 * selbst und führt zusammen — Lehrkräfte aus der Schulorganisation, Slots und geltende Buchungen
 * aus dem Query-Port.
 *
 * <p>Wie das Blatt aussieht, prüft {@code PdfTagesplandruckTest} ohne Spring; hier geht es darum,
 * welche Slots und welche Lehrkräfte darauf landen.
 */
@ServiceTest
@Import(SprechtagKontextTestConfig.class)
class DruckenTest extends AbstractServiceTest {

  // In der Zukunft: Der Elternlink bucht nur bis zum Anmeldeschluss (Issue #122).
  private static final LocalDate DATE = LocalDate.of(2099, 7, 20);
  private static final DateTimeFormatter STAND = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

  @Autowired private Drucken drucken;

  private record Fixture(Sprechtag sprechtag, UUID lehrauftrag, UUID berg) {}

  /** Ein veröffentlichter Sprechtag 14–15 Uhr in 15-Minuten-Slots, zwei Lehrkräfte in 5a. */
  private Fixture veroeffentlicht() {
    UUID klasse = persistKlasse("5a");
    UUID berg = persistLehrkraft("Anna", "Berg", "BER");
    UUID adler = persistLehrkraft("Carl", "Adler", "ADL");
    UUID deutsch = persistFach("Deutsch", "D");
    UUID lehrauftrag = persistLehrauftrag(berg, klasse, deutsch);
    persistLehrauftrag(adler, klasse, persistFach("Mathematik", "M"));
    Sprechtag sprechtag =
        persistSprechtag(
            "Herbst", DATE, LocalTime.of(14, 0), LocalTime.of(15, 0), 15,
            SprechtagStatus.ENTWURF, klasse);
    veroeffentlichen.veroeffentliche(sprechtag.id().wert());
    return new Fixture(sprechtag, lehrauftrag, berg);
  }

  private void buche(UUID lehrauftrag, Termin termin, String name) {
    buchen.buchen(
        new BuchungsAnfrage(
            "Eltern " + name,
            "Kind " + name,
            name.toLowerCase() + "@example.com",
            List.of(new BuchungsWunsch(lehrauftrag, termin.id().wert(), null))));
  }

  @Test
  void druckePlaene_unbekannterSprechtag_liefertNichts() {
    assertThat(drucken.druckePlaene(UUID.randomUUID())).isEmpty();
  }

  @Test
  void druckePlaene_jedeLehrkraftBekommtIhrBlatt_mitGebuchtenUndFreienSlots() throws IOException {
    Fixture f = veroeffentlicht();
    List<Termin> slots = termineVon(f.berg());
    buche(f.lehrauftrag(), slots.get(0), "Mueller");
    buche(f.lehrauftrag(), slots.get(1), "Schmidt");
    storniere(buchung -> buchung.familie().schuelerName().equals("Kind Schmidt"));
    entfallenLassen.entfallenLassen(List.of(slots.get(2).id().wert()));
    LocalDateTime vorher = LocalDateTime.now();

    Datei datei = drucken.druckePlaene(f.sprechtag().id().wert()).orElseThrow();

    // Minutengenau: Der Stand ist die Minute vor oder die nach dem Aufruf.
    String stand =
        datei.name().startsWith(vorher.format(STAND))
            ? vorher.format(STAND)
            : LocalDateTime.now().format(STAND);
    assertThat(datei.name()).isEqualTo(stand + "_Herbst_Tagesplaene.zip");
    Map<String, String> blaetter = blaetter(datei.inhalt());
    // Nach Nachname sortiert, wie in der Auswertung — Adler ohne Buchung bekommt sein Blatt.
    assertThat(blaetter.keySet())
        .containsExactly(stand + "_Herbst_ADL.pdf", stand + "_Herbst_BER.pdf");

    String berg = blaetter.get(stand + "_Herbst_BER.pdf");
    String standImKopf = "Stand: " + Formats.dateTimeShort(LocalDateTime.parse(stand, STAND));
    assertThat(berg)
        .contains("Berg", standImKopf)
        .contains("14:00", "Kind Mueller", "Eltern Mueller")
        // Der stornierte Slot ist wieder frei, der entfallene fehlt ganz.
        .contains("14:15", "14:45")
        .doesNotContain("Kind Schmidt", "14:30")
        .doesNotContain("@example.com");
    assertThat(berg.split("frei", -1)).hasSize(3);

    String adler = blaetter.get(stand + "_Herbst_ADL.pdf");
    assertThat(adler).contains("Adler", "14:00", "14:15", "14:30", "14:45");
    assertThat(adler.split("frei", -1)).hasSize(5);
  }

  /** Name je ZIP-Eintrag, dazu der Text des PDFs. */
  private static Map<String, String> blaetter(byte[] zip) throws IOException {
    Map<String, String> blaetter = new LinkedHashMap<>();
    try (ZipInputStream eingang = new ZipInputStream(new ByteArrayInputStream(zip))) {
      for (ZipEntry eintrag = eingang.getNextEntry(); eintrag != null;
          eintrag = eingang.getNextEntry()) {
        PdfReader leser = new PdfReader(eingang.readAllBytes());
        blaetter.put(eintrag.getName(), new PdfTextExtractor(leser).getTextFromPage(1));
      }
    }
    return blaetter;
  }
}
