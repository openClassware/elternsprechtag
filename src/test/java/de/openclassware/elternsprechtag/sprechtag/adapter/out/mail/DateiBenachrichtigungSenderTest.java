package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;

import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Die Mail-Ablage ohne Spring: Was der Sender schreibt, wird aus dem Verzeichnis zurückgelesen. Der
 * Doku-Lauf (`site/screenshots`) liest dieselben Dateien — ihr Aufbau ist der Vertrag dorthin.
 */
class DateiBenachrichtigungSenderTest {

  @TempDir Path ablage;

  private static final Nachricht ABSAGE =
      new Nachricht(
          "mueller@example.org",
          "Sprechtag „Frühjahr“ am 12. März 2027 abgesagt",
          "Guten Tag,\n\nder Sprechtag muss leider abgesagt werden.\n\nMit freundlichen Grüßen");

  @Test
  void sende_schreibtEmpfaengerBetreffUndTextInEineDatei() throws IOException {
    new DateiBenachrichtigungSender(ablage).sende(ABSAGE);

    assertThat(dateien()).hasSize(1);
    assertThat(Files.readString(dateien().get(0), StandardCharsets.UTF_8))
        .isEqualTo(
            """
            An: mueller@example.org
            Betreff: Sprechtag „Frühjahr“ am 12. März 2027 abgesagt

            Guten Tag,

            der Sprechtag muss leider abgesagt werden.

            Mit freundlichen Grüßen""");
  }

  @Test
  void sende_legtJedeNachrichtAlsEigeneDateiAb_inDerReihenfolgeDesVersands() throws IOException {
    DateiBenachrichtigungSender sender = new DateiBenachrichtigungSender(ablage);
    for (int i = 1; i <= 12; i++) {
      sender.sende(new Nachricht("a@example.org", "Betreff " + i, "Text"));
    }

    // Der Doku-Lauf ordnet nach dem Dateinamen; der muss also auch bei gleicher Millisekunde und
    // über zweistellige Zähler hinweg die Versandreihenfolge treffen.
    List<String> betreffs = dateien().stream().map(DateiBenachrichtigungSenderTest::betreff).toList();
    assertThat(betreffs)
        .containsExactly(
            "Betreff 1", "Betreff 2", "Betreff 3", "Betreff 4", "Betreff 5", "Betreff 6",
            "Betreff 7", "Betreff 8", "Betreff 9", "Betreff 10", "Betreff 11", "Betreff 12");
  }

  @Test
  void sende_legtDasVerzeichnisAn_wennEsFehlt() throws IOException {
    Path tiefer = ablage.resolve("doku/mails");

    new DateiBenachrichtigungSender(tiefer).sende(ABSAGE);

    try (Stream<Path> inhalt = Files.list(tiefer)) {
      assertThat(inhalt).hasSize(1);
    }
  }

  @Test
  void sende_hinterlaesstNurFertigeTextdateien() throws IOException {
    new DateiBenachrichtigungSender(ablage).sende(ABSAGE);

    // Geschrieben wird erst unter anderem Namen und dann umbenannt: Wer das Verzeichnis liest,
    // sieht nie eine halb geschriebene `.txt`.
    try (Stream<Path> inhalt = Files.list(ablage)) {
      assertThat(inhalt).allMatch(datei -> datei.getFileName().toString().endsWith(".txt"));
    }
  }

  private List<Path> dateien() throws IOException {
    try (Stream<Path> inhalt = Files.list(ablage)) {
      return inhalt.filter(datei -> datei.toString().endsWith(".txt")).sorted().toList();
    }
  }

  private static String betreff(Path datei) {
    try {
      return Files.readAllLines(datei, StandardCharsets.UTF_8).get(1).substring("Betreff: ".length());
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
