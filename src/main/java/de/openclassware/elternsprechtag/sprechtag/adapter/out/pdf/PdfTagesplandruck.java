package de.openclassware.elternsprechtag.sprechtag.adapter.out.pdf;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Drucken.Datei;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Tagesplandruck;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

/**
 * Erfüllt {@link Tagesplandruck}: ein PDF je Blatt über {@link TagesplanPdf}, gebündelt als ZIP,
 * die Namen über {@link Dateinamen}.
 *
 * <p>Der {@link I18NProvider} entsteht hier selbst, statt injiziert zu werden — derselbe Weg wie im
 * Mailtext, und aus demselben Grund: Gedruckt wird ohne Vaadin-{@code UI}-Kontext, und der Adapter
 * soll auch in einem Slice-Test ohne Vaadin-Verdrahtung stehen.
 */
@Component
class PdfTagesplandruck implements Tagesplandruck {

  static final String ZIP = "application/zip";

  private final I18NProvider i18n = new DefaultI18NProvider(List.of(Locale.GERMANY));

  @Override
  public Datei gebuendelt(Kopf kopf, List<Blatt> blaetter, LocalDateTime stand) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    TagesplanPdf satz = new TagesplanPdf(i18n);
    // UTF-8 setzt das Sprach-Flag der Einträge — Umlaute in den Namen überstehen damit auch den
    // Windows-Explorer.
    try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
      Set<String> vergeben = new HashSet<>();
      for (Blatt blatt : blaetter) {
        String name = Dateinamen.eindeutig(Dateinamen.plan(kopf, blatt, stand), vergeben);
        zip.putNextEntry(new ZipEntry(name));
        zip.write(satz.setze(kopf, blatt, stand));
        zip.closeEntry();
      }
    } catch (IOException fehler) {
      throw new UncheckedIOException(fehler);
    }
    String wort = i18n.getTranslation("druck.datei.plaene", Locale.GERMANY);
    return new Datei(Dateinamen.plaene(kopf, wort, stand), ZIP, bytes.toByteArray());
  }
}
