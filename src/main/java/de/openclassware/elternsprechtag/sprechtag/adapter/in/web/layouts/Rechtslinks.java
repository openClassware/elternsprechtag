package de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts;

import java.util.ArrayList;
import java.util.List;

/**
 * Welche Links der Footer jeder Web-View zeigt (Issue #230): Impressum und Datenschutzerklärung,
 * jeweils nur, wenn ihre Adresse konfiguriert ist. Vaadin-frei, damit die Entscheidung ohne
 * Spring und ohne Oberfläche testbar bleibt.
 */
final class Rechtslinks {

  static final String IMPRESSUM = "footer.impressum";
  static final String DATENSCHUTZERKLAERUNG = "footer.datenschutzerklaerung";

  /** Ein Footer-Link: Übersetzungsschlüssel des Linktexts und Zieladresse. */
  record Rechtslink(String textKey, String url) {}

  private Rechtslinks() {}

  /** Die Links in fester Reihenfolge; fehlt eine Adresse oder ist sie leer, entfällt ihr Link. */
  static List<Rechtslink> aus(String impressumUrl, String datenschutzerklaerungUrl) {
    List<Rechtslink> links = new ArrayList<>(2);
    hinzu(links, IMPRESSUM, impressumUrl);
    hinzu(links, DATENSCHUTZERKLAERUNG, datenschutzerklaerungUrl);
    return List.copyOf(links);
  }

  private static void hinzu(List<Rechtslink> links, String textKey, String url) {
    if (url != null && !url.isBlank()) {
      links.add(new Rechtslink(textKey, url.strip()));
    }
  }
}
