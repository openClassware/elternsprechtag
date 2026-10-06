package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link BenachrichtigungSender}, der jede Nachricht als Textdatei in ein Verzeichnis legt, statt
 * sie zu versenden. Greift nur ohne SMTP und nur, wenn {@code elternsprechtag.mail.ablage} gesetzt
 * ist — siehe {@link BenachrichtigungConfig}. Gebraucht wird er vom Doku-Lauf: Die Anwender-Doku
 * zeigt die Mails an Eltern im Wortlaut, und der Lauf liest sie hier ab, statt sie abzuschreiben.
 *
 * <p>Je Nachricht eine Datei {@code <Millisekunden>-<Zähler>.txt}: die Zeilen {@code An:} und
 * {@code Betreff:}, eine Leerzeile, dann der Text. Der Name ordnet nach Versandreihenfolge. Die
 * Datei entsteht unter anderem Namen und wird erst fertig umbenannt, damit niemand eine halbe liest.
 */
class DateiBenachrichtigungSender implements BenachrichtigungSender {

  private final Path ablage;
  private final AtomicLong zaehler = new AtomicLong();

  DateiBenachrichtigungSender(Path ablage) {
    this.ablage = ablage;
  }

  @Override
  public void sende(Nachricht nachricht) {
    String name = "%013d-%06d".formatted(System.currentTimeMillis(), zaehler.incrementAndGet());
    String inhalt =
        "An: " + nachricht.empfaenger() + "\nBetreff: " + nachricht.betreff() + "\n\n" + nachricht.text();
    try {
      Files.createDirectories(ablage);
      Path entwurf = ablage.resolve(name + ".schreibt");
      Files.writeString(entwurf, inhalt, StandardCharsets.UTF_8);
      Files.move(entwurf, ablage.resolve(name + ".txt"), StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new UncheckedIOException("Nachricht an " + nachricht.empfaenger() + " nicht abgelegt", e);
    }
  }
}
