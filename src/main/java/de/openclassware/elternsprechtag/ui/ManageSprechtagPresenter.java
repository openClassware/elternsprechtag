package de.openclassware.elternsprechtag.ui;

import de.openclassware.elternsprechtag.domain.SprechtagStatusEnum;
import de.openclassware.elternsprechtag.services.AbsageBenachrichtigungService;
import de.openclassware.elternsprechtag.services.SprechtagService;
import de.openclassware.elternsprechtag.services.SprechtagService.SchulkontaktFehltException;
import de.openclassware.elternsprechtag.services.SprechtagService.SprechtagRow;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
class ManageSprechtagPresenter {

  private final SprechtagService sprechtagService;
  private final AbsageBenachrichtigungService absageBenachrichtigungService;

  List<SprechtagRow> findAllSprechtage() {
    return sprechtagService.findAllRows();
  }

  /**
   * Filtert die Sprechtage nach Status und Titel-Suchbegriff. {@code status == null} bedeutet „alle";
   * die Query wird normalisiert (getrimmt, kleingeschrieben) und als Teilstring im Titel gesucht.
   */
  List<SprechtagRow> filter(List<SprechtagRow> alle, SprechtagStatusEnum status, String query) {
    String needle = query == null ? "" : query.trim().toLowerCase(Locale.GERMANY);
    return alle.stream()
        .filter(sprechtag -> status == null || sprechtag.status() == status)
        .filter(sprechtag -> sprechtag.titel().toLowerCase(Locale.GERMANY).contains(needle))
        .toList();
  }

  /**
   * Wechselt den Status und liefert im Fehlerfall den i18n-Schlüssel der Meldung, sonst
   * {@link Optional#empty()}. Die Ausnahme der Service-Schicht endet hier: Der View bekommt keine
   * Exception zu sehen, sondern nur noch einen Text-Schlüssel, den er anzeigt.
   */
  Optional<String> changeStatus(UUID id, SprechtagStatusEnum newStatus) {
    try {
      sprechtagService.changeStatus(id, newStatus);
      return Optional.empty();
    } catch (SchulkontaktFehltException fehlt) {
      return Optional.of("manage-sprechtag.publish.schulkontakt-required");
    }
  }

  /**
   * Anzahl der von einer Absage betroffenen Eltern (aktive Buchungen, je E-Mail-Adresse einmal) —
   * für den Bestätigungsdialog vor dem Absagen.
   */
  long zaehleBetroffeneEltern(UUID id) {
    return absageBenachrichtigungService.zaehleAktiveEmpfaenger(id);
  }

  UUID duplicate(UUID id) {
    return sprechtagService.duplicate(id);
  }
}
