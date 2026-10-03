package de.openclassware.elternsprechtag.sprechtag.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Drucken.Datei;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Blatt;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Kopf;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Zeile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

/**
 * Der PDF-Adapter ohne Spring und ohne Datenbank: Was auf dem Blatt steht, wird über die
 * Textextraktion von OpenPDF zurückgelesen. Geprüft wird der Inhalt, nicht das Layout — ein
 * Vergleich der Bytes zerbräche an Zeitstempel und Schriftmetrik.
 */
class PdfTagesplandruckTest {

  private static final LocalDateTime STAND = LocalDateTime.of(2026, 10, 2, 14, 37);
  private static final Kopf KOPF =
      new Kopf("Herbstsprechtag", LocalDate.of(2026, 11, 12), "Aula Nordbau", null);
  private static final String PDF_MUELLER = "2026-10-02_1437_Herbstsprechtag_MÜL.pdf";

  private final PdfTagesplandruck druck = new PdfTagesplandruck();

  private static Zeile gebucht(LocalTime zeit, String schueler, String eltern, String notiz) {
    return new Zeile(zeit, false, schueler, "5a", "Deutsch", eltern, notiz, null);
  }

  private static Zeile frei(LocalTime zeit) {
    return new Zeile(zeit, true, null, null, null, null, null, null);
  }

  private static Blatt mueller(Zeile... zeilen) {
    return new Blatt("MÜL", "Müller, Anna", List.of(zeilen));
  }

  /** Der Text des einen PDFs im ZIP — für die Tests, die nur ein Blatt drucken. */
  private String einzigesBlatt(Blatt blatt) throws IOException {
    return einzigesBlatt(KOPF, blatt);
  }

  private String einzigesBlatt(Kopf kopf, Blatt blatt) throws IOException {
    Map<String, byte[]> eintraege = entpacke(druck.gebuendelt(kopf, List.of(blatt), STAND).inhalt());
    assertThat(eintraege).hasSize(1);
    return text(eintraege.values().iterator().next());
  }

  @Test
  void blatt_traegtKopfStandUndAlleSlots() throws IOException {
    String text =
        einzigesBlatt(
            mueller(
                gebucht(LocalTime.of(15, 0), "Lena Schmidt", "Petra Schmidt", "Mathe-Sorgen"),
                frei(LocalTime.of(15, 10))));

    assertThat(text)
        .contains("Müller, Anna", "MÜL")
        .contains("Herbstsprechtag", "12. November 2026", "Ort: Aula Nordbau")
        .contains("Stand: 02.10.2026, 14:37")
        .contains("15:00", "Lena Schmidt", "5a", "Deutsch", "Petra Schmidt", "Mathe-Sorgen")
        .contains("15:10", "frei")
        .contains("Eigene Notizen")
        .contains("Seite 1 von 1");
  }

  @Test
  void ohneOrt_entfaelltDieOrtsangabe() throws IOException {
    Kopf ohneOrt = new Kopf("Herbstsprechtag", LocalDate.of(2026, 11, 12), null, null);

    assertThat(einzigesBlatt(ohneOrt, mueller())).contains("Herbstsprechtag").doesNotContain("Ort:");
  }

  @Test
  void entfernteAngaben_zeigenDenVermerkStattDerNotiz() throws IOException {
    Blatt blatt =
        mueller(
            new Zeile(LocalTime.of(15, 0), false, "Schüler 1", "5a", "D", "Eltern 1", null,
                LocalDate.of(2026, 9, 30)));

    // Der Vermerk bricht in der schmalen Notizspalte um.
    String text = einzigesBlatt(blatt).replaceAll("\\s+", " ");

    assertThat(text).contains("Angaben entfernt am 30. September 2026");
  }

  @Test
  void anonymisierterSprechtag_erklaertDiePlatzhalter() throws IOException {
    Kopf anonymisiert =
        new Kopf("Herbstsprechtag", LocalDate.of(2026, 11, 12), null, LocalDate.of(2027, 3, 1));

    assertThat(einzigesBlatt(anonymisiert, mueller()))
        .contains("Personenbezogene Angaben am 1. März 2027 entfernt");
  }

  @Test
  void blattOhneSlots_sagtDass() throws IOException {
    assertThat(einzigesBlatt(mueller())).contains("Müller, Anna", "Keine Termine", "Seite 1 von 1");
  }

  @Test
  void namenAusserhalbVonWinAnsi_kommenUnversehrtAufsBlatt() throws IOException {
    Blatt blatt = mueller(gebucht(LocalTime.of(15, 0), "Ayşe Yılmaz", "Łukasz Wójcik", null));

    assertThat(einzigesBlatt(blatt)).contains("Ayşe Yılmaz", "Łukasz Wójcik");
  }

  @Test
  void vieleZeilen_brechenUmMitKopfzeileUndSeitenzahlAufJederSeite() throws IOException {
    Zeile[] zeilen =
        IntStream.range(0, 30)
            .mapToObj(
                i ->
                    gebucht(
                        LocalTime.of(14, 0).plusMinutes(10L * i),
                        "Kind " + i,
                        "Eltern " + i,
                        "Eine längere Notiz, die in ihrer Zelle umbrechen muss, Nummer " + i))
            .toArray(Zeile[]::new);
    byte[] pdf =
        entpacke(druck.gebuendelt(KOPF, List.of(mueller(zeilen)), STAND).inhalt())
            .get(PDF_MUELLER);

    PdfReader leser = new PdfReader(pdf);
    int seiten = leser.getNumberOfPages();
    assertThat(seiten).isGreaterThan(1);
    PdfTextExtractor extraktor = new PdfTextExtractor(leser);
    for (int seite = 1; seite <= seiten; seite++) {
      assertThat(extraktor.getTextFromPage(seite))
          .contains("Schüler/in", "Eigene Notizen")
          .contains("Müller, Anna · Stand: 02.10.2026, 14:37")
          .contains("Seite " + seite + " von " + seiten);
    }
    assertThat(text(pdf)).contains("Kind 0", "Kind 29");
  }

  @Test
  void zip_enthaeltJeBlattEinPdf_auchEinLeeres() throws IOException {
    Blatt berg = new Blatt("BER", "Berg, Tom", List.of());

    Datei datei =
        druck.gebuendelt(
            KOPF,
            List.of(mueller(gebucht(LocalTime.of(15, 0), "Lena Schmidt", "Petra", null)), berg),
            STAND);

    assertThat(datei.name()).isEqualTo("2026-10-02_1437_Herbstsprechtag_Tagesplaene.zip");
    assertThat(datei.medientyp()).isEqualTo("application/zip");
    Map<String, byte[]> eintraege = entpacke(datei.inhalt());
    assertThat(eintraege.keySet())
        .containsExactly(PDF_MUELLER, "2026-10-02_1437_Herbstsprechtag_BER.pdf");
    assertThat(text(eintraege.get("2026-10-02_1437_Herbstsprechtag_BER.pdf")))
        .contains("Berg, Tom", "Keine Termine");
  }

  @Test
  void zip_machtDoppelteNamenEindeutig() throws IOException {
    Datei datei = druck.gebuendelt(KOPF, List.of(mueller(), mueller()), STAND);

    assertThat(entpacke(datei.inhalt()).keySet())
        .containsExactly(PDF_MUELLER, "2026-10-02_1437_Herbstsprechtag_MÜL-2.pdf");
  }

  @Test
  void dateiname_ohneKuerzel_nimmtDenNamen() {
    Blatt ohneKuerzel = new Blatt(null, "Kramer, Jan", List.of());

    assertThat(Dateinamen.plan(KOPF, ohneKuerzel, STAND))
        .isEqualTo("2026-10-02_1437_Herbstsprechtag_Kramer-Jan.pdf");
  }

  @Test
  void dateinamen_kuerzenDenTitelUndErsetzenUnzulaessiges() {
    String langerTitel = "Elternsprechtag: Jahrgänge 5/6 * Schuljahr 2026/27 — zweiter Termin";
    Kopf kopf = new Kopf(langerTitel, LocalDate.of(2026, 11, 12), null, null);
    Blatt blatt = new Blatt("Ö", "Egal", List.of());

    String name = Dateinamen.plan(kopf, blatt, STAND);

    assertThat(name).isEqualTo("2026-10-02_1437_Elternsprechtag-Jahrgänge-5-6-Schuljahr_Ö.pdf");
    assertThat(name).doesNotContainPattern("[\\\\/:*?\"<>|\\s]");
  }

  @Test
  void dateinamen_kuerzenEinenLangenNamen() {
    assertThat(Dateinamen.bereinigt("Sehr-Langer-Doppelname-Von-Und-Zu, Anna-Maria Theresia"))
        .isEqualTo("Sehr-Langer-Doppelname-Von-Und-Zu-Anna-M");
  }

  private static String text(byte[] pdf) throws IOException {
    PdfReader leser = new PdfReader(pdf);
    PdfTextExtractor extraktor = new PdfTextExtractor(leser);
    List<String> seiten = new ArrayList<>();
    for (int seite = 1; seite <= leser.getNumberOfPages(); seite++) {
      seiten.add(extraktor.getTextFromPage(seite));
    }
    return String.join("\n", seiten);
  }

  private static Map<String, byte[]> entpacke(byte[] zip) throws IOException {
    Map<String, byte[]> eintraege = new LinkedHashMap<>();
    try (ZipInputStream eingang = new ZipInputStream(new ByteArrayInputStream(zip))) {
      for (ZipEntry eintrag = eingang.getNextEntry(); eintrag != null;
          eintrag = eingang.getNextEntry()) {
        eintraege.put(eintrag.getName(), eingang.readAllBytes());
      }
    }
    return eintraege;
  }
}
