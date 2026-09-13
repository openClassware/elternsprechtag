package de.openclassware.elternsprechtag.sprechtag.domain;

/**
 * Wer gebucht hat: Elternteil, Kind und die Kontakt-E-Mail. Die Buchung hält diese Angaben an sich
 * selbst — es gibt bewusst keine Eltern-Entity, und ein Import darf sie nie überschreiben (ADR 0003).
 *
 * <p>Die E-Mail ist Pflicht: An ihr hängen die Absage-Benachrichtigung (ADR 0001) und die
 * Buchungsbestätigung (ADR 0002). Geprüft wird nur „nicht leer" — eine Syntaxprüfung gehört an die
 * Oberfläche, nicht an das Aggregat.
 */
public record Familie(String elternName, String schuelerName, String email) {

  public Familie {
    elternName = pflicht(elternName, "elternName");
    schuelerName = pflicht(schuelerName, "schuelerName");
    email = pflicht(email, "email");
  }

  private static String pflicht(String wert, String feld) {
    if (wert == null || wert.isBlank()) {
      throw new IllegalArgumentException("Pflichtangabe fehlt: " + feld);
    }
    return wert.trim();
  }
}
