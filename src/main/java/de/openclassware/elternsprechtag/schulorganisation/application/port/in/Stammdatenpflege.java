package de.openclassware.elternsprechtag.schulorganisation.application.port.in;

import java.util.UUID;

/**
 * Die Schreibseite dieses Kontexts: Stammdaten anlegen, aktualisieren, stilllegen.
 *
 * <p><b>Heute ruft sie im Betrieb niemand.</b> Sie ist die Naht, an der der Stammdaten-Import
 * andocken wird — der Import selbst gehört nicht zu diesem Ticket (#140), seine Abgleichregeln
 * (Wiedererkennung über eine Fremd-Id, Umgang mit Datensätzen, die im nächsten Export fehlen) werden
 * entschieden, wenn Quellsystem und Format bekannt sind. Dass die Naht trotzdem jetzt entsteht, hat
 * einen Grund: Sie legt fest, dass Stammdaten <em>stillgelegt</em> und nicht gelöscht werden, und
 * diese Entscheidung soll nicht der Import treffen.
 *
 * <p>Bis dahin sind die Service-Tests ihr einziger Aufrufer — sie legen ihre Stammdaten über diese
 * Ports an, sodass der Weg nicht unbenutzt und damit ungeprüft bleibt.
 *
 * <p><b>Warum es kein {@code aktualisiereLehrauftrag} gibt:</b> Ein Lehrauftrag ist sein Tripel aus
 * Lehrkraft, Klasse und Fach. Ein anderes Tripel ist ein anderer Lehrauftrag — der alte wird
 * stillgelegt, der neue erteilt. Siehe {@code Lehrauftrag}.
 */
public interface Stammdatenpflege {

  UUID legeLehrkraftAn(String vorname, String nachname, String kuerzel);

  /**
   * @throws de.openclassware.elternsprechtag.schulorganisation.domain.StammdatumNichtGefundenException
   *     wenn es die Lehrkraft nicht gibt
   * @throws de.openclassware.elternsprechtag.schulorganisation.domain.StammdatumStillgelegtException
   *     wenn sie stillgelegt ist
   */
  void aktualisiereLehrkraft(UUID id, String vorname, String nachname, String kuerzel);

  /** Nimmt die Lehrkraft aus dem Angebot, ohne sie zu löschen. Zweimal aufgerufen: unverändert. */
  void legeLehrkraftStill(UUID id);

  UUID legeKlasseAn(String name);

  void aktualisiereKlasse(UUID id, String name);

  void legeKlasseStill(UUID id);

  UUID legeFachAn(String name, String kuerzel);

  void aktualisiereFach(UUID id, String name, String kuerzel);

  void legeFachStill(UUID id);

  /**
   * @throws org.springframework.dao.DuplicateKeyException wenn es das Tripel schon gibt
   */
  UUID erteileLehrauftrag(UUID lehrkraftId, UUID klasseId, UUID fachId);

  void legeLehrauftragStill(UUID id);
}
