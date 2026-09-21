package de.openclassware.elternsprechtag.sprechtag.application.port.in;

import de.openclassware.elternsprechtag.sprechtag.domain.ErinnerungsVorlauf;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Was der Organizer an einem Sprechtag eingibt — die gemeinsame Form von {@link Anlegen} und
 * {@link Bearbeiten}.
 *
 * <p>Als einziger Typ dieser Schicht <b>veränderlich</b>, mit Gettern, Settern und
 * No-Args-Konstruktor: Der Vaadin-{@code Binder} schreibt Feld für Feld hinein und liest Feld für
 * Feld heraus. Ein Record ginge dafür nicht.
 *
 * <p>Die Werte sind hier noch roh — {@link String}, {@link LocalTime}, {@link UUID}. Ob sie eine
 * gültige Zeitstruktur ergeben, entscheidet die Domäne, nicht dieses Formular; deshalb darf hier
 * auch alles {@code null} sein, was der Benutzer noch nicht ausgefüllt hat.
 */
public class SprechtagFormular {

  private String titel;
  private String ort;
  private String beschreibung;
  private String schulkontakt;
  private LocalDate datum;
  private LocalTime beginn;
  private LocalTime ende;
  private Integer slotInMinuten = 15;
  private String accessToken = UUID.randomUUID().toString();
  private Set<UUID> klasseIds = new LinkedHashSet<>();
  private ErinnerungsVorlauf erinnerungsVorlauf = ErinnerungsVorlauf.KEINE;

  /**
   * Ob Datum, Zeitfenster, Slot-Dauer und Klassen schon festliegen. Kein Eingabewert, sondern die
   * Antwort des Aggregats auf die Frage, die die Oberfläche stellen muss: Darf sie diese Felder
   * überhaupt noch anbieten?
   *
   * <p>Ohne sie bliebe nur die Weigerung <em>nach</em> dem Absenden — der Organizer verlöre dabei
   * auch seine erlaubten Textänderungen, weil die Transaktion als Ganzes zurückrollt.
   * `ABDECKUNG.md` Z. 87 verlangt „gesperrt", nicht „abgewiesen".
   */
  private boolean zeitstrukturEingefroren;

  public String getTitel() {
    return titel;
  }

  public void setTitel(String titel) {
    this.titel = titel;
  }

  public String getOrt() {
    return ort;
  }

  public void setOrt(String ort) {
    this.ort = ort;
  }

  public String getBeschreibung() {
    return beschreibung;
  }

  public void setBeschreibung(String beschreibung) {
    this.beschreibung = beschreibung;
  }

  public String getSchulkontakt() {
    return schulkontakt;
  }

  public void setSchulkontakt(String schulkontakt) {
    this.schulkontakt = schulkontakt;
  }

  public LocalDate getDatum() {
    return datum;
  }

  public void setDatum(LocalDate datum) {
    this.datum = datum;
  }

  public LocalTime getBeginn() {
    return beginn;
  }

  public void setBeginn(LocalTime beginn) {
    this.beginn = beginn;
  }

  public LocalTime getEnde() {
    return ende;
  }

  public void setEnde(LocalTime ende) {
    this.ende = ende;
  }

  public Integer getSlotInMinuten() {
    return slotInMinuten;
  }

  public void setSlotInMinuten(Integer slotInMinuten) {
    this.slotInMinuten = slotInMinuten;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public Set<UUID> getKlasseIds() {
    return klasseIds;
  }

  public void setKlasseIds(Set<UUID> klasseIds) {
    this.klasseIds = klasseIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(klasseIds);
  }

  public ErinnerungsVorlauf getErinnerungsVorlauf() {
    return erinnerungsVorlauf;
  }

  public void setErinnerungsVorlauf(ErinnerungsVorlauf erinnerungsVorlauf) {
    this.erinnerungsVorlauf =
        erinnerungsVorlauf == null ? ErinnerungsVorlauf.KEINE : erinnerungsVorlauf;
  }

  public boolean isZeitstrukturEingefroren() {
    return zeitstrukturEingefroren;
  }

  public void setZeitstrukturEingefroren(boolean zeitstrukturEingefroren) {
    this.zeitstrukturEingefroren = zeitstrukturEingefroren;
  }
}
