package de.openclassware.elternsprechtag.services;

import de.openclassware.elternsprechtag.domain.BuchungStatusEnum;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.repositories.BuchungRepository;
import de.openclassware.elternsprechtag.repositories.SprechtagRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ermittelt bei Absage eines Sprechtags die zu benachrichtigenden Eltern und übergibt jede Adresse
 * genau einmal an den {@link BenachrichtigungSender}-Port. Die Kernmethode ist synchron und ohne
 * echtes SMTP verifizierbar; der eigentliche Auslöser (Domänen-Event, {@code @Async}) kommt in
 * einem späteren Ticket und ruft {@link #benachrichtige(UUID)} lediglich auf.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AbsageBenachrichtigungService {

  private final SprechtagRepository sprechtagRepository;
  private final BuchungRepository buchungRepository;
  private final BenachrichtigungSender sender;

  /**
   * Vertrag zwischen Kernlogik und {@link BenachrichtigungSender}: die Empfänger-Adresse plus die
   * für den Mailtext nötigen Sprechtag-Kopfdaten. {@code ort} ist optional ({@code null}, wenn der
   * Sprechtag keinen Ort trägt).
   */
  public record AbsageEmpfaenger(String email, String titel, LocalDate datum, String ort) {}

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
   * protokolliert und mit den übrigen Empfängern fortgefahren. Mappt vollständig auf Records —
   * Entities verlassen die Service-Schicht nicht.
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

    for (String adresse : adressen) {
      AbsageEmpfaenger empfaenger =
          new AbsageEmpfaenger(
              adresse, sprechtag.getTitel(), sprechtag.getStartDate(), sprechtag.getLocation());
      try {
        sender.sende(empfaenger);
      } catch (RuntimeException e) {
        // Best-effort: Einzelfehler (Bounce, voller Posteingang) stoppen den Versand nicht.
        log.warn("Absage-Benachrichtigung an {} fehlgeschlagen: {}", adresse, e.getMessage());
      }
    }
  }
}
