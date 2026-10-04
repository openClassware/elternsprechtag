package de.openclassware.elternsprechtag.sprechtag.adapter.out.mail;

import com.vaadin.flow.i18n.I18NProvider;
import de.openclassware.elternsprechtag.config.ElternsprechtagProperties;
import de.openclassware.elternsprechtag.sprechtag.adapter.Elternlink;
import de.openclassware.elternsprechtag.sprechtag.adapter.Formats;
import de.openclassware.elternsprechtag.sprechtag.adapter.out.mail.BenachrichtigungSender.Nachricht;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.Benachrichtigungen.Versand;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.BuchungsAnsichten.BelegZeile;
import de.openclassware.elternsprechtag.sprechtag.application.port.out.SprechtagAnsichten;
import de.openclassware.elternsprechtag.sprechtag.domain.BuchungId;
import de.openclassware.elternsprechtag.sprechtag.domain.SprechtagId;
import de.openclassware.elternsprechtag.sprechtag.domain.Zustellergebnis;
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
 * Formuliert die Ausfall-Mail der Sammelaktion „Lehrkraft fällt aus" (Issue #156) und übergibt sie
 * als fertige {@link Nachricht} an den {@link BenachrichtigungSender}-Port — Spiegelbild von
 * {@link ErinnerungBenachrichtigungService}. Ausgelöst wird sie über {@link MailBenachrichtigungen}.
 *
 * <p>Eine Sammelaktion trifft typischerweise mehrere Familien; die stornierten Buchungen werden
 * deshalb je {@link Empfaenger} gruppiert — ein Kind mit zwei betroffenen Terminen bekommt eine Mail
 * mit zwei Positionen, nicht zwei Mails; Geschwister an derselben Adresse je eine eigene (ADR 0007).
 * Die wiederverwendete Beleg-Query filtert nicht nach Status; sie liefert deshalb auch die gerade
 * stornierten Buchungen.
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
  private final Elternlink elternlink;

  /** Ein entfallener Termin dieses Kindes an diesem Sprechtag. */
  private record AusfallPosition(LocalTime zeit, String lehrkraft, String fach) {}

  /**
   * Benachrichtigt die Familien genau dieser stornierten Buchungen — je Sprechtag, Adresse und Kind
   * eine Nachricht. Eine leere Id-Liste oder durchweg unbekannte Ids führen zu keinem Sende-Aufruf und
   * zu keinem Fehler. Best-effort: Scheitert der Sender, wird der Fehler protokolliert, als
   * {@link Zustellergebnis#FEHLGESCHLAGEN} gemeldet und mit den übrigen Gruppen fortgefahren; fehlt
   * der Sprechtag inzwischen, entsteht für die Gruppe gar keine Nachricht.
   *
   * @return je verschickter oder gescheiterter Nachricht die Buchungen, die sie trug
   */
  public List<Versand> benachrichtige(List<BuchungId> buchungIds, boolean nachbuchbar) {
    if (buchungIds == null || buchungIds.isEmpty()) {
      return List.of();
    }
    List<BelegZeile> zeilen = buchungsAnsichten.belege(buchungIds);
    if (zeilen.isEmpty()) {
      return List.of();
    }

    Map<Empfaenger, List<BelegZeile>> gruppen = Empfaenger.gruppiere(zeilen);

    // Je Sprechtag genau einmal geladen: Eine Sammelaktion betrifft typischerweise viele Familien
    // desselben Sprechtags, und der Kopf ändert sich nicht zwischen zwei Gruppen.
    Map<UUID, Optional<SprechtagAnsichten.Kopf>> koepfe = new LinkedHashMap<>();
    List<Versand> versand = new ArrayList<>();
    for (Map.Entry<Empfaenger, List<BelegZeile>> gruppe : gruppen.entrySet()) {
      Optional<SprechtagAnsichten.Kopf> kopf =
          koepfe.computeIfAbsent(
              gruppe.getKey().sprechtagId(),
              id -> sprechtagAnsichten.kopf(SprechtagId.von(id)));
      sendeAn(gruppe.getKey(), gruppe.getValue(), kopf, nachbuchbar).ifPresent(versand::add);
    }
    return versand;
  }

  private Optional<Versand> sendeAn(
      Empfaenger empfaenger,
      List<BelegZeile> zeilen,
      Optional<SprechtagAnsichten.Kopf> kopf,
      boolean nachbuchbar) {
    if (kopf.isEmpty()) {
      log.warn(
          "Ausfall-Benachrichtigung an {} übersprungen: Sprechtag nicht mehr gefunden",
          empfaenger.elternEmail());
      return Optional.empty();
    }
    List<BuchungId> buchungen = zeilen.stream().map(BelegZeile::buchung).toList();
    try {
      String datum = Formats.dateLong(kopf.get().datum());
      String betreff = i18n.getTranslation("ausfall.mail.subject", LOCALE, kopf.get().titel(), datum);
      String text = baueText(kopf.get(), datum, empfaenger, positionen(zeilen), nachbuchbar);
      sender.sende(new Nachricht(empfaenger.elternEmail(), betreff, text));
      return Optional.of(new Versand(buchungen, Zustellergebnis.ABGESCHICKT));
    } catch (RuntimeException e) {
      log.warn("Ausfall-Benachrichtigung an {} fehlgeschlagen: {}", empfaenger.elternEmail(), e.getMessage());
      return Optional.of(new Versand(buchungen, Zustellergebnis.FEHLGESCHLAGEN));
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
   * Setzt den Fließtext aus den i18n-Bausteinen zusammen — Spiegelbild von
   * {@code ErinnerungBenachrichtigungService.baueText}, ohne Ort und ohne Notiz. Nimmt der
   * Sprechtag noch Elternbuchungen an, führt ein eigener Absatz mit dem Elternlink zurück in die
   * Buchung (Issue #109) — der Link allein auf einer Zeile, damit jedes Mailprogramm ihn klickbar
   * macht. Sonst verweist die Mail wie bisher nur an die Schule; der Schulkontakt steht in beiden
   * Fällen.
   */
  private String baueText(
      SprechtagAnsichten.Kopf sprechtag,
      String datum,
      Empfaenger empfaenger,
      List<AusfallPosition> termine,
      boolean nachbuchbar) {
    List<String> absaetze = new ArrayList<>();
    absaetze.add(i18n.getTranslation("ausfall.mail.greeting", LOCALE));
    absaetze.add(i18n.getTranslation("ausfall.mail.intro", LOCALE, sprechtag.titel(), datum));
    absaetze.add(
        i18n.getTranslation(
            "ausfall.mail.kind", LOCALE, empfaenger.schuelerName(), empfaenger.klasse()));

    List<String> liste = new ArrayList<>();
    liste.add(i18n.getTranslation("ausfall.mail.termine", LOCALE));
    for (AusfallPosition termin : termine) {
      liste.add(
          i18n.getTranslation(
              "ausfall.mail.termin", LOCALE, Formats.time(termin.zeit()), termin.lehrkraft(), termin.fach()));
    }
    absaetze.add(String.join("\n", liste));

    if (nachbuchbar) {
      absaetze.add(
          i18n.getTranslation(
              "ausfall.mail.nachbuchen", LOCALE, elternlink.zu(sprechtag.accessToken())));
    }

    absaetze.add(
        i18n.getTranslation("ausfall.mail.schulkontakt", LOCALE, sprechtag.schulkontakt().trim()));
    absaetze.add(i18n.getTranslation("ausfall.mail.closing", LOCALE, properties.getSchoolname()));
    return String.join("\n\n", absaetze);
  }
}
