package de.openclassware.elternsprechtag.sprechtag.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Ein Sprechtag — Aggregat-Wurzel über seinen Lebenszyklus und seine Zeitstruktur.
 *
 * <p>Die Termine gehören <em>nicht</em> dazu, obwohl sie hier entstehen: Ein Lock über alle rund 600
 * Termine eines Sprechtags würde jede buchende Familie serialisieren, deshalb ist {@link Termin}
 * eine eigene Wurzel (ADR 0003). Der Sprechtag rechnet nur aus, welche Slots es gibt; herstellen tut
 * sie der Materialisierungs-Use-Case.
 *
 * <p>Die tragende Invariante ist das <b>Einfrieren der Zeitstruktur</b>: Datum, Zeitfenster,
 * Slot-Dauer und Klassenliste sind ab {@link SprechtagStatus#VEROEFFENTLICHT} unveränderlich, weil
 * genau aus ihnen die Termine berechnet wurden. Titel, Ort, Hinweistext, Schulkontakt und
 * Zugangs-Token bleiben änderbar — sie beschreiben den Sprechtag, sie bestimmen ihn nicht.
 *
 * <p>Die Klassen stehen als {@link KlasseId} darin; die Klasse selbst gehört der Schulorganisation.
 */
public final class Sprechtag extends AggregateRoot {

  private final SprechtagId id;

  /**
   * Der Stand des optimistischen Sperrens, wie er geladen wurde. {@code 0} heißt „noch nie
   * gespeichert". Das Aggregat zählt nicht selbst hoch — das tut der Persistenz-Adapter (ADR 0004).
   */
  private final long version;

  private String titel;
  private String ort;
  private String beschreibung;
  private Schulkontakt schulkontakt;
  private AccessToken accessToken;

  private LocalDate datum;
  private Zeitfenster zeitfenster;
  private Slotdauer slotdauer;
  private final List<KlasseId> klassen;

  private SprechtagStatus status;
  private ErinnerungsVorlauf erinnerungsVorlauf;

  private Sprechtag(
      SprechtagId id,
      long version,
      String titel,
      String ort,
      String beschreibung,
      Schulkontakt schulkontakt,
      AccessToken accessToken,
      LocalDate datum,
      Zeitfenster zeitfenster,
      Slotdauer slotdauer,
      List<KlasseId> klassen,
      SprechtagStatus status,
      ErinnerungsVorlauf erinnerungsVorlauf) {
    this.id = Objects.requireNonNull(id, "id");
    this.version = version;
    this.titel = pflichtTitel(titel);
    this.ort = ort;
    this.beschreibung = beschreibung;
    this.schulkontakt = Objects.requireNonNull(schulkontakt, "schulkontakt");
    this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
    this.datum = Objects.requireNonNull(datum, "datum");
    this.zeitfenster = Objects.requireNonNull(zeitfenster, "zeitfenster");
    this.slotdauer = Objects.requireNonNull(slotdauer, "slotdauer");
    this.klassen = new ArrayList<>(Objects.requireNonNull(klassen, "klassen"));
    this.status = Objects.requireNonNull(status, "status");
    this.erinnerungsVorlauf = Objects.requireNonNull(erinnerungsVorlauf, "erinnerungsVorlauf");
  }

  /** Ein frischer Entwurf. Nur so entsteht ein Sprechtag — jeder beginnt als Entwurf. */
  public static Sprechtag entwirf(
      String titel,
      String ort,
      String beschreibung,
      Schulkontakt schulkontakt,
      AccessToken accessToken,
      LocalDate datum,
      Zeitfenster zeitfenster,
      Slotdauer slotdauer,
      List<KlasseId> klassen,
      ErinnerungsVorlauf erinnerungsVorlauf) {
    return new Sprechtag(
        SprechtagId.neu(),
        0L,
        titel,
        ort,
        beschreibung,
        schulkontakt,
        accessToken,
        datum,
        zeitfenster,
        slotdauer,
        klassen,
        SprechtagStatus.ENTWURF,
        erinnerungsVorlauf);
  }

  /**
   * Setzt einen gespeicherten Sprechtag unverändert wieder zusammen — für den Persistenz-Adapter.
   * Prüft keine Übergänge und meldet kein Ereignis.
   */
  public static Sprechtag rekonstruiere(
      SprechtagId id,
      long version,
      String titel,
      String ort,
      String beschreibung,
      Schulkontakt schulkontakt,
      AccessToken accessToken,
      LocalDate datum,
      Zeitfenster zeitfenster,
      Slotdauer slotdauer,
      List<KlasseId> klassen,
      SprechtagStatus status,
      ErinnerungsVorlauf erinnerungsVorlauf) {
    return new Sprechtag(
        id,
        version,
        titel,
        ort,
        beschreibung,
        schulkontakt,
        accessToken,
        datum,
        zeitfenster,
        slotdauer,
        klassen,
        status,
        erinnerungsVorlauf);
  }

  /**
   * Ändert, was den Sprechtag beschreibt. Bleibt nach dem Veröffentlichen erlaubt (`ABDECKUNG.md`
   * Z. 90) — ein korrigierter Raum oder ein ergänzter Hinweis erreicht die Eltern über denselben
   * Link.
   *
   * <p>Dass das {@code accessToken} hier mitgeht, ist <em>keine</em> Entscheidung dieser Scheibe,
   * sondern der übernommene Stand: Die Oberfläche kann den Link neu würfeln. `ABDECKUNG.md` Z. 101
   * stuft das als <b>bewusst nein</b> ein und verlangt, die Funktion zu entfernen (#117) — dann
   * verschwindet der Parameter hier mit.
   *
   * @throws StatusuebergangException an einem abgesagten oder abgeschlossenen Sprechtag
   */
  public void beschreibeNeu(
      String titel,
      String ort,
      String beschreibung,
      Schulkontakt schulkontakt,
      AccessToken accessToken) {
    verlangeOffen("bearbeitet");
    this.titel = pflichtTitel(titel);
    this.ort = ort;
    this.beschreibung = beschreibung;
    this.schulkontakt = Objects.requireNonNull(schulkontakt, "schulkontakt");
    this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
  }

  /**
   * Ändert den Erinnerungsvorlauf. Nicht Teil der Zeitstruktur — er verschiebt keinen Termin und
   * macht keine Buchung ungültig — und bleibt deshalb auch nach dem Veröffentlichen änderbar
   * (ADR 0006). Eine Änderung wirkt erst ab dem nächsten Lauf des Erinnerungs-Schedulers (#107).
   *
   * @throws StatusuebergangException an einem abgesagten oder abgeschlossenen Sprechtag
   */
  public void aendereErinnerungsVorlauf(ErinnerungsVorlauf erinnerungsVorlauf) {
    verlangeOffen("in seinem Erinnerungsvorlauf geändert");
    this.erinnerungsVorlauf = Objects.requireNonNull(erinnerungsVorlauf, "erinnerungsVorlauf");
  }

  /**
   * Legt die Zeitstruktur fest: Datum, Zeitfenster, Slot-Dauer und teilnehmende Klassen.
   *
   * <p>Nur im Entwurf. Ein <em>unveränderter</em> Aufruf ist überall zulässig — die Oberfläche
   * schickt beim Speichern immer das ganze Formular, und das Einfrieren richtet sich gegen
   * Änderungen, nicht gegen das Wiedervorlegen desselben Standes.
   *
   * @throws ZeitstrukturEingefrorenException wenn sich nach dem Veröffentlichen etwas daran ändern
   *     soll
   */
  public void legeZeitstrukturFest(
      LocalDate datum, Zeitfenster zeitfenster, Slotdauer slotdauer, List<KlasseId> klassen) {
    Objects.requireNonNull(datum, "datum");
    Objects.requireNonNull(zeitfenster, "zeitfenster");
    Objects.requireNonNull(slotdauer, "slotdauer");
    Objects.requireNonNull(klassen, "klassen");
    if (zeitstrukturGleich(datum, zeitfenster, slotdauer, klassen)) {
      return;
    }
    verlangeOffen("in seiner Zeitstruktur geändert");
    if (status != SprechtagStatus.ENTWURF) {
      throw new ZeitstrukturEingefrorenException(
          "Datum, Zeitfenster, Slot-Dauer und Klassen liegen ab dem Veröffentlichen fest — die "
              + "Termine sind daraus entstanden. Wer sie ändern muss, sagt ab und legt neu an.");
    }
    this.datum = datum;
    this.zeitfenster = zeitfenster;
    this.slotdauer = slotdauer;
    this.klassen.clear();
    this.klassen.addAll(klassen);
  }

  private boolean zeitstrukturGleich(
      LocalDate datum, Zeitfenster zeitfenster, Slotdauer slotdauer, List<KlasseId> klassen) {
    // Die Klassenliste ist fachlich eine Menge; ihre Reihenfolge steht nirgends und ist deshalb
    // auch keine Änderung.
    return this.datum.equals(datum)
        && this.zeitfenster.equals(zeitfenster)
        && this.slotdauer.equals(slotdauer)
        && new LinkedHashSet<>(this.klassen).equals(new LinkedHashSet<>(klassen));
  }

  /** Gibt den Sprechtag für die Eltern frei und meldet {@link SprechtagVeroeffentlicht}. */
  public void veroeffentliche() {
    wechsleNach(SprechtagStatus.VEROEFFENTLICHT);
    melde(new SprechtagVeroeffentlicht(id));
  }

  /** Sagt den Sprechtag ab und meldet {@link SprechtagAbgesagt}; daran hängt der Elternversand. */
  public void sageAb() {
    wechsleNach(SprechtagStatus.ABGESAGT);
    melde(new SprechtagAbgesagt(id));
  }

  /**
   * Schließt den Sprechtag ab, sobald seine Endzeit verstrichen ist — der Tagesjob (Issue #124).
   * Das ist der einzige Weg nach {@link SprechtagStatus#ABGESCHLOSSEN}: Der Abschluss ist eine
   * Tatsache, kein Handgriff — und braucht deshalb auch keinen Rückweg (#166).
   *
   * @param jetzt der Zeitpunkt, gegen den die Endzeit geprüft wird
   * @return ob der Sprechtag dabei abgeschlossen wurde
   */
  public boolean schliesseAbWennVorbei(LocalDateTime jetzt) {
    if (status != SprechtagStatus.VEROEFFENTLICHT || !endzeit().isBefore(jetzt)) {
      return false;
    }
    wechsleNach(SprechtagStatus.ABGESCHLOSSEN);
    return true;
  }

  /**
   * Nimmt die Veröffentlichung zurück — für den zu früh veröffentlichten Sprechtag (`ABDECKUNG.md`
   * Z. 91). Die materialisierten Termine verlieren damit ihre Grundlage; sie zu entfernen ist Sache
   * des Use Case.
   *
   * <p>Ob gebucht wurde, weiß der Sprechtag nicht — das steht in den Termin-Aggregaten. Die Antwort
   * kommt deshalb als Parameter herein; über die Folge entscheidet das Aggregat.
   *
   * @param wurdeGebucht ob an diesem Sprechtag jemals gebucht wurde — auch eine inzwischen
   *     stornierte Buchung zählt: Benachrichtigt wurde trotzdem
   * @throws SprechtagHatBuchungenException wenn gebucht wurde; dann führt nur die Absage weiter
   */
  public void nimmVeroeffentlichungZurueck(boolean wurdeGebucht) {
    if (wurdeGebucht) {
      throw new SprechtagHatBuchungenException(
          "An diesem Sprechtag wurde bereits gebucht. Er lässt sich nicht mehr zum Entwurf "
              + "zurücknehmen — wer ihn nicht halten kann, sagt ihn ab; dann erfahren die Eltern "
              + "davon.");
    }
    wechsleNach(SprechtagStatus.ENTWURF);
  }

  /**
   * Die Zeiträume der Gespräche, wie sie beim Veröffentlichen materialisiert werden — je Slot einer,
   * chronologisch. Ein Rest-Slot, der nicht mehr voll ins Zeitfenster passt, entfällt
   * (`ABDECKUNG.md` Z. 96).
   *
   * <p>Die Rechnung steht hier und nicht im Use Case: Sie ist die eigentliche Bedeutung von Datum,
   * Zeitfenster und Slot-Dauer — und der Grund, warum diese drei danach festliegen.
   */
  public List<Zeitraum> slots() {
    List<Zeitraum> slots = new ArrayList<>();
    int dauer = slotdauer.minuten();
    for (LocalTime beginn = zeitfenster.beginn();
        !beginn.plusMinutes(dauer).isAfter(zeitfenster.ende());
        beginn = beginn.plusMinutes(dauer)) {
      slots.add(
          new Zeitraum(
              LocalDateTime.of(datum, beginn), LocalDateTime.of(datum, beginn.plusMinutes(dauer))));
    }
    return slots;
  }

  /**
   * Eine Kopie als frischer Entwurf — gleiche Zeitstruktur und Beschreibung, eigenes Zugangs-Token.
   * Das Token muss neu sein: Zwei Sprechtage am selben Link wären für die Eltern einer.
   */
  public Sprechtag dupliziere(AccessToken neuesToken) {
    return entwirf(
        titel,
        ort,
        beschreibung,
        schulkontakt,
        neuesToken,
        datum,
        zeitfenster,
        slotdauer,
        List.copyOf(klassen),
        erinnerungsVorlauf);
  }

  private void wechsleNach(SprechtagStatus ziel) {
    if (!status.erlaubteUebergaenge().contains(ziel)) {
      throw new StatusuebergangException("Ungültiger Statusübergang: " + status + " -> " + ziel);
    }
    status = ziel;
  }

  /** Ein Endzustand ist endgültig: Von hier aus wird weder geändert noch weitergeschaltet. */
  private void verlangeOffen(String was) {
    if (status.istEndzustand()) {
      throw new StatusuebergangException(
          "Ein Sprechtag im Status " + status + " wird nicht mehr " + was + ".");
    }
  }

  private static String pflichtTitel(String titel) {
    Objects.requireNonNull(titel, "titel");
    if (titel.isBlank()) {
      throw new IllegalArgumentException("Ein Sprechtag ohne Titel ist in keiner Liste zu finden");
    }
    return titel;
  }

  public SprechtagId id() {
    return id;
  }

  public long version() {
    return version;
  }

  public String titel() {
    return titel;
  }

  /** Darf {@code null} sein. */
  public String ort() {
    return ort;
  }

  /** Darf {@code null} sein. */
  public String beschreibung() {
    return beschreibung;
  }

  public Schulkontakt schulkontakt() {
    return schulkontakt;
  }

  public AccessToken accessToken() {
    return accessToken;
  }

  public LocalDate datum() {
    return datum;
  }

  public Zeitfenster zeitfenster() {
    return zeitfenster;
  }

  /**
   * Der Zeitpunkt, an dem der Sprechtag vorbei ist: sein Datum zur Endzeit des Zeitfensters. Ab
   * hier schließt ihn der Tagesjob ab (#124), ab hier läuft auch die Aufbewahrungsfrist (#126).
   */
  public LocalDateTime endzeit() {
    return datum.atTime(zeitfenster.ende());
  }

  public Slotdauer slotdauer() {
    return slotdauer;
  }

  public List<KlasseId> klassen() {
    return List.copyOf(klassen);
  }

  public SprechtagStatus status() {
    return status;
  }

  public ErinnerungsVorlauf erinnerungsVorlauf() {
    return erinnerungsVorlauf;
  }
}
