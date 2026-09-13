package de.openclassware.elternsprechtag.services;

import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.services.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.BelegZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.ui.Formats;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Formuliert nach einem Buchungsvorgang die Bestätigungsmail und übergibt sie als fertige
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

  private final BuchungsAnsichten buchungsAnsichten;
  private final SprechtagAnsichten sprechtagAnsichten;
  private final BenachrichtigungSender sender;
  private final I18NProvider i18n;
  private final ElternsprechtagProperties properties;

  /**
   * Ein bestätigter Termin des Vorgangs: wann, bei wem, in welchem Fach — und die Notiz, die genau
   * dieser Lehrkraft gilt. {@code notiz} darf {@code null} sein, dann entfällt die Notiz-Zeile.
   */
  record TerminPosition(LocalTime zeit, String lehrkraft, String fach, String notiz) {}

  /**
   * Der komplette Beleg eines Absendevorgangs. Empfänger und Kopfdaten sind aus den geladenen
   * Buchungen abgeleitet, nicht aus der Anfrage übernommen. {@code ort} darf {@code null} sein —
   * dann entfällt der Ort-Abschnitt; {@code schulkontakt} ist am Sprechtag Pflicht.
   */
  record Bestaetigung(
      String empfaenger,
      String sprechtagTitel,
      LocalDate datum,
      String ort,
      String schulkontakt,
      String schuelerName,
      String klasse,
      List<TerminPosition> termine) {}

  /**
   * Bestätigt genau die Buchungen dieses Absendevorgangs mit <b>einer</b> Nachricht an die dort
   * hinterlegte Adresse. Eine leere Id-Liste oder durchweg unbekannte Ids führen zu keinem
   * Sende-Aufruf und zu keinem Fehler. Der Versand ist best-effort: scheitert der Sender, wird der
   * Fehler per {@code log.warn} protokolliert — die Buchung bleibt gültig, es gibt kein Retry.
   * Betreff und Text stammen aus {@code vaadin-i18n/translations.properties} (direkt über den
   * {@link I18NProvider}, weil der Versand ohne Vaadin-{@code UI}-Kontext läuft), Datum und Uhrzeit
   * aus {@link Formats}.
   *
   * <p>Bewusst <b>ohne</b> eigenes {@code @Transactional}: Die beiden Abfragen sind je für sich
   * abgeschlossen. So hält der Versand keine DB-Verbindung — ein hängender SMTP-Server (JavaMail
   * wartet voreingestellt unbegrenzt) könnte sonst je Buchung eine Verbindung des Pools blockieren
   * und den Pool erschöpfen, während der Versand hier bei <em>jeder</em> Buchung läuft.
   *
   * <p>Dass die Ids nach dem Commit überhaupt noch gelten, liegt daran, dass die Domäne sie selbst
   * vergibt und ein erneutes Speichern des Termin-Aggregats sie nicht verändert (ADR 0004).
   */
  public void bestaetige(List<BuchungId> buchungIds) {
    if (buchungIds == null || buchungIds.isEmpty()) {
      return;
    }
    // Die Query sortiert bereits nach Startzeit; die Mail listet damit chronologisch.
    List<BelegZeile> zeilen = buchungsAnsichten.belege(buchungIds);
    if (zeilen.isEmpty()) {
      // Unbekannte Ids (etwa nach zwischenzeitlicher Löschung): nichts zu bestätigen, kein Fehler.
      return;
    }

    // Empfänger vorab, damit ein Fehler beim Formulieren die Adresse trotzdem protokollieren kann.
    String empfaenger = zeilen.get(0).elternEmail();
    try {
      Bestaetigung bestaetigung = zuBestaetigung(zeilen);
      String betreff =
          i18n.getTranslation(
              "buchung.mail.subject",
              LOCALE,
              bestaetigung.sprechtagTitel(),
              Formats.dateLong(bestaetigung.datum()));
      sender.sende(new Nachricht(bestaetigung.empfaenger(), betreff, baueText(bestaetigung)));
    } catch (RuntimeException e) {
      // Best-effort: Weder ein Zustellproblem noch ein Fehler beim Formulieren (fehlender
      // Textbaustein, verschwundener Sprechtag) darf die festgeschriebene Buchung entwerten oder
      // als unbehandelte Ausnahme aus dem @Async-Thread entkommen.
      log.warn("Buchungsbestätigung an {} fehlgeschlagen: {}", empfaenger, e.getMessage());
    }
  }

  /**
   * Fügt Beleg-Zeilen und Sprechtag-Kopf zum Record zusammen. Alle Buchungen eines Vorgangs teilen
   * Adresse, Kind und Sprechtag; abgeleitet werden sie aus der ersten Zeile. Der Kopf kommt aus
   * einem zweiten Port — kein Join über die Grenze der Aggregate.
   */
  private Bestaetigung zuBestaetigung(List<BelegZeile> zeilen) {
    BelegZeile erste = zeilen.get(0);
    Optional<SprechtagAnsichten.Kopf> kopf =
        sprechtagAnsichten.kopf(SprechtagId.von(erste.sprechtagId()));
    if (kopf.isEmpty()) {
      throw new IllegalStateException("Sprechtag zur Buchung nicht gefunden: " + erste.sprechtagId());
    }

    List<TerminPosition> termine = new ArrayList<>();
    for (BelegZeile zeile : zeilen) {
      termine.add(
          new TerminPosition(zeile.zeit(), zeile.lehrkraftName(), zeile.fach(), zeile.notiz()));
    }

    return new Bestaetigung(
        erste.elternEmail(),
        kopf.get().titel(),
        kopf.get().datum(),
        kopf.get().ort(),
        kopf.get().schulkontakt(),
        erste.schuelerName(),
        erste.klasse(),
        termine);
  }

  /**
   * Setzt den Fließtext aus den i18n-Bausteinen zusammen. Die Terminliste ist beliebig lang, daher
   * ist der Text nicht ein einzelner Format-String wie bei der Absage. Der Ort-Abschnitt entfällt
   * vollständig, wenn nichts hinterlegt ist — keine leere Zeile, keine leere Überschrift; der
   * Schulkontakt ist Pflicht und daher immer dabei. Die Notiz steht eingerückt unter ihrer
   * Terminzeile; ein Termin ohne Notiz bleibt einzeilig.
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
    for (TerminPosition termin : b.termine()) {
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
    // Eigener Absatz mit eigener Beschriftung — der Schulkontakt ist mehrzeiliger Freitext und
    // passt in keinen laufenden Satz. Er ist ab dem Entwurf Pflicht, kann hier also nicht fehlen.
    absaetze.add(i18n.getTranslation("buchung.mail.schulkontakt", LOCALE, b.schulkontakt().trim()));
    absaetze.add(i18n.getTranslation("buchung.mail.closing", LOCALE, properties.getSchoolname()));
    return String.join("\n\n", absaetze);
  }

  private static boolean hatInhalt(String wert) {
    return wert != null && !wert.isBlank();
  }
}
