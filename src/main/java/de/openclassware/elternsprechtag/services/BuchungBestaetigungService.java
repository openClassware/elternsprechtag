package de.openclassware.elternsprechtag.services;

import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.domain.Buchung;
import de.openclassware.elternsprechtag.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.domain.Lehrer;
import de.openclassware.elternsprechtag.domain.Sprechtag;
import de.openclassware.elternsprechtag.repositories.BuchungRepository;
import de.openclassware.elternsprechtag.services.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.ui.Formats;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Formuliert nach einem Eltern-Submit die Bestätigungsmail und übergibt sie als fertige
 * {@link Nachricht} an den {@link BenachrichtigungSender}-Port — Spiegelbild zu
 * {@link AbsageBenachrichtigungService}. Die Mail ist ein reiner Beleg: Sie enthält keinerlei
 * Aktion, insbesondere keinen Storno-Link. Die Kernmethode ist synchron und ohne echtes SMTP
 * verifizierbar; ausgelöst wird sie nach Commit vom {@link BuchungBestaetigungListener}.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class BuchungBestaetigungService {

  private static final Locale LOCALE = Locale.GERMANY;

  private final BuchungRepository buchungRepository;
  private final BenachrichtigungSender sender;
  private final I18NProvider i18n;
  private final ElternsprechtagProperties properties;

  /**
   * Ein bestätigter Termin des Vorgangs: wann, bei wem, in welchem Fach — und die Notiz, die genau
   * dieser Lehrkraft gilt. {@code notiz} darf {@code null} sein, dann entfällt die Notiz-Zeile.
   */
  record TerminZeile(LocalTime zeit, String lehrkraft, String fach, String notiz) {}

  /**
   * Der komplette Beleg eines Absendevorgangs. Empfänger und Kopfdaten sind aus den geladenen
   * Buchungen abgeleitet, nicht aus der Anfrage übernommen. {@code ort} darf {@code null} sein —
   * dann entfällt der Ort-Abschnitt.
   */
  record Bestaetigung(
      String empfaenger,
      String sprechtagTitel,
      LocalDate datum,
      String ort,
      String schuelerName,
      String klasse,
      List<TerminZeile> termine) {}

  /**
   * Bestätigt genau die Buchungen dieses Absendevorgangs mit <b>einer</b> Nachricht an die dort
   * hinterlegte Adresse. Eine leere Id-Liste oder durchweg unbekannte Ids führen zu keinem
   * Sende-Aufruf und zu keinem Fehler. Der Versand ist best-effort: scheitert der Sender, wird der
   * Fehler per {@code log.warn} protokolliert — die Buchung bleibt gültig, es gibt kein Retry.
   * Betreff und Text stammen aus {@code vaadin-i18n/translations.properties} (direkt über den
   * {@link I18NProvider}, weil der Versand ohne Vaadin-{@code UI}-Kontext läuft), Datum und Uhrzeit
   * aus {@link Formats}. Übergeben wird ausschließlich der {@link Nachricht}-Record — Entities
   * verlassen die Service-Schicht nicht.
   *
   * <p>Bewusst <b>ohne</b> eigenes {@code @Transactional}: Die eine Repository-Query bringt per
   * {@code @EntityGraph} alles mit, was das Mapping braucht, und ist damit für sich abgeschlossen.
   * So hält der Versand keine DB-Verbindung — ein hängender SMTP-Server (JavaMail wartet
   * voreingestellt unbegrenzt) könnte sonst je Buchung eine Verbindung des Pools blockieren und den
   * Pool erschöpfen, während der Versand hier bei <em>jeder</em> Buchung läuft.
   */
  public void bestaetige(List<UUID> buchungIds) {
    if (buchungIds == null || buchungIds.isEmpty()) {
      return;
    }
    // Die Query sortiert bereits nach Startzeit; die Mail listet damit chronologisch.
    List<Buchung> buchungen = buchungRepository.findByIdInOrderByTermin_StartzeitAsc(buchungIds);
    if (buchungen.isEmpty()) {
      // Unbekannte Ids (etwa nach zwischenzeitlicher Löschung): nichts zu bestätigen, kein Fehler.
      return;
    }

    // Empfänger vorab, damit ein Fehler beim Formulieren die Adresse trotzdem protokollieren kann.
    String empfaenger = buchungen.get(0).getElternEmail();
    try {
      Bestaetigung bestaetigung = zuBestaetigung(buchungen);
      String betreff =
          i18n.getTranslation(
              "buchung.mail.subject",
              LOCALE,
              bestaetigung.sprechtagTitel(),
              Formats.dateLong(bestaetigung.datum()));
      sender.sende(new Nachricht(bestaetigung.empfaenger(), betreff, baueText(bestaetigung)));
    } catch (RuntimeException e) {
      // Best-effort: Weder ein Zustellproblem noch ein Fehler beim Formulieren (fehlender
      // Textbaustein, unvollständiger Lehrauftrag) darf die festgeschriebene Buchung entwerten oder
      // als unbehandelte Ausnahme aus dem @Async-Thread entkommen.
      log.warn("Buchungsbestätigung an {} fehlgeschlagen: {}", empfaenger, e.getMessage());
    }
  }

  /**
   * Mappt die Buchungen des Vorgangs vollständig auf den Record. Alle Buchungen eines Submits teilen
   * Adresse, Kind und Sprechtag; abgeleitet werden sie aus der ersten geladenen Buchung.
   */
  private Bestaetigung zuBestaetigung(List<Buchung> buchungen) {
    Buchung erste = buchungen.get(0);
    Sprechtag sprechtag = erste.getTermin().getSprechtag();

    List<TerminZeile> termine = new ArrayList<>();
    for (Buchung buchung : buchungen) {
      Lehrauftrag lehrauftrag = buchung.getLehrauftrag();
      Lehrer lehrer = lehrauftrag.getLehrer();
      termine.add(
          new TerminZeile(
              buchung.getTermin().getStartzeit().toLocalTime(),
              lehrer.getVorname() + " " + lehrer.getNachname(),
              lehrauftrag.getFach().getName(),
              buchung.getNotiz()));
    }

    return new Bestaetigung(
        erste.getElternEmail(),
        sprechtag.getTitel(),
        sprechtag.getStartDate(),
        sprechtag.getLocation(),
        erste.getSchuelerName(),
        erste.getLehrauftrag().getKlasse().getName(),
        termine);
  }

  /**
   * Setzt den Fließtext aus den i18n-Bausteinen zusammen. Die Terminliste ist beliebig lang, daher
   * ist der Text nicht ein einzelner Format-String wie bei der Absage. Der Ort-Abschnitt entfällt
   * vollständig, wenn nichts hinterlegt ist — keine leere Zeile, keine leere Überschrift. Die Notiz
   * steht eingerückt unter ihrer Terminzeile; ein Termin ohne Notiz bleibt einzeilig.
   */
  private String baueText(Bestaetigung b) {
    List<String> absaetze = new ArrayList<>();
    absaetze.add(i18n.getTranslation("buchung.mail.greeting", LOCALE));
    absaetze.add(i18n.getTranslation("buchung.mail.intro", LOCALE));

    List<String> kopf = new ArrayList<>();
    kopf.add(
        i18n.getTranslation(
            "buchung.mail.sprechtag", LOCALE, b.sprechtagTitel(), Formats.dateLong(b.datum())));
    if (hatInhalt(b.ort())) {
      kopf.add(i18n.getTranslation("buchung.mail.ort", LOCALE, b.ort().trim()));
    }
    kopf.add(i18n.getTranslation("buchung.mail.kind", LOCALE, b.schuelerName(), b.klasse()));
    absaetze.add(String.join("\n", kopf));

    List<String> liste = new ArrayList<>();
    liste.add(i18n.getTranslation("buchung.mail.termine", LOCALE));
    for (TerminZeile termin : b.termine()) {
      liste.add(
          i18n.getTranslation(
              "buchung.mail.termin",
              LOCALE,
              Formats.time(termin.zeit()),
              termin.lehrkraft(),
              termin.fach()));
      if (hatInhalt(termin.notiz())) {
        liste.add(i18n.getTranslation("buchung.mail.notiz", LOCALE, termin.notiz().trim()));
      }
    }
    absaetze.add(String.join("\n", liste));

    absaetze.add(i18n.getTranslation("buchung.mail.hinweis", LOCALE));
    absaetze.add(i18n.getTranslation("buchung.mail.closing", LOCALE, properties.getSchoolname()));
    return String.join("\n\n", absaetze);
  }

  private static boolean hatInhalt(String wert) {
    return wert != null && !wert.isBlank();
  }
}
