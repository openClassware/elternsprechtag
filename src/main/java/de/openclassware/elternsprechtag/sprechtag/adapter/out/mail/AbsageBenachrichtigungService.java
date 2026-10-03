package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen.Versand;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.Empfaenger;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellergebnis;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Ermittelt bei Absage eines Sprechtags die zu benachrichtigenden Eltern, formuliert die Absage-Mail
 * und übergibt jede Adresse genau einmal als fertige {@link Nachricht} an den
 * {@link BenachrichtigungSender}-Port. Die Kernmethode ist synchron und ohne echtes SMTP
 * verifizierbar; ausgelöst wird sie über {@link MailBenachrichtigungen}.
 */
@RequiredArgsConstructor
@Service
@Slf4j
class AbsageBenachrichtigungService {

  private static final Locale LOCALE = Locale.GERMANY;

  private final SprechtagAnsichten sprechtagAnsichten;
  private final BuchungsAnsichten buchungsAnsichten;
  private final BenachrichtigungSender sender;
  private final I18NProvider i18n;
  private final ElternsprechtagProperties properties;

  /**
   * Benachrichtigt alle Eltern mit aktiver Buchung an diesem Sprechtag über die Absage — je
   * E-Mail-Adresse genau einmal. Existiert der Sprechtag nicht oder gibt es keine aktive Buchung,
   * passiert nichts (kein Sende-Aufruf, kein Fehler, leeres Ergebnis). Der Versand ist best-effort:
   * schlägt der Sender für eine Adresse fehl, wird der Fehler per {@code log.warn} protokolliert, als
   * {@link Zustellergebnis#FEHLGESCHLAGEN} gemeldet und mit den übrigen Empfängern fortgefahren.
   * Betreff und Text stammen aus {@code vaadin-i18n/translations.properties} (direkt über den
   * {@link I18NProvider}, weil der Versand ohne Vaadin-{@code UI}-Kontext läuft), das Datum aus
   * {@link Formats}.
   *
   * <p>Bewusst <b>ohne</b> eigenes {@code @Transactional}: Die beiden Abfragen sind je für sich
   * abgeschlossen, und der Versand soll keine Datenbankverbindung halten, während er auf den
   * Mailserver wartet.
   *
   * @return je Adresse eine Nachricht samt den Buchungen, die sie trägt — mehrere, wenn sich
   *     Geschwister oder mehrere Lehrkräfte eine Adresse teilen
   */
  public List<Versand> benachrichtige(SprechtagId sprechtagId) {
    Optional<SprechtagAnsichten.Kopf> gefunden = sprechtagAnsichten.kopf(sprechtagId);
    if (gefunden.isEmpty()) {
      return List.of();
    }
    SprechtagAnsichten.Kopf sprechtag = gefunden.get();

    // Die Query liefert je Buchung eine Zeile; gebündelt wird hier, je Adresse genau eine Nachricht.
    Map<String, List<BuchungId>> jeAdresse = new LinkedHashMap<>();
    for (Empfaenger empfaenger : buchungsAnsichten.aktiveEmpfaenger(sprechtagId)) {
      jeAdresse
          .computeIfAbsent(empfaenger.elternEmail(), k -> new ArrayList<>())
          .add(empfaenger.buchung());
    }
    // Ohne Empfänger auch keine Formulierung — hält die Zusage „ohne Buchung passiert nichts".
    List<Versand> versand = new ArrayList<>();
    for (Map.Entry<String, List<BuchungId>> adresse : jeAdresse.entrySet()) {
      versand.add(new Versand(adresse.getValue(), sende(sprechtag, adresse.getKey())));
    }
    return versand;
  }

  private Zustellergebnis sende(SprechtagAnsichten.Kopf sprechtag, String adresse) {
    try {
      String datum = Formats.dateLong(sprechtag.datum());
      String betreff = i18n.getTranslation("absage.mail.subject", LOCALE, sprechtag.titel(), datum);
      sender.sende(new Nachricht(adresse, betreff, baueText(sprechtag, datum)));
      return Zustellergebnis.ABGESCHICKT;
    } catch (RuntimeException e) {
      // Best-effort: Einzelfehler (abgelehnte Adresse, fehlender Textbaustein) stoppen den Versand
      // an die übrigen nicht.
      log.warn("Absage-Benachrichtigung an {} fehlgeschlagen: {}", adresse, e.getMessage());
      return Zustellergebnis.FEHLGESCHLAGEN;
    }
  }

  /**
   * Setzt den Fließtext aus den i18n-Bausteinen zusammen. Der Schulkontakt steht als eigener,
   * beschrifteter Absatz zwischen Hinweis und Grußformel, nicht in einem bestehenden Satz — er ist
   * mehrzeiliger Freitext. Er ist am Sprechtag ab dem Entwurf Pflicht und daher immer vorhanden.
   */
  private String baueText(SprechtagAnsichten.Kopf sprechtag, String datum) {
    List<String> absaetze = new ArrayList<>();
    absaetze.add(i18n.getTranslation("absage.mail.body", LOCALE, sprechtag.titel(), datum));
    absaetze.add(
        i18n.getTranslation(
            "absage.mail.schulkontakt", LOCALE, sprechtag.schulkontakt().trim()));
    absaetze.add(i18n.getTranslation("absage.mail.closing", LOCALE, properties.getSchoolname()));
    return String.join("\n\n", absaetze);
  }
}
