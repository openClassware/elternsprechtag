package de.openclassware.elternsprechtag.schulorganisation.application.service;

import de.openclassware.elternsprechtag.schulorganisation.application.port.in.Stammdatenpflege;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Faecher;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Klassen;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Lehrauftraege;
import de.openclassware.elternsprechtag.schulorganisation.application.port.out.Lehrkraefte;
import de.openclassware.elternsprechtag.schulorganisation.domain.Fach;
import de.openclassware.elternsprechtag.schulorganisation.domain.FachId;
import de.openclassware.elternsprechtag.schulorganisation.domain.Klasse;
import de.openclassware.elternsprechtag.schulorganisation.domain.KlasseId;
import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrauftrag;
import de.openclassware.elternsprechtag.schulorganisation.domain.LehrauftragId;
import de.openclassware.elternsprechtag.schulorganisation.domain.Lehrkraft;
import de.openclassware.elternsprechtag.schulorganisation.domain.LehrkraftId;
import de.openclassware.elternsprechtag.schulorganisation.domain.StammdatumNichtGefundenException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Führt die Schreibvorgänge der Stammdatenpflege aus: laden, das Aggregat entscheiden lassen,
 * speichern.
 *
 * <p>Jede Methode fasst genau ein Aggregat an. Das ist keine Bescheidenheit, sondern die Regel: Ein
 * Vorgang, der zwei Wurzeln in einer Transaktion änderte, bräuchte eine Begründung wie ADR 0005 —
 * und hier gibt es keine. Ein Lehrauftrag prüft deshalb auch nicht nach, ob es seine Lehrkraft
 * wirklich gibt; das tun die Fremdschlüssel der Datenbank.
 */
@RequiredArgsConstructor
@Service
class StammdatenpflegeService implements Stammdatenpflege {

  private final Lehrkraefte lehrkraefte;
  private final Klassen klassen;
  private final Faecher faecher;
  private final Lehrauftraege lehrauftraege;

  @Override
  @Transactional
  public UUID legeLehrkraftAn(String vorname, String nachname, String kuerzel) {
    Lehrkraft lehrkraft = Lehrkraft.stelleEin(vorname, nachname, kuerzel);
    lehrkraefte.speichere(lehrkraft);
    return lehrkraft.id().wert();
  }

  @Override
  @Transactional
  public void aktualisiereLehrkraft(UUID id, String vorname, String nachname, String kuerzel) {
    Lehrkraft lehrkraft = ladeLehrkraft(id);
    lehrkraft.benenne(vorname, nachname, kuerzel);
    lehrkraefte.speichere(lehrkraft);
  }

  @Override
  @Transactional
  public void legeLehrkraftStill(UUID id) {
    Lehrkraft lehrkraft = ladeLehrkraft(id);
    lehrkraft.legeStill();
    lehrkraefte.speichere(lehrkraft);
  }

  @Override
  @Transactional
  public UUID legeKlasseAn(String name) {
    Klasse klasse = Klasse.richteEin(name);
    klassen.speichere(klasse);
    return klasse.id().wert();
  }

  @Override
  @Transactional
  public void aktualisiereKlasse(UUID id, String name) {
    Klasse klasse = ladeKlasse(id);
    klasse.benenne(name);
    klassen.speichere(klasse);
  }

  @Override
  @Transactional
  public void legeKlasseStill(UUID id) {
    Klasse klasse = ladeKlasse(id);
    klasse.legeStill();
    klassen.speichere(klasse);
  }

  @Override
  @Transactional
  public UUID legeFachAn(String name, String kuerzel) {
    Fach fach = Fach.fuehreEin(name, kuerzel);
    faecher.speichere(fach);
    return fach.id().wert();
  }

  @Override
  @Transactional
  public void aktualisiereFach(UUID id, String name, String kuerzel) {
    Fach fach = ladeFach(id);
    fach.benenne(name, kuerzel);
    faecher.speichere(fach);
  }

  @Override
  @Transactional
  public void legeFachStill(UUID id) {
    Fach fach = ladeFach(id);
    fach.legeStill();
    faecher.speichere(fach);
  }

  @Override
  @Transactional
  public UUID erteileLehrauftrag(UUID lehrkraftId, UUID klasseId, UUID fachId) {
    Lehrauftrag lehrauftrag =
        Lehrauftrag.erteile(
            LehrkraftId.von(lehrkraftId), KlasseId.von(klasseId), FachId.von(fachId));
    lehrauftraege.speichere(lehrauftrag);
    return lehrauftrag.id().wert();
  }

  @Override
  @Transactional
  public void legeLehrauftragStill(UUID id) {
    Lehrauftrag lehrauftrag =
        lehrauftraege
            .lade(LehrauftragId.von(id))
            .orElseThrow(() -> nichtGefunden("Lehrauftrag", id));
    lehrauftrag.legeStill();
    lehrauftraege.speichere(lehrauftrag);
  }

  private Lehrkraft ladeLehrkraft(UUID id) {
    return lehrkraefte.lade(LehrkraftId.von(id)).orElseThrow(() -> nichtGefunden("Lehrkraft", id));
  }

  private Klasse ladeKlasse(UUID id) {
    return klassen.lade(KlasseId.von(id)).orElseThrow(() -> nichtGefunden("Klasse", id));
  }

  private Fach ladeFach(UUID id) {
    return faecher.lade(FachId.von(id)).orElseThrow(() -> nichtGefunden("Fach", id));
  }

  private static StammdatumNichtGefundenException nichtGefunden(String was, UUID id) {
    return new StammdatumNichtGefundenException(was + " nicht gefunden: " + id);
  }
}
