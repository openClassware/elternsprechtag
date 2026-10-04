package de.openclassware.elternsprechtag.sprechtag.adapter;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Die eine Stelle, die den Elternlink eines Sprechtags baut (Issue #109) — aus der konfigurierten
 * öffentlichen Basis-URL und dem Zugangs-Token. Teilen-Link der Oberfläche und Link in der
 * Ausfall-Mail sind damit derselbe; der Organizer sieht vorab, was die Familien bekommen.
 *
 * <p>Bewusst nicht aus {@code window.location.origin}: Der Organizer sitzt womöglich im internen Netz
 * unter einer anderen Adresse als die Eltern, und eine Mail hat gar keinen Browser. Die Basis-URL ist
 * deshalb Pflicht — ohne sie oder mit einer unbrauchbaren startet die Anwendung nicht, statt still
 * falsche Links zu verschicken.
 */
@Component
public class Elternlink {

  /** Der Pfad der Elternansicht — zugleich ihre Route, damit Link und Route nicht auseinanderlaufen. */
  public static final String PFAD = "elternsprechtag";

  private static final String PROPERTY = "elternsprechtag.oeffentliche-url";

  private final String basis;

  Elternlink(@Value("${" + PROPERTY + ":}") String oeffentlicheUrl) {
    this.basis = pruefe(oeffentlicheUrl);
  }

  /** Der absolute Elternlink zu diesem Zugangs-Token. */
  public String zu(String accessToken) {
    return basis + "/" + PFAD + "/" + accessToken;
  }

  /**
   * Gesetzt, absolut, {@code http} oder {@code https}, mit Host und ohne Query oder Fragment. Ein
   * Pfad ist erlaubt (Betrieb unter einem Unterpfad), abschließende {@code /} fallen weg.
   */
  private static String pruefe(String oeffentlicheUrl) {
    String wert = oeffentlicheUrl == null ? "" : oeffentlicheUrl.trim();
    if (wert.isEmpty()) {
      throw new IllegalStateException(
          PROPERTY
              + " ist nicht gesetzt (Umgebungsvariable ELTERNSPRECHTAG_OEFFENTLICHE_URL) — die"
              + " öffentliche Adresse, unter der die Eltern die Anwendung erreichen, z. B."
              + " https://elternsprechtag.schule.de");
    }
    URI uri;
    try {
      uri = new URI(wert);
    } catch (URISyntaxException e) {
      throw unbrauchbar(wert);
    }
    String schema = uri.getScheme();
    boolean webAdresse = "http".equalsIgnoreCase(schema) || "https".equalsIgnoreCase(schema);
    if (!webAdresse || uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null) {
      throw unbrauchbar(wert);
    }
    while (wert.endsWith("/")) {
      wert = wert.substring(0, wert.length() - 1);
    }
    return wert;
  }

  private static IllegalStateException unbrauchbar(String wert) {
    return new IllegalStateException(
        PROPERTY
            + " ist keine absolute http(s)-Adresse ohne Query und Fragment: "
            + wert);
  }
}
