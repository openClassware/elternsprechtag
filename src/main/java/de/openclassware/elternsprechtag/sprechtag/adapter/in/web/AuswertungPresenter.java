package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.AngabenEntfernen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.BuchungsZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.LehrkraftPlan;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.NichtErreicht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Auswerten.SprechtagAuswertung;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Drucken;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Drucken.Datei;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.Ergebnis;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.EntfallenLassen.Angebot;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Stornieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen.SlotOption;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Umbuchen.UmbuchAnfrage;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.SprechtagMeldungen.Meldung;
import de.openclassware.elternsprechtag.sprechtag.domain.Mailart;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class AuswertungPresenter {

  private final Auswerten auswerten;
  private final Stornieren stornieren;
  private final Umbuchen umbuchen;
  private final EntfallenLassen entfallenLassen;
  private final AngabenEntfernen angabenEntfernen;
  private final Drucken drucken;

  Optional<SprechtagAuswertung> werteAus(UUID sprechtagId) {
    return auswerten.werteAus(sprechtagId);
  }

  /**
   * Ob die Ansicht je Zeile eine Storno-Aktion anbietet. Nur an einem veröffentlichten Sprechtag:
   * Danach ist „wieder frei" eine Lüge, weil niemand mehr bucht, und ein Löschverlangen nach dem
   * Sprechtag ist Anonymisierung, nicht Storno.
   *
   * <p>Die Entscheidung liegt hier und nicht im View — der rendert nur, was er bekommt. Verbindlich
   * ist sie ohnehin erst im Use Case: Die Route ist per URL für jeden Status erreichbar.
   */
  boolean darfStornieren(SprechtagAuswertung auswertung) {
    return auswertung.status() == SprechtagStatus.VEROEFFENTLICHT;
  }

  /**
   * Ob die Ansicht den Einstieg ins Nachtragen anbietet. Dieselbe Bedingung wie beim Storno: Nur an
   * einem veröffentlichten Sprechtag gibt es Termine, die eine Familie belegen könnte.
   *
   * <p>Verbindlich ist auch das erst im {@code Nachtragen}-Use-Case — die Route ist per URL für
   * jeden Status erreichbar.
   */
  boolean darfNachtragen(SprechtagAuswertung auswertung) {
    return auswertung.status() == SprechtagStatus.VEROEFFENTLICHT;
  }

  /**
   * Ob die Ansicht die Sammelaktion „Lehrkraft fällt aus" anbietet. Dieselbe Bedingung wie beim
   * Storno und aus demselben Grund: Verbindlich ist sie ohnehin erst im
   * {@code EntfallenLassen}-Use-Case — die Route ist per URL für jeden Status erreichbar.
   */
  boolean darfAusfallErfassen(SprechtagAuswertung auswertung) {
    return auswertung.status() == SprechtagStatus.VEROEFFENTLICHT;
  }

  /**
   * Ob die Ansicht den Hinweis auf die abgelaufene Aufbewahrungsfrist zeigt, und mit welchem Datum
   * (Issue #127). Ohne ihn hielte der Organizer die Pseudonyme und leeren Notizen für einen Fehler.
   *
   * <p>Unabhängig vom Status: Auch die Buchungen eines abgesagten Sprechtags werden anonymisiert.
   * Solange die Frist läuft — oder ein abgebrochener Lauf den Sprechtag noch nicht vermerkt hat —
   * bleibt das Optional leer.
   */
  Optional<LocalDate> anonymisierungsHinweis(SprechtagAuswertung auswertung) {
    return Optional.ofNullable(auswertung.anonymisiertAm());
  }

  /**
   * Die Arbeitsliste „Nicht erreicht" (Issue #110). Leer heißt: kein Block — auch dann nicht, wenn
   * der Sprechtag schon anonymisiert ist; anonymisierte Buchungen führt die Liste ohnehin nicht.
   */
  List<NichtErreicht> nichtErreicht(SprechtagAuswertung auswertung) {
    return auswertung.nichtErreicht();
  }

  /** Der i18n-Schlüssel, unter dem die Liste eine Nachrichtenart nennt. */
  String nachrichtSchluessel(Mailart art) {
    return switch (art) {
      case BESTAETIGUNG -> "auswertung.nicht-erreicht.art.bestaetigung";
      case ERINNERUNG -> "auswertung.nicht-erreicht.art.erinnerung";
      case ABSAGE -> "auswertung.nicht-erreicht.art.absage";
      case AUSFALL -> "auswertung.nicht-erreicht.art.ausfall";
    };
  }

  /**
   * Ob die Tabelle eine Aktionsspalte trägt: überall dort, wo es an irgendeiner Zeile etwas zu tun
   * geben kann — Storno und Umbuchen am veröffentlichten Sprechtag, „Angaben entfernen" auch danach.
   * Nach dem Anonymisierungs-Lauf gibt es nichts mehr zu entfernen, die Spalte fällt weg.
   */
  boolean hatAktionsspalte(SprechtagAuswertung auswertung) {
    return auswertung.status() != SprechtagStatus.ENTWURF && auswertung.anonymisiertAm() == null;
  }

  /**
   * Ob diese Zeile „Angaben entfernen" anbietet (Issue #129): solange ihre Angaben noch dastehen,
   * und an einem veröffentlichten Sprechtag nur für eine stornierte Buchung — eine geltende Zusage
   * läuft dort über das Storno mit entfernten Angaben. Verbindlich prüft der Termin.
   */
  boolean darfAngabenEntfernen(SprechtagAuswertung auswertung, BuchungsZeile zeile) {
    return hatAktionsspalte(auswertung)
        && zeile.anonymisiertAm() == null
        && (zeile.storniert() || auswertung.status() != SprechtagStatus.VEROEFFENTLICHT);
  }

  /** Ob diese Zeile Storno und Umbuchen anbietet — nur eine geltende Zusage, nur veröffentlicht. */
  boolean darfStornieren(SprechtagAuswertung auswertung, BuchungsZeile zeile) {
    return darfStornieren(auswertung) && !zeile.storniert();
  }

  /**
   * Der Vermerk „Angaben entfernt am …" an der Zeile. Nach dem Anonymisierungs-Lauf erklärt der
   * Hinweis über der Tabelle alles, der Vermerk an jeder Zeile wäre nur Wiederholung.
   */
  Optional<LocalDate> entferntVermerk(SprechtagAuswertung auswertung, BuchungsZeile zeile) {
    if (auswertung.anonymisiertAm() != null) {
      return Optional.empty();
    }
    return Optional.ofNullable(zeile.anonymisiertAm());
  }

  /**
   * Das Etikett unter der Uhrzeit einer stornierten Zeile: „entfallen", wenn der Ausfall der
   * Lehrkraft sie zurückgenommen hat, sonst „storniert". Eine geltende Zusage trägt keins.
   */
  Optional<String> zustandsEtikett(BuchungsZeile zeile) {
    if (!zeile.storniert()) {
      return Optional.empty();
    }
    return Optional.of(
        zeile.entfallen() ? "auswertung.zeile.entfallen" : "auswertung.zeile.storniert");
  }

  /** Ob es den Schalter „Stornierte anzeigen" braucht — nur, wenn es Stornierte gibt. */
  boolean hatStornierte(SprechtagAuswertung auswertung) {
    return auswertung.plaene().stream().anyMatch(plan -> !plan.stornierte().isEmpty());
  }

  /**
   * Die Zeilen eines Plans, wie der View sie zeigt: die geltenden Zusagen, auf Wunsch mit den
   * stornierten dazwischen — chronologisch, eine stornierte vor einer späteren Buchung desselben
   * Slots.
   */
  List<BuchungsZeile> sichtbareZeilen(LehrkraftPlan plan, boolean stornierteAnzeigen) {
    if (!stornierteAnzeigen || plan.stornierte().isEmpty()) {
      return plan.zeilen();
    }
    List<BuchungsZeile> alle = new ArrayList<>(plan.stornierte());
    alle.addAll(plan.zeilen());
    // Stabil sortiert: Bei gleicher Startzeit bleibt die stornierte vor der geltenden stehen.
    alle.sort(Comparator.comparing(BuchungsZeile::startzeit));
    return alle;
  }

  /**
   * Reicht das Storno an den Use Case durch.
   *
   * @param angabenEntfernen ob die Angaben der Familie im selben Zug fallen — das Löschverlangen
   *     vor dem Sprechtag
   * @return leer, wenn es geklappt hat — sonst die Begründung der Weigerung. Eine verletzte
   *     Vorbedingung (fremder Tab, Sprechtag inzwischen abgeschlossen) soll der Organizer sehen und
   *     nicht als stille Wirkungslosigkeit erleben.
   */
  Optional<Meldung> storniere(UUID buchungId, boolean angabenEntfernen) {
    try {
      stornieren.storniere(buchungId, angabenEntfernen);
      return Optional.empty();
    } catch (RuntimeException fehler) {
      return Optional.of(SprechtagMeldungen.zu(fehler));
    }
  }

  /** Der i18n-Schlüssel der Erfolgsmeldung — mit entfernten Angaben sagt sie das dazu. */
  String stornoErfolg(boolean angabenEntfernen) {
    return angabenEntfernen ? "auswertung.storno.erfolg-entfernt" : "auswertung.storno.erfolg";
  }

  /**
   * Reicht „Angaben entfernen" an den Use Case durch — Spiegelbild zu
   * {@link #storniere(UUID, boolean)}.
   *
   * @return leer, wenn es geklappt hat — sonst die Begründung der Weigerung.
   */
  Optional<Meldung> entferneAngaben(UUID buchungId) {
    try {
      angabenEntfernen.entferne(buchungId);
      return Optional.empty();
    } catch (RuntimeException fehler) {
      return Optional.of(SprechtagMeldungen.zu(fehler));
    }
  }

  /**
   * Die freien Slots derselben Lehrkraft wie die übergebene Buchung — das Angebot des
   * Umbuchen-Dialogs. Reicht nur durch: Die Auswahl trifft {@code Umbuchen} selbst.
   */
  List<SlotOption> freieSlots(UUID buchungId) {
    return umbuchen.freieSlots(buchungId);
  }

  /**
   * Reicht das Umbuchen an den Use Case durch — Spiegelbild zu {@link #storniere(UUID)}.
   *
   * @return leer, wenn es geklappt hat — sonst die Begründung der Weigerung.
   */
  Optional<Meldung> umbuche(UUID buchungId, UUID neuerTerminId) {
    try {
      umbuchen.umbuche(new UmbuchAnfrage(buchungId, neuerTerminId));
      return Optional.empty();
    } catch (RuntimeException fehler) {
      return Optional.of(SprechtagMeldungen.zu(fehler));
    }
  }

  /**
   * Die Slots dieser Lehrkraft an diesem Sprechtag und ob die Familien danach selbst neu buchen
   * können — das Angebot des Ausfall-Dialogs. Reicht nur durch: Die Auswahl trifft der Dialog,
   * verbindlich entschieden wird in {@link #entfalleLassen(List)}.
   */
  Angebot ausfallAngebot(UUID sprechtagId, UUID lehrkraftId) {
    return entfallenLassen.angebot(sprechtagId, lehrkraftId);
  }

  /**
   * Reicht die Sammelaktion an den Use Case durch.
   *
   * @return das Ergebnis, wenn es geklappt hat — sonst die Begründung der Weigerung
   */
  AusfallAusgang entfalleLassen(List<UUID> terminIds) {
    try {
      return AusfallAusgang.erfolg(entfallenLassen.entfallenLassen(terminIds));
    } catch (RuntimeException fehler) {
      return AusfallAusgang.weigerung(SprechtagMeldungen.zu(fehler));
    }
  }

  /** Ergebnis der Sammelaktion — entweder das echte Ergebnis oder die Begründung der Weigerung. */
  record AusfallAusgang(Ergebnis ergebnis, Meldung weigerung) {
    static AusfallAusgang erfolg(Ergebnis ergebnis) {
      return new AusfallAusgang(ergebnis, null);
    }

    static AusfallAusgang weigerung(Meldung weigerung) {
      return new AusfallAusgang(null, weigerung);
    }

    boolean istWeigerung() {
      return weigerung != null;
    }
  }

  /**
   * Was die View unter der Filterzeile zeigt: Lehrkraft-Filter und Namenssuche gelten zugleich
   * (Issue #121). {@code lehrerId == null} bedeutet „alle Lehrkräfte", ein leerer Suchbegriff „keine
   * Suche". Reine Ableitung ohne Zustand — Auswahl und Begriff hält der View.
   */
  Planansicht ansicht(
      List<LehrkraftPlan> alle, UUID lehrerId, String suchbegriff, boolean stornierteAnzeigen) {
    List<LehrkraftPlan> plaene =
        lehrerId == null
            ? alle
            : alle.stream().filter(plan -> plan.lehrerId().equals(lehrerId)).toList();
    Namenssuche suche = Namenssuche.nach(suchbegriff);
    List<LehrkraftPlan> treffer = suche.filtere(plaene, stornierteAnzeigen);
    Optional<String> keinTreffer =
        suche.istAktiv() && treffer.isEmpty() ? Optional.of(suche.begriff()) : Optional.empty();
    // Ausgeblendete stornierte Treffer verschwiegen sonst eine gespeicherte Buchung der Familie —
    // beim Auskunftsverlangen genau die falsche Antwort.
    int verborgeneStornierte = stornierteAnzeigen ? 0 : suche.stornierteTreffer(plaene);
    return new Planansicht(treffer, keinTreffer, verborgeneStornierte);
  }

  /**
   * Die Pläne, die die View rendert; dazu der Suchbegriff für „Keine Buchung passt zu …", wenn eine
   * Suche nichts findet, und die Zahl passender stornierter Buchungen, die der Schalter gerade
   * ausblendet ({@code 0}: kein Hinweis).
   */
  record Planansicht(
      List<LehrkraftPlan> plaene, Optional<String> keinTrefferFuer, int verborgeneStornierte) {}

  /**
   * Ob die Ansicht den Download der Tagespläne anbietet (Issue #120): sobald der Sprechtag eine
   * beteiligte Lehrkraft hat, in jedem Status. Filter und Suche spielen keine Rolle — gedruckt wird
   * immer der vollständige Tag jeder Lehrkraft.
   */
  boolean darfDrucken(SprechtagAuswertung auswertung) {
    return !auswertung.plaene().isEmpty();
  }

  /** Reicht den Druck an den Use Case durch — er lädt selbst, frisch aus der Datenbank. */
  Optional<Datei> drucke(UUID sprechtagId) {
    return drucken.druckePlaene(sprechtagId);
  }
}
