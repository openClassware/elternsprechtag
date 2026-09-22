package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.BelegZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Formuliert die Ausfall-Mail (#158) nach einem Ausfall-Vorgang (#156) und übergibt sie als fertige
 * {@link Nachricht} an den {@link BenachrichtigungSender}-Port — viertes Geschwister neben
 * {@link AbsageBenachrichtigungService}, {@link BuchungBestaetigungService} und
 * {@link ErinnerungBenachrichtigungService}. Ausgelöst wird sie nach Commit vom
 * {@link AusfallBenachrichtigungListener}.
 *
 * <p>Gebündelt wird nach Sprechtag und Eltern-<em>Adresse</em> — es gibt bewusst keine
 * Eltern-Entity, und der bestehende Code dedupliziert überall per Adresse. Wen es mit drei Terminen
 * trifft, bekommt eine Nachricht mit drei Positionen, nicht drei fast gleiche Mails.
 *
 * <p>Die Beleg-Query ist dieselbe wie bei Bestätigung und Erinnerung: Sie filtert nur nach
 * Buchungs-Id und trägt keinen Status-Filter — sie liefert also auch die soeben stornierten
 * Buchungen, um die es hier gerade geht.
 */
@RequiredArgsConstructor
@Service
@Slf4j
class AusfallBenachrichtigungService {

  private static final Locale LOCALE = Locale.GERMANY;

  private final BuchungsAnsichten buchungsAnsichten;
  private final SprechtagAnsichten sprechtagAnsichten;
  private final BenachrichtigungSender sender;
  private final I18NProvider i18n;
  private final ElternsprechtagProperties properties;

  private record Empfaenger(UUID sprechtagId, String elternEmail) {}

  /** Ein entfallener Termin dieser Familie an diesem Sprechtag. */
  private record AusfallPosition(LocalTime zeit, String lehrkraft, String fach) {}

  /**
   * Benachrichtigt die Familien genau dieser stornierten Buchungen — je Sprechtag und Adresse eine
   * Nachricht. Eine leere Id-Liste oder durchweg unbekannte Ids führen zu keinem Sende-Aufruf und zu
   * keinem Fehler; entfielen nur freie Slots, kommt hier gar nichts an. Der Versand ist
   * best-effort: Scheitert der Sender oder fehlt der Sprechtag inzwischen, wird der Fehler
   * protokolliert und mit den übrigen Adressen fortgefahren.
   */
  public void benachrichtige(List<BuchungId> buchungIds) {
    if (buchungIds == null || buchungIds.isEmpty()) {
      return;
    }
    List<BelegZeile> zeilen = buchungsAnsichten.belege(buchungIds);
    if (zeilen.isEmpty()) {
      return;
    }

    Map<Empfaenger, List<BelegZeile>> gruppen = new LinkedHashMap<>();
    for (BelegZeile zeile : zeilen) {
      gruppen
          .computeIfAbsent(
              new Empfaenger(zeile.sprechtagId(), zeile.elternEmail()), k -> new ArrayList<>())
          .add(zeile);
    }

    // Je Sprechtag genau einmal geladen: Ein Ausfall trifft in aller Regel viele Familien desselben
    // Sprechtags, und der Kopf ändert sich nicht zwischen zwei Gruppen.
    Map<UUID, Optional<SprechtagAnsichten.Kopf>> koepfe = new LinkedHashMap<>();
    for (Map.Entry<Empfaenger, List<BelegZeile>> gruppe : gruppen.entrySet()) {
      Optional<SprechtagAnsichten.Kopf> kopf =
          koepfe.computeIfAbsent(
              gruppe.getKey().sprechtagId(), id -> sprechtagAnsichten.kopf(SprechtagId.von(id)));
      sendeFuerFamilie(gruppe.getKey(), gruppe.getValue(), kopf);
    }
  }

  private void sendeFuerFamilie(
      Empfaenger empfaenger, List<BelegZeile> zeilen, Optional<SprechtagAnsichten.Kopf> kopf) {
    if (kopf.isEmpty()) {
      log.warn(
          "Ausfall-Benachrichtigung an {} übersprungen: Sprechtag nicht mehr gefunden",
          empfaenger.elternEmail());
      return;
    }
    try {
      String datum = Formats.dateLong(kopf.get().datum());
      String betreff = i18n.getTranslation("ausfall.mail.subject", LOCALE, kopf.get().titel(), datum);
      String text = baueText(kopf.get(), datum, positionen(zeilen));
      sender.sende(new Nachricht(empfaenger.elternEmail(), betreff, text));
    } catch (RuntimeException e) {
      // Best-effort: Einzelfehler (Bounce, fehlender Textbaustein) stoppen die übrigen Adressen
      // nicht — und keine Ausnahme entkommt dem @Async-Thread.
      log.warn(
          "Ausfall-Benachrichtigung an {} fehlgeschlagen: {}", empfaenger.elternEmail(), e.getMessage());
    }
  }

  private List<AusfallPosition> positionen(List<BelegZeile> zeilen) {
    List<AusfallPosition> positionen = new ArrayList<>();
    for (BelegZeile zeile : zeilen) {
      positionen.add(new AusfallPosition(zeile.zeit(), zeile.lehrkraftName(), zeile.fach()));
    }
    return positionen;
  }

  /**
   * Setzt den Fließtext aus den i18n-Bausteinen zusammen — wie die Erinnerung, aber <b>ohne Ort</b>:
   * Für einen Termin, der nicht stattfindet, ist er gegenstandslos. Wie die Absage-Mail enthält der
   * Text keine Aktion, insbesondere keinen Zugangs-Link, sondern verweist an die Schule; der
   * Schulkontakt steht dafür als eigener, beschrifteter Absatz vor der Grußformel.
   *
   * <p>Und <b>ohne Kind-Zeile</b>, anders als Bestätigung und Erinnerung. Die Bündelung geht über
   * die Adresse, nicht über ein Kind: Zwei Geschwister an einem Sprechtag teilen sich die
   * Eltern-Adresse, und eine aus der ersten Zeile abgeleitete Kopfzeile nennte dann eines von
   * beiden, während darunter die Termine <em>beider</em> stehen. Die Spec verlangt die Zeile nicht
   * — und eine, die falsch sein kann, ist schlechter als keine. Wer welchen Termin hatte, steht an
   * der Position: Uhrzeit, Lehrkraft, Fach.
   */
  private String baueText(
      SprechtagAnsichten.Kopf sprechtag, String datum, List<AusfallPosition> termine) {
    List<String> absaetze = new ArrayList<>();
    absaetze.add(i18n.getTranslation("ausfall.mail.greeting", LOCALE));
    absaetze.add(i18n.getTranslation("ausfall.mail.intro", LOCALE, sprechtag.titel(), datum));
    absaetze.add(i18n.getTranslation("ausfall.mail.sprechtag", LOCALE, sprechtag.titel(), datum));

    List<String> liste = new ArrayList<>();
    liste.add(i18n.getTranslation("ausfall.mail.termine", LOCALE));
    for (AusfallPosition termin : termine) {
      liste.add(
          i18n.getTranslation(
              "ausfall.mail.termin",
              LOCALE,
              Formats.time(termin.zeit()),
              termin.lehrkraft(),
              termin.fach()));
    }
    absaetze.add(String.join("\n", liste));

    absaetze.add(i18n.getTranslation("ausfall.mail.hinweis", LOCALE));
    absaetze.add(
        i18n.getTranslation("ausfall.mail.schulkontakt", LOCALE, sprechtag.schulkontakt().trim()));
    absaetze.add(i18n.getTranslation("ausfall.mail.closing", LOCALE, properties.getSchoolname()));
    return String.join("\n\n", absaetze);
  }
}
