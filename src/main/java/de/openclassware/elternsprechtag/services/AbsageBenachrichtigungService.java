package de.openclassware.elternsprechtag.services;

import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.domain.BuchungStatusEnum;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.repositories.BuchungRepository;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;
import de.openclassware.elternsprechtag.services.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.ui.Formats;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ermittelt bei Absage eines Sprechtags die zu benachrichtigenden Eltern, formuliert die Absage-Mail
 * und übergibt jede Adresse genau einmal an den anlassneutralen {@link BenachrichtigungSender}-Port.
 * Alle Texte kommen aus {@code vaadin-i18n/translations.properties} über den {@link I18NProvider},
 * das Datum über {@link Formats}. Die Kernmethode ist synchron und ohne echtes SMTP verifizierbar;
 * der Auslöser ({@link AbsageBenachrichtigungListener}) ruft {@link #benachrichtige(UUID)} lediglich
 * auf.
 */
@Service
@Slf4j
public class AbsageBenachrichtigungService {

  private static final Locale LOCALE = Locale.GERMANY;

  private final SprechtagRepository sprechtagRepository;
  private final BuchungRepository buchungRepository;
  private final BenachrichtigungSender sender;
  private final I18NProvider i18n;
  private final String schulname;

  AbsageBenachrichtigungService(
      SprechtagRepository sprechtagRepository,
      BuchungRepository buchungRepository,
      BenachrichtigungSender sender,
      I18NProvider i18n,
      @Value("${elternsprechtag.schoolname}") String schulname) {
    this.sprechtagRepository = sprechtagRepository;
    this.buchungRepository = buchungRepository;
    this.sender = sender;
    this.i18n = i18n;
    this.schulname = schulname;
  }

  /**
   * Anzahl der Eltern mit aktiver ({@link BuchungStatusEnum#ZUGESAGT}) Buchung an diesem Sprechtag —
   * je E-Mail-Adresse genau einmal gezählt. Genau die Menge, die {@link #benachrichtige(UUID)}
   * benachrichtigen würde; dient dem Bestätigungsdialog vor der Absage.
   */
  @Transactional(readOnly = true)
  public long zaehleAktiveEmpfaenger(UUID sprechtagId) {
    return buchungRepository.countDistinctElternEmailByTermin_SprechtagIdAndStatus(
        sprechtagId, BuchungStatusEnum.ZUGESAGT);
  }

  /**
   * Benachrichtigt alle Eltern mit aktiver ({@link BuchungStatusEnum#ZUGESAGT}) Buchung an diesem
   * Sprechtag über die Absage — je E-Mail-Adresse genau einmal. Existiert der Sprechtag nicht oder
   * gibt es keine aktive Buchung, passiert nichts (kein Sende-Aufruf, kein Fehler). Der Versand ist
   * best-effort: schlägt der Sender für eine Adresse fehl, wird der Fehler per {@code log.warn}
   * protokolliert und mit den übrigen Empfängern fortgefahren. Über die Port-Grenze geht nur die
   * fertige {@link Nachricht} — Entities verlassen die Service-Schicht nicht.
   */
  @Transactional(readOnly = true)
  public void benachrichtige(UUID sprechtagId) {
    Optional<Sprechtag> gefunden = sprechtagRepository.findById(sprechtagId);
    if (gefunden.isEmpty()) {
      return;
    }
    Sprechtag sprechtag = gefunden.get();

    // Dedup pro E-Mail-Adresse erledigt die Query (distinct); je Adresse genau ein Empfänger.
    List<String> adressen =
        buchungRepository.findDistinctElternEmailByTermin_SprechtagAndStatus(
            sprechtag, BuchungStatusEnum.ZUGESAGT);

    String datum = Formats.dateLong(sprechtag.getStartDate());
    String betreff = i18n.getTranslation("absage.mail.subject", LOCALE, sprechtag.getTitel(), datum);
    String text =
        i18n.getTranslation("absage.mail.body", LOCALE, sprechtag.getTitel(), datum, schulname);

    for (String adresse : adressen) {
      try {
        sender.sende(new Nachricht(adresse, betreff, text));
      } catch (RuntimeException e) {
        // Best-effort: Einzelfehler (Bounce, voller Posteingang) stoppen den Versand nicht.
        log.warn("Absage-Benachrichtigung an {} fehlgeschlagen: {}", adresse, e.getMessage());
      }
    }
  }
}
