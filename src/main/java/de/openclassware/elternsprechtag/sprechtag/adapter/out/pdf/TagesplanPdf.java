package de.openclassware.elternsprechtag.sprechtag.adapter.out.pdf;

import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Blatt;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Kopf;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Zeile;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;
import org.openpdf.text.pdf.PdfWriter;

/**
 * Setzt das Blatt einer Lehrkraft: A4 quer, Kopf mit Lehrkraft, Sprechtag, Datum, Ort und Stand,
 * darunter alle Slots chronologisch und rechts eine leere Spalte für Handschrift.
 *
 * <p>Belegte Slots tragen die Uhrzeit fett, freie tragen den Vermerk „frei" und sonst leere Zellen
 * — Platz für eine Familie, die am Tag selbst dazukommt.
 *
 * <p>„Seite x von y" entsteht in einem zweiten Durchgang: Die Gesamtzahl kennt erst, wer fertig
 * gesetzt hat. Die Fußzeile nennt dazu Lehrkraft und Stand, damit ein loses zweites Blatt
 * zuzuordnen bleibt.
 *
 * <p>Eine Instanz setzt alle Blätter eines ZIPs — sie hält nur Schriften und Übersetzungen.
 */
final class TagesplanPdf {

  private static final Locale LOCALE = Locale.GERMANY;
  private static final float RAND = 36f;
  /** Rund 1,2 cm — Platz für eine handschriftliche Zeile, auch wo die Notiz leer ist. */
  private static final float MINDESTZEILENHOEHE = 34f;
  /** Zeit, Schüler, Klasse, Fach, Eltern, Notiz, Handschrift — die Handschrift bekommt ein Drittel. */
  private static final float[] SPALTEN = {7f, 14f, 7f, 9f, 14f, 17f, 32f};

  private static final Color GRAU = new Color(0x6b, 0x6b, 0x6b);
  private static final Color LINIE = new Color(0xb0, 0xb0, 0xb0);
  private static final Color KOPFGRUND = new Color(0xee, 0xee, 0xee);

  private final I18NProvider i18n;
  private final Font text;
  private final Font fett;
  private final Font klein;
  private final Font titel;
  private final Font vermerk;

  TagesplanPdf(I18NProvider i18n) {
    this.i18n = i18n;
    BaseFont regular = schrift("liberation/LiberationSans-Regular.ttf");
    BaseFont bold = schrift("liberation/LiberationSans-Bold.ttf");
    this.text = new Font(regular, 10f);
    this.fett = new Font(bold, 10f);
    this.klein = new Font(regular, 8f, Font.NORMAL, GRAU);
    this.titel = new Font(bold, 16f);
    this.vermerk = new Font(regular, 9f, Font.ITALIC, GRAU);
  }

  byte[] setze(Kopf kopf, Blatt blatt, LocalDateTime stand) {
    String standText = Formats.dateTimeShort(stand);
    return nummeriere(rohfassung(kopf, blatt, standText), blatt, standText);
  }

  private byte[] rohfassung(Kopf kopf, Blatt blatt, String stand) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    // Unten mehr Rand: Dort setzt der zweite Durchgang die Fußzeile hinein.
    Document dokument = new Document(PageSize.A4.rotate(), RAND, RAND, RAND, RAND + 14f);
    PdfWriter.getInstance(dokument, bytes);
    dokument.addTitle(blatt.anzeigeName() + " — " + kopf.titel());
    dokument.open();
    dokument.add(kopfzeilen(kopf, blatt, stand));
    dokument.add(tabelle(blatt));
    dokument.close();
    return bytes.toByteArray();
  }

  private Paragraph kopfzeilen(Kopf kopf, Blatt blatt, String stand) {
    Paragraph kopfzeilen = new Paragraph();
    Phrase name = new Phrase(blatt.anzeigeName(), titel);
    if (blatt.kuerzel() != null && !blatt.kuerzel().isBlank()) {
      name.add(new Chunk("  " + blatt.kuerzel(), text));
    }
    kopfzeilen.add(name);
    kopfzeilen.add(Chunk.NEWLINE);

    List<String> sprechtag = new ArrayList<>();
    sprechtag.add(kopf.titel());
    sprechtag.add(Formats.dateLong(kopf.datum()));
    if (kopf.ort() != null && !kopf.ort().isBlank()) {
      sprechtag.add(uebersetze("druck.kopf.ort", kopf.ort()));
    }
    kopfzeilen.add(new Phrase(String.join(" · ", sprechtag), fett));
    kopfzeilen.add(Chunk.NEWLINE);
    kopfzeilen.add(new Phrase(uebersetze("druck.kopf.stand", stand), text));
    if (kopf.anonymisiertAm() != null) {
      kopfzeilen.add(Chunk.NEWLINE);
      kopfzeilen.add(
          new Phrase(
              uebersetze("druck.kopf.anonymisiert", Formats.dateLong(kopf.anonymisiertAm())),
              vermerk));
    }
    kopfzeilen.setSpacingAfter(12f);
    return kopfzeilen;
  }

  private PdfPTable tabelle(Blatt blatt) {
    PdfPTable tabelle = new PdfPTable(SPALTEN);
    tabelle.setWidthPercentage(100f);
    // Die Kopfzeile wiederholt sich auf jeder Folgeseite.
    tabelle.setHeaderRows(1);
    for (String schluessel :
        List.of(
            "auswertung.table.zeit",
            "auswertung.table.schueler",
            "auswertung.table.klasse",
            "auswertung.table.fach",
            "auswertung.table.eltern",
            "auswertung.table.notiz",
            "druck.table.handschrift")) {
      PdfPCell zelle = zelle(new Phrase(uebersetze(schluessel), fett));
      zelle.setBackgroundColor(KOPFGRUND);
      tabelle.addCell(zelle);
    }
    if (blatt.zeilen().isEmpty()) {
      PdfPCell leer = zelle(new Phrase(uebersetze("druck.leer"), vermerk));
      leer.setColspan(SPALTEN.length);
      leer.setMinimumHeight(MINDESTZEILENHOEHE);
      tabelle.addCell(leer);
      return tabelle;
    }
    for (Zeile zeile : blatt.zeilen()) {
      zeile(tabelle, zeile);
    }
    return tabelle;
  }

  private void zeile(PdfPTable tabelle, Zeile zeile) {
    if (zeile.frei()) {
      // Ein freier Slot ist Platz für den Nachtrag am Tag selbst: Uhrzeit und Vermerk, die übrigen
      // Zellen bleiben leer — das Raster führt die Hand.
      tabelle.addCell(inhaltszelle(new Phrase(Formats.time(zeile.startzeit()), text)));
      tabelle.addCell(inhaltszelle(new Phrase(uebersetze("druck.zeile.frei"), vermerk)));
      for (int spalte = 2; spalte < SPALTEN.length; spalte++) {
        tabelle.addCell(inhaltszelle(new Phrase("")));
      }
      return;
    }
    tabelle.addCell(inhaltszelle(new Phrase(Formats.time(zeile.startzeit()), fett)));
    tabelle.addCell(inhaltszelle(new Phrase(leerWennNull(zeile.schuelerName()), text)));
    tabelle.addCell(inhaltszelle(new Phrase(leerWennNull(zeile.klasse()), text)));
    tabelle.addCell(inhaltszelle(new Phrase(leerWennNull(zeile.fach()), text)));
    tabelle.addCell(inhaltszelle(new Phrase(leerWennNull(zeile.elternName()), text)));
    // Sind die Angaben entfernt, steht statt der (ohnehin leeren) Notiz der Vermerk — wie am
    // Bildschirm.
    Phrase notiz =
        zeile.entferntAm() != null
            ? new Phrase(
                uebersetze("auswertung.zeile.entfernt", Formats.dateLong(zeile.entferntAm())),
                vermerk)
            : new Phrase(leerWennNull(zeile.notiz()), text);
    tabelle.addCell(inhaltszelle(notiz));
    tabelle.addCell(inhaltszelle(new Phrase("")));
  }

  private PdfPCell inhaltszelle(Phrase inhalt) {
    PdfPCell zelle = zelle(inhalt);
    zelle.setMinimumHeight(MINDESTZEILENHOEHE);
    return zelle;
  }

  private static PdfPCell zelle(Phrase inhalt) {
    PdfPCell zelle = new PdfPCell(inhalt);
    zelle.setPadding(4f);
    zelle.setPaddingBottom(6f);
    zelle.setUseAscender(true);
    zelle.setBorderColor(LINIE);
    zelle.setBorderWidth(0.5f);
    zelle.setVerticalAlignment(Element.ALIGN_TOP);
    return zelle;
  }

  /** Zweiter Durchgang: Fußzeile mit Lehrkraft, Stand und „Seite x von y" auf jede Seite. */
  private byte[] nummeriere(byte[] roh, Blatt blatt, String stand) {
    try {
      PdfReader leser = new PdfReader(roh);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      PdfStamper stempel = new PdfStamper(leser, bytes);
      int seiten = leser.getNumberOfPages();
      for (int seite = 1; seite <= seiten; seite++) {
        // Mit Drehung: Das Blatt liegt quer, die ungedrehte Breite wäre die des Hochformats.
        Rectangle format = leser.getPageSizeWithRotation(seite);
        PdfContentByte ueber = stempel.getOverContent(seite);
        ColumnText.showTextAligned(
            ueber,
            Element.ALIGN_LEFT,
            new Phrase(uebersetze("druck.fuss.links", blatt.anzeigeName(), stand), klein),
            RAND,
            RAND - 4f,
            0f);
        ColumnText.showTextAligned(
            ueber,
            Element.ALIGN_RIGHT,
            new Phrase(uebersetze("druck.fuss.seite", seite, seiten), klein),
            format.getWidth() - RAND,
            RAND - 4f,
            0f);
      }
      stempel.close();
      leser.close();
      return bytes.toByteArray();
    } catch (IOException fehler) {
      throw new UncheckedIOException(fehler);
    }
  }

  private String uebersetze(String schluessel, Object... parameter) {
    return i18n.getTranslation(schluessel, LOCALE, parameter);
  }

  private static String leerWennNull(String wert) {
    return wert == null ? "" : wert;
  }

  /**
   * Liberation Sans, eingebettet als Identity-H: Die Standard-Helvetica kann nur WinAnsi, und Namen
   * wie „Yılmaz" oder „Łukasz" kämen verstümmelt aufs Blatt.
   */
  private static BaseFont schrift(String pfad) {
    try {
      return BaseFont.createFont(pfad, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    } catch (IOException fehler) {
      throw new UncheckedIOException(fehler);
    } catch (DocumentException fehler) {
      throw new IllegalStateException("Schrift nicht ladbar: " + pfad, fehler);
    }
  }
}
