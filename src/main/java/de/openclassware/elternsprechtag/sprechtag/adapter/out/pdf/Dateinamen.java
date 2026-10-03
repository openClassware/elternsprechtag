package de.openclassware.elternsprechtag.sprechtag.adapter.out.pdf;

import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Blatt;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck.Kopf;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Die Namen der Druckdateien: vorn der Stand des Exports, dann der Sprechtag, dann die Lehrkraft —
 * {@code 2026-10-02_1437_Herbstsprechtag_KRA.pdf} im ZIP
 * {@code 2026-10-02_1437_Herbstsprechtag_Tagesplaene.zip}. Der Stand vorn sortiert mehrere Exporte
 * nach Version und verhindert, dass ein neuer Download einen alten überschreibt; den Sprechtag
 * erkennt man am Titel. Die Uhrzeit steht ohne Doppelpunkt, den Windows im Namen nicht nimmt.
 *
 * <p>Die Lehrkraft steht als Kürzel im Namen — der Bezeichner, den eine Schule kennt. Fehlt es
 * (eine Lehrkraft, die nur noch aus einer Buchung bekannt ist), steht ersatzweise ihr Name da.
 *
 * <p>Titel und Name werden auf je {@value #HOECHSTLAENGE} Zeichen gekürzt. Das ist keine
 * Kosmetik: Der Windows-Explorer entpackt in einen Ordner mit dem ZIP-Namen, der Titel steht im
 * Pfad also zweimal — ungekürzt (bis zu 255 Zeichen) risse das die 260 Zeichen von MAX_PATH, und
 * das Entpacken bräche ab oder ließe Dateien stillschweigend aus. Der volle Text steht ohnehin im
 * Kopf des Blatts.
 *
 * <p>Umlaute bleiben, das ZIP schreibt UTF-8. Ersetzt wird, was ein Dateisystem nicht nimmt, dazu
 * Leerraum und Komma — aus „Müller, Anna" wird {@code Müller-Anna}.
 */
final class Dateinamen {

  static final int HOECHSTLAENGE = 40;

  /** Kein Anzeigeformat, sondern ein Namensbestandteil — deshalb nicht in {@code Formats}. */
  private static final DateTimeFormatter STAND = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");
  private static final Pattern UNZULAESSIG = Pattern.compile("[\\\\/:*?\"<>|,\\s\\p{Cntrl}]+");
  private static final Pattern MEHRFACH = Pattern.compile("-{2,}");

  private Dateinamen() {}

  static String plan(Kopf kopf, Blatt blatt, LocalDateTime stand) {
    String lehrkraft = bereinigt(blatt.kuerzel());
    if (lehrkraft.isEmpty()) {
      lehrkraft = bereinigt(blatt.anzeigeName());
    }
    return verbinde(stand.format(STAND), bereinigt(kopf.titel()), lehrkraft) + ".pdf";
  }

  static String plaene(Kopf kopf, String wort, LocalDateTime stand) {
    return verbinde(stand.format(STAND), bereinigt(kopf.titel()), bereinigt(wort)) + ".zip";
  }

  /**
   * Hängt {@code -2}, {@code -3} … an, wenn der Name im ZIP schon vergeben ist — zwei Lehrkräfte mit
   * gleichem Kürzel ließen den ZIP-Bau sonst mit einer {@code ZipException} abbrechen.
   */
  static String eindeutig(String name, Set<String> vergeben) {
    String kandidat = name;
    int punkt = name.lastIndexOf('.');
    for (int nummer = 2; !vergeben.add(kandidat); nummer++) {
      kandidat = name.substring(0, punkt) + "-" + nummer + name.substring(punkt);
    }
    return kandidat;
  }

  /** Ersetzt Unzulässiges durch {@code -} und kürzt auf die Höchstlänge — in Zeichen, nicht Bytes. */
  static String bereinigt(String text) {
    if (text == null) {
      return "";
    }
    String ersetzt =
        MEHRFACH.matcher(UNZULAESSIG.matcher(text.strip()).replaceAll("-")).replaceAll("-");
    if (ersetzt.codePointCount(0, ersetzt.length()) > HOECHSTLAENGE) {
      ersetzt = ersetzt.substring(0, ersetzt.offsetByCodePoints(0, HOECHSTLAENGE));
    }
    // Windows verträgt keinen Punkt am Namensende; Bindestriche an den Rändern sind nur Rauschen.
    return ersetzt.replaceAll("^[-.]+|[-.]+$", "");
  }

  private static String verbinde(String... teile) {
    List<String> vorhanden = new ArrayList<>();
    for (String teil : teile) {
      if (!teil.isEmpty()) {
        vorhanden.add(teil);
      }
    }
    return String.join("_", vorhanden);
  }
}
