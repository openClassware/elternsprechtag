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
 * Formuliert die Erinnerungsmail vor dem Sprechtag und übergibt sie als fertige {@link Nachricht}
 * an den {@link BenachrichtigungSender}-Port — drittes Geschwister neben
 * {@link AbsageBenachrichtigungService} und {@link BuchungBestaetigungService}. Ausgelöst wird sie
 * nach Commit vom {@link ErinnerungBenachrichtigungListener}, den {@code ErinnernService} am Ende
 * eines Scheduler-Laufs bestückt.
 *
 * <p>Ein Lauf erinnert typischerweise mehrere Familien an mehreren Sprechtagen in einem Zug; die
 * Buchungen werden deshalb erst nach Sprechtag und Eltern-Adresse gruppiert — eine Familie mit
 * mehreren Terminen am selben Sprechtag bekommt eine Mail, keine je Termin.
 */
@RequiredArgsConstructor
@Service
@Slf4j
class ErinnerungBenachrichtigungService {

  private static final Locale LOCALE = Locale.GERMANY;

  private final BuchungsAnsichten buchungsAnsichten;
  private final SprechtagAnsichten sprechtagAnsichten;
  private final BenachrichtigungSender sender;
  private final I18NProvider i18n;
  private final ElternsprechtagProperties properties;

  private record Empfaenger(UUID sprechtagId, String elternEmail) {}

  /** Ein erinnerter Termin dieser Familie an diesem Sprechtag. */
  private record ErinnerungsPosition(LocalTime zeit, String lehrkraft, String fach) {}

  /**
   * Erinnert die Familien genau dieser Buchungen — je Sprechtag und Adresse eine Nachricht. Eine
   * leere Id-Liste oder durchweg unbekannte Ids führen zu keinem Sende-Aufruf und zu keinem Fehler.
   * Der Versand ist best-effort: Scheitert der Sender oder fehlt der Sprechtag inzwischen, wird der
   * Fehler protokolliert und mit den übrigen Gruppen fortgefahren.
   */
  public void erinnere(List<BuchungId> buchungIds) {
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
          .computeIfAbsent(new Empfaenger(zeile.sprechtagId(), zeile.elternEmail()), k -> new ArrayList<>())
          .add(zeile);
    }

    // Je Sprechtag genau einmal geladen: Ein Lauf erinnert typischerweise viele Familien
    // desselben Sprechtags, und der Kopf ändert sich nicht zwischen zwei Gruppen.
    Map<UUID, Optional<SprechtagAnsichten.Kopf>> koepfe = new LinkedHashMap<>();
    for (Map.Entry<Empfaenger, List<BelegZeile>> gruppe : gruppen.entrySet()) {
      Optional<SprechtagAnsichten.Kopf> kopf =
          koepfe.computeIfAbsent(
              gruppe.getKey().sprechtagId(),
              id -> sprechtagAnsichten.kopf(SprechtagId.von(id)));
      sendeFuerFamilie(gruppe.getKey(), gruppe.getValue(), kopf);
    }
  }

  private void sendeFuerFamilie(
      Empfaenger empfaenger, List<BelegZeile> zeilen, Optional<SprechtagAnsichten.Kopf> kopf) {
    if (kopf.isEmpty()) {
      log.warn("Erinnerung an {} übersprungen: Sprechtag nicht mehr gefunden", empfaenger.elternEmail());
      return;
    }
    try {
      String datum = Formats.dateLong(kopf.get().datum());
      String betreff = i18n.getTranslation("erinnerung.mail.subject", LOCALE, kopf.get().titel(), datum);
      String text = baueText(kopf.get(), datum, zeilen.get(0), positionen(zeilen));
      sender.sende(new Nachricht(empfaenger.elternEmail(), betreff, text));
    } catch (RuntimeException e) {
      log.warn("Erinnerung an {} fehlgeschlagen: {}", empfaenger.elternEmail(), e.getMessage());
    }
  }

  private List<ErinnerungsPosition> positionen(List<BelegZeile> zeilen) {
    List<ErinnerungsPosition> positionen = new ArrayList<>();
    for (BelegZeile zeile : zeilen) {
      positionen.add(new ErinnerungsPosition(zeile.zeit(), zeile.lehrkraftName(), zeile.fach()));
    }
    return positionen;
  }

  /**
   * Setzt den Fließtext aus den i18n-Bausteinen zusammen — Spiegelbild von
   * {@code BuchungBestaetigungService.baueText}, ohne Notiz: Die Erinnerung fasst nur zusammen,
   * was bereits bestätigt ist.
   */
  private String baueText(
      SprechtagAnsichten.Kopf sprechtag, String datum, BelegZeile erste, List<ErinnerungsPosition> termine) {
    List<String> absaetze = new ArrayList<>();
    absaetze.add(i18n.getTranslation("erinnerung.mail.greeting", LOCALE));
    absaetze.add(i18n.getTranslation("erinnerung.mail.intro", LOCALE, sprechtag.titel(), datum));

    List<String> kopf = new ArrayList<>();
    kopf.add(i18n.getTranslation("erinnerung.mail.sprechtag", LOCALE, sprechtag.titel(), datum));
    if (hatInhalt(sprechtag.ort())) {
      kopf.add(i18n.getTranslation("erinnerung.mail.ort", LOCALE, sprechtag.ort().trim()));
    }
    kopf.add(i18n.getTranslation("erinnerung.mail.kind", LOCALE, erste.schuelerName(), erste.klasse()));
    absaetze.add(String.join("\n", kopf));

    List<String> liste = new ArrayList<>();
    liste.add(i18n.getTranslation("erinnerung.mail.termine", LOCALE));
    for (ErinnerungsPosition termin : termine) {
      liste.add(
          i18n.getTranslation(
              "erinnerung.mail.termin", LOCALE, Formats.time(termin.zeit()), termin.lehrkraft(), termin.fach()));
    }
    absaetze.add(String.join("\n", liste));

    absaetze.add(
        i18n.getTranslation(
            "erinnerung.mail.schulkontakt", LOCALE, sprechtag.schulkontakt().trim()));
    absaetze.add(i18n.getTranslation("erinnerung.mail.closing", LOCALE, properties.getSchoolname()));
    return String.join("\n\n", absaetze);
  }

  private static boolean hatInhalt(String wert) {
    return wert != null && !wert.isBlank();
  }
}
