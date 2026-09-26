package de.openclassware.elternsprechtag.sprechtag.adapter.in.web;

import de.openclassware.elternsprechtag.sprechtag.application.port.in.Absagen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Duplizieren;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Sprechtagsuebersicht.SprechtagZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.Veroeffentlichen;
import de.openclassware.elternsprechtag.sprechtag.application.port.in.ZurueckAufEntwurf;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagStatus;
import de.openclassware.elternsprechtag.sprechtag.adapter.in.web.SprechtagMeldungen.Meldung;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class ManageSprechtagPresenter {

  private final Sprechtagsuebersicht uebersicht;
  private final Veroeffentlichen veroeffentlichen;
  private final Absagen absagen;
  private final ZurueckAufEntwurf zurueckAufEntwurf;
  private final Duplizieren duplizieren;

  List<SprechtagZeile> findAllSprechtage() {
    return uebersicht.alle();
  }

  /**
   * Filtert die Sprechtage nach Status und Titel-Suchbegriff. {@code status == null} bedeutet „alle";
   * die Query wird normalisiert (getrimmt, kleingeschrieben) und als Teilstring im Titel gesucht.
   */
  List<SprechtagZeile> filter(List<SprechtagZeile> alle, SprechtagStatus status, String query) {
    String needle = query == null ? "" : query.trim().toLowerCase(Locale.GERMANY);
    return alle.stream()
        .filter(sprechtag -> status == null || sprechtag.status() == status)
        .filter(sprechtag -> sprechtag.titel().toLowerCase(Locale.GERMANY).contains(needle))
        .toList();
  }

  /**
   * Schaltet den Sprechtag auf den gewählten Zielstatus — jeder Weg hat seinen eigenen Use Case, und
   * jeder prüft am Aggregat, ob er gangbar ist. Dass die Oberfläche einen Eintrag anbietet, heißt
   * nichts: Das Menü entsteht aus einem Read-Modell und darf veraltet sein.
   *
   * @return leer, wenn es nichts zu sagen gibt — sonst die Begründung der Weigerung oder die
   *     Hinweise zum Veröffentlichen: ohne einen einzigen Termin, Anmeldeschluss schon vorbei
   */
  List<Meldung> wechsleStatus(UUID id, SprechtagStatus ziel) {
    try {
      return switch (ziel) {
        case VEROEFFENTLICHT ->
            SprechtagMeldungen.hinweiseZu(veroeffentlichen.veroeffentliche(id));
        case ABGESAGT -> {
          absagen.sageAb(id);
          yield List.of();
        }
        case ENTWURF -> {
          zurueckAufEntwurf.nimmZurueck(id);
          yield List.of();
        }
        // Kein Handgriff: Abgeschlossen wird allein durch den Tagesjob (#166).
        case ABGESCHLOSSEN -> List.of();
      };
    } catch (RuntimeException fehler) {
      return List.of(SprechtagMeldungen.zu(fehler));
    }
  }

  /**
   * Anzahl der von einer Absage betroffenen Eltern (aktive Buchungen, je E-Mail-Adresse einmal) —
   * für den Bestätigungsdialog vor dem Absagen.
   */
  long zaehleBetroffeneEltern(UUID id) {
    return absagen.zaehleBetroffeneEltern(id);
  }

  UUID duplicate(UUID id) {
    return duplizieren.dupliziere(id);
  }
}
