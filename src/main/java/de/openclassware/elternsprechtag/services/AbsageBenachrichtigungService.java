package de.openclassware.elternsprechtag.services;

import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ermittelt bei Absage eines Sprechtags die zu benachrichtigenden Eltern, formuliert die Absage-Mail
 * und übergibt jede Adresse genau einmal als fertige {@link Nachricht} an den
 * {@link BenachrichtigungSender}-Port. Die Kernmethode ist synchron und ohne echtes SMTP
 * verifizierbar; ausgelöst wird sie nach Commit vom {@link AbsageBenachrichtigungListener}.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AbsageBenachrichtigungService {

  private static final Locale LOCALE = Locale.GERMANY;

  private final SprechtagRepository sprechtagRepository;
  private final BuchungRepository buchungRepository;
  private final BenachrichtigungSender sender;
  private final I18NProvider i18n;
  private final ElternsprechtagProperties properties;

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
   * protokolliert und mit den übrigen Empfängern fortgefahren. Betreff und Text stammen aus
   * {@code vaadin-i18n/translations.properties} (direkt über den {@link I18NProvider}, weil der
   * Versand ohne Vaadin-{@code UI}-Kontext läuft), das Datum aus {@link Formats}. Übergeben wird
   * ausschließlich der {@link Nachricht}-Record — Entities verlassen die Service-Schicht nicht.
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
    if (adressen.isEmpty()) {
      // Ohne Empfänger auch keine Formulierung — hält die Zusage „ohne Buchung passiert nichts".
      return;
    }

    String datum = Formats.dateLong(sprechtag.getStartDate());
    String betreff = i18n.getTranslation("absage.mail.subject", LOCALE, sprechtag.getTitel(), datum);
    String text =
        i18n.getTranslation(
            "absage.mail.body", LOCALE, sprechtag.getTitel(), datum, properties.getSchoolname());

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
