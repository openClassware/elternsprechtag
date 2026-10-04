package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen.Versand;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.BelegZeile;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellergebnis;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Ermittelt bei Absage eines Sprechtags die zu benachrichtigenden Eltern, formuliert die Absage-Mail
 * und übergibt je {@link Empfaenger} — ein Kind an einer Adresse (ADR 0007) — eine fertige
 * {@link Nachricht} an den {@link BenachrichtigungSender}-Port. Die Kernmethode ist synchron und
 * ohne echtes SMTP verifizierbar; ausgelöst wird sie über {@link MailBenachrichtigungen}.
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
   * Benachrichtigt alle Eltern mit aktiver Buchung an diesem Sprechtag über die Absage — je Kind an
   * einer Adresse genau einmal. So bekommt auch die Stellvertreteradresse der Schule je Kind eine
   * eigene Mail und kann die Familien einzeln anrufen (Issue #148). Existiert der Sprechtag nicht
   * oder gibt es keine aktive Buchung, passiert nichts (kein Sende-Aufruf, kein Fehler, leeres
   * Ergebnis). Der Versand ist best-effort: schlägt der Sender für einen Empfänger fehl, wird der
   * Fehler per {@code log.warn} protokolliert, als {@link Zustellergebnis#FEHLGESCHLAGEN} gemeldet
   * und mit den übrigen fortgefahren. Betreff und Text stammen aus {@code
   * vaadin-i18n/translations.properties} (direkt über den {@link I18NProvider}, weil der Versand
   * ohne Vaadin-{@code UI}-Kontext läuft), Datum und Uhrzeit aus {@link Formats}.
   *
   * <p>Bewusst <b>ohne</b> eigenes {@code @Transactional}: Die beiden Abfragen sind je für sich
   * abgeschlossen, und der Versand soll keine Datenbankverbindung halten, während er auf den
   * Mailserver wartet.
   *
   * @return je Empfänger eine Nachricht samt den Buchungen, die sie trägt — mehrere, wenn das Kind
   *     bei mehreren Lehrkräften gebucht hat
   */
  public List<Versand> benachrichtige(SprechtagId sprechtagId) {
    Optional<SprechtagAnsichten.Kopf> gefunden = sprechtagAnsichten.kopf(sprechtagId);
    if (gefunden.isEmpty()) {
      return List.of();
    }
    SprechtagAnsichten.Kopf sprechtag = gefunden.get();

    // Ohne Empfänger auch keine Formulierung — hält die Zusage „ohne Buchung passiert nichts".
    List<Versand> versand = new ArrayList<>();
    for (Map.Entry<Empfaenger, List<BelegZeile>> gruppe :
        Empfaenger.gruppiere(buchungsAnsichten.aktiveBelege(sprechtagId)).entrySet()) {
      List<BuchungId> buchungen = gruppe.getValue().stream().map(BelegZeile::buchung).toList();
      versand.add(new Versand(buchungen, sende(sprechtag, gruppe.getKey(), gruppe.getValue())));
    }
    return versand;
  }

  private Zustellergebnis sende(
      SprechtagAnsichten.Kopf sprechtag, Empfaenger empfaenger, List<BelegZeile> zeilen) {
    try {
      String datum = Formats.dateLong(sprechtag.datum());
      String betreff = i18n.getTranslation("absage.mail.subject", LOCALE, sprechtag.titel(), datum);
      sender.sende(
          new Nachricht(
              empfaenger.elternEmail(), betreff, baueText(sprechtag, datum, empfaenger, zeilen)));
      return Zustellergebnis.ABGESCHICKT;
    } catch (RuntimeException e) {
      // Best-effort: Einzelfehler (abgelehnte Adresse, fehlender Textbaustein) stoppen den Versand
      // an die übrigen nicht.
      log.warn(
          "Absage-Benachrichtigung an {} fehlgeschlagen: {}", empfaenger.elternEmail(), e.getMessage());
      return Zustellergebnis.FEHLGESCHLAGEN;
    }
  }

  /**
   * Setzt den Fließtext aus den i18n-Bausteinen zusammen — Spiegelbild von
   * {@code AusfallBenachrichtigungService.baueText}, ohne Nachbuchen-Link (es gibt nichts mehr zu
   * buchen) und ohne Notiz (das Gespräch findet nicht statt). Der Schulkontakt ist am Sprechtag ab
   * dem Entwurf Pflicht und daher immer vorhanden.
   */
  private String baueText(
      SprechtagAnsichten.Kopf sprechtag,
      String datum,
      Empfaenger empfaenger,
      List<BelegZeile> zeilen) {
    List<String> absaetze = new ArrayList<>();
    absaetze.add(i18n.getTranslation("absage.mail.greeting", LOCALE));
    absaetze.add(i18n.getTranslation("absage.mail.intro", LOCALE, sprechtag.titel(), datum));
    absaetze.add(
        i18n.getTranslation(
            "absage.mail.kind", LOCALE, empfaenger.schuelerName(), empfaenger.klasse()));

    List<String> liste = new ArrayList<>();
    liste.add(i18n.getTranslation("absage.mail.termine", LOCALE));
    for (BelegZeile zeile : zeilen) {
      liste.add(
          i18n.getTranslation(
              "absage.mail.termin",
              LOCALE,
              Formats.time(zeile.zeit()),
              zeile.lehrkraftName(),
              zeile.fach()));
    }
    absaetze.add(String.join("\n", liste));

    absaetze.add(
        i18n.getTranslation(
            "absage.mail.schulkontakt", LOCALE, sprechtag.schulkontakt().trim()));
    absaetze.add(i18n.getTranslation("absage.mail.closing", LOCALE, properties.getSchoolname()));
    return String.join("\n\n", absaetze);
  }
}
