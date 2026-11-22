# Recherche: Werkzeug für die Massenumbenennung

Frage aus [#246](https://github.com/openClassware/elternsprechtag/issues/246) (Karte
[#243](https://github.com/openClassware/elternsprechtag/issues/243)): Mit welchem Werkzeug lässt
sich eine Umbenennungs-Tabelle alt → neu verlässlich auf diese Codebasis anwenden?

Stand der Recherche: **2026-11-22**, Code auf `origin/main` (`2cebf80`). Belege sind so markiert:

- **[Lauf]**: in einem Worktree tatsächlich ausgeführt, danach zurückgesetzt.
- **[Repo]**: im Code nachgelesen oder gezählt.
- **[Doku]**: nur aus der genannten Primärquelle, hier nicht nachgeprüft.

## Kurzfassung

**Empfehlung: OpenRewrite als Hauptwerkzeug, aber nicht allein.** Die Tabelle zerfällt in vier
Spuren:

| Spur | Werkzeug | Status |
|---|---|---|
| Typen, Packages, explizit deklarierte Methoden | OpenRewrite `ChangeType`, `ChangePackage`, `ChangeMethodName` | **belegt** [Lauf] |
| Felder, Record-Komponenten, Lombok-Accessoren | eigenes OpenRewrite-Rezept, Ersatzweg IntelliJ `rename_refactoring` | **offen, vor dem ersten PR zu klären** |
| i18n-Keys, CSS-Klassen und -Dateien, Screenshot-Selektoren | kleines Skript mit exakten Tokens aus derselben Tabelle | Bestand gezählt [Repo], Skript nicht gebaut |
| Was gleich bleiben muss | Sperrliste plus Prüfungen vor und nach dem Lauf | Mapping-Vergleich belegt [Lauf] |

Die fünf Befunde, die den Plan bestimmen:

1. **Die Standardrezepte decken Felder und Records nicht ab.** `ChangeMethodName` auf einem
   Lombok-Getter oder Record-Accessor ändert nur die Aufrufer, der Build bricht [Lauf]. Betroffen
   sind 93 Records und 9 Klassen mit `@Getter` [Repo]. `ChangeFieldName` ist kein aufrufbares
   Rezept, sondern ein Visitor ([Quelle](https://github.com/openrewrite/rewrite/blob/main/rewrite-java/src/main/java/org/openrewrite/java/ChangeFieldName.java)).
2. **Jedes umbenannte Persistenzfeld ändert still seine Spalte.** Das Repo hat **null** `@Column`;
   alle 59 Spalten hängen am Feldnamen. `startzeit` → `startTime` mappt auf `start_time`, mit
   `@Column("startzeit")` ist das Mapping wieder identisch [Lauf].
3. **Eine öffentliche Env-Variable hängt am Feldnamen.** `ELTERNSPRECHTAG_STELLVERTRETERADRESSE`
   steht nicht in `application.properties`, sondern bindet nur über das Feld in
   `ElternsprechtagProperties` [Repo]. Diese Klasse muss auf die Sperrliste.
4. **Enum-Konstanten sind gespeicherte Daten.** Sie stehen als Text in der Datenbank, in
   `CHECK`-Constraints und in SQL-Strings [Repo]. Auch sie gehören auf die Sperrliste.
5. **Migrationen dürfen nicht mitlaufen.** `V12__zustellungen.sql:11` nennt `TerminZeile` in einem
   Kommentar. Eine Textersetzung dort änderte die Prüfsumme, und Flyway meldet dann einen
   `CHECKSUM_MISMATCH` ([Flyway Validate](https://documentation.red-gate.com/flyway/reference/commands/validate),
   [Error Codes](https://documentation.red-gate.com/flyway/reference/usage/error-codes)).

## Vergleich der Kandidaten

| Kriterium | OpenRewrite 6.46.1 (Maven-Plugin) | IntelliJ 2026.1 | Textersetzung (`sed`, Skript) |
|---|---|---|---|
| Typen und Packages | ja, inkl. Dateiverschiebung [Lauf] | ja [Doku] | unsicher |
| Methoden mit Overrides | ja [Lauf] | ja [Doku] | nein |
| Lombok-Accessoren | nur Aufrufer, Build bricht [Lauf] | laut Hersteller ja, nicht geprüft [Doku] | nein |
| Record-Komponenten | nur Aufrufer, Build bricht [Lauf] | nicht geprüft | nein |
| `{@link}` | ja, Typ und Methode [Lauf] | nicht geprüft | nur zufällig |
| i18n, CSS, Selektoren | nein ([Quelle](https://github.com/openrewrite/rewrite/blob/main/rewrite-java/src/main/java/org/openrewrite/java/ChangeType.java)) | nur als Textfund | **ja, mit exakten Tokens** |
| Scriptbar ohne laufende IDE | ja [Lauf] | **nein**, siehe unten | ja |
| Wiederholbar | ja, zweiter Lauf ohne Änderung [Lauf] | nicht geprüft | nur bei sauberer Tabelle |
| Lizenz | Apache 2.0 ([Quelle](https://docs.openrewrite.org/licensing/openrewrite-licensing)) | nicht geprüft | – |

**IntelliJ.** Die offizielle Kommandozeile kennt `diff`, `merge`, `format`, `inspect` und
`installPlugins`, aber kein Refactoring
([Doku](https://www.jetbrains.com/help/idea/working-with-the-ide-features-from-command-line.html)).
Scriptbar ist Rename nur über den seit 2025.2 enthaltenen MCP-Server mit dem Tool
`rename_refactoring` ([Doku](https://www.jetbrains.com/help/idea/mcp-server.html)). Das geht Symbol
für Symbol; dass dafür eine laufende IDE nötig ist, legt die Doku nahe, sagt es aber nicht
ausdrücklich. Lombok ist laut JetBrains gebündelt und Rename funktioniert damit
([Blog, 2023](https://blog.jetbrains.com/idea/2023/04/turbocharge-lombok-annotations-with-intellij-idea/)).
Ob dabei die generierten Accessoren aller Aufrufer mitwandern, ist **nicht geprüft**.

**Reine Textersetzung** scheidet für Java aus: Die Bestandsaufnahme nennt Homographen wie `tag`,
`plan`, `kind`, `art`, `still`
([#244](https://github.com/openClassware/elternsprechtag/issues/244#issuecomment-7082174646)).
Für i18n-Keys und CSS-Klassen taugt sie, weil das dort ganze, eindeutige Tokens sind.

**Spoon, Eclipse JDT** wurden nicht bewertet. Die geprüfte Spoon-Seite sagt nichts zu Rename oder
Formaterhalt, und OpenRewrite trägt die belegten Spuren bereits.

## Versionen und Kompatibilität

| Komponente | Version | Quelle |
|---|---|---|
| Java | 25 (CI: Temurin 25) | `pom.xml:34`, `.github/workflows/ci.yml:62-63` |
| Spring Boot | 4.1.2 | `pom.xml:8` |
| Lombok | 1.18.48 | BOM von Spring Boot 4.1.2 |
| Spring Data | 2026.0.2 | BOM von Spring Boot 4.1.2 |
| Vaadin | 25.3.4 | `pom.xml:36` |
| Maven | 3.10.0 (Wrapper) | `.mvn/wrapper/maven-wrapper.properties` |
| rewrite-maven-plugin | 6.46.1, zieht `rewrite-java-lombok` 8.89.0 | Maven Central, Lauf-Log |

Das Plugin lief ohne Änderung an der `pom.xml` über die Kommandozeile, auf **JBR 25.0.3** statt
Temurin 25. Die Log-Suche nach Parse-Problemen blieb leer, und der Treffer auf `getStartzeit()`
zeigt, dass Lombok-Methoden typattribuiert werden [Lauf].

## Befunde je Punkt

### 1. Lombok

Genutzt werden [Repo]: `@RequiredArgsConstructor` (54), `@Slf4j` (13), `@Getter` (9), `@Setter` (8),
`@AllArgsConstructor`, `@NoArgsConstructor` und `@EqualsAndHashCode` (je 7). Es gibt keine
`lombok.config`, kein `@Data`, `@Builder` oder `@With`. Die 7 `@Value` sind Springs Annotation.

- **Accessoren** liegen an den 7 Persistenzmodellen (`*Zeile`), an `ElternsprechtagProperties` und
  an einem Feld in `FormPanel.java:13`. Die Fläche ist also klein.
- **Probe:** `ChangeMethodName` auf `TerminZeile getStartzeit()` änderte nur `TerminMapper.java:39`.
  Das Feld blieb, der Compiler schlug fehl [Lauf].
- **Folge:** Feld und Accessoren müssen in einem Schritt umbenannt werden.
- `@EqualsAndHashCode(of = "id")` referenziert per String nur `id`, das bleibt.
- MapStruct steht in der `pom.xml`, es gibt aber **kein** `@Mapper` im Code [Repo]. Die Mapper sind
  handgeschrieben und werden wie normaler Code umbenannt.

### 2. `{@link}` in Javadoc

291 `{@link}`/`{@linkplain}`, 78 Zeilen davon mit `#member` [Repo].

- `ChangeType` zog `{@link TerminZeile}` (`BuchungZeile.java:15`) und `{@link Termine}`
  (`Sprechtage.java:26`) nach [Lauf].
- `ChangeMethodName` zog `{@link Termin#buche}` nach (`BuchungsVorgangService.java:95`) [Lauf].
- **Nicht erfasst** werden Namen in `{@code ...}` und Fließtext, etwa `{@code termin_id}`. Das
  fällt ohnehin in die Übersetzung der Kommentare.
- Längere Namen können die Zeilenbreite überschreiten (`Sprechtage.java:26`) [Lauf].

### 3. Spring Data JDBC, SQL und Row-Mapper

Die Standardstrategie leitet Spalten aus Feldnamen in Snake Case ab
([Doku](https://docs.spring.io/spring-data/relational/reference/jdbc/mapping.html)).

| Element | Bestand [Repo] | Beim Umbenennen |
|---|---|---|
| `@Table` | an allen 8 Modellen | Klassen sind frei umbenennbar |
| `@MappedCollection(idColumn = ...)` | beide Collections (`TerminZeile.java:51`, `SprechtagZeile.java:65`) | Rückreferenz bleibt stabil |
| `@Column` | **0** bei 59 Spalten | **je umbenanntem Feld Pflicht** |
| Abgeleitete Query | `findByAccessToken` (`SprechtagZeilen.java:19`) | hängt am Property-Namen, `accessToken` nicht umbenennen oder Query mitziehen |
| `@Query` | 4 in `TerminZeilen.java` | SQL bleibt; `@Param`-String und `:param` nur gemeinsam ändern |
| Row-Mapper | 87 `rs.getX("spalte")` in 4 Adaptern | Strings sind Spaltennamen und **bleiben** |

**Probe** mit Springs `JdbcMappingContext`, ohne Datenbank, über alle 8 Modelle [Lauf]:

```text
Feld umbenannt, ohne @Column:   termin.startzeit → termin.start_time
Feld umbenannt, mit @Column:    keine Abweichung zur Ausgangslage
```

`lehrer` ist laut #244 nur **außerhalb** der Persistenz eine Abweichung vom Glossar. Das Feld
`lehrerId` in den Persistenzmodellen (`TerminZeile.java:43`, `LehrauftragZeile.java:30`) kann also
bleiben und braucht dann kein `@Column`. Wird es doch umbenannt, muss die Spalte `lehrer_id` per
`@Column` erhalten bleiben.

### 4. Vaadin

- **Routen hängen nicht am Klassennamen.** Ohne `@Route`-Wert leitet Vaadin den Pfad aus dem
  Klassennamen ab ([Doku](https://vaadin.com/docs/latest/flow/routing/route)), aber alle 8 Views
  setzen den Wert explizit [Repo].
- **Die Werte müssen bleiben**, die Konstanten dürfen wandern: `"auswertung"`, `"sprechtag"`,
  `"sprechtage"`, `"nachtragen"`, `"organizers"`, `"login"`, `""` und
  `Elternlink.PFAD = "elternsprechtag"` (`Elternlink.java:22`).
- **`@CssImport`:** 28 Pfade auf 27 Dateien [Repo]. Das sind Strings, OpenRewrite ändert sie nicht.
  Sie gehören in die CSS-Spur.
- **Dev-Bundle:** `src/main/bundles/dev.bundle` ist eingecheckt und wird laut
  `src/main/bundles/README.md` bei über `@CssImport` hinzugefügten Dateien neu erzeugt. Ob ein
  reines Umbenennen das auslöst, ist nicht geprüft; das Bundle kann im PR-Diff auftauchen.
- `@JsModule` wird nicht verwendet [Repo].

### 5. i18n-Keys

341 Keys, keine Duplikate [Repo]. **332 stehen als vollständiges Literal im Java-Code**, sind also
per exaktem Token ersetzbar.

Die übrigen **9 entstehen dynamisch** aus drei Präfixen in `ElternsprechtagPresenter.java:61-63`
und `:72`: `elternsprechtag.{beendet,vorbei,cancelled}.{title,hinweis,kontakt}`. Diese Stelle muss
von Hand oder mit eigenen Tabellenzeilen umgestellt werden.

Eine Vollständigkeitsprüfung ist einfach: Jeder Key der Datei muss nach dem Lauf im Code vorkommen,
die 9 dynamischen als bekannte Ausnahme.

### 6. CSS-Klassen und Screenshot-Module

253 Klassen in 27 Dateien [Repo], gezählt per Regex-Inventar:

- **251 stehen als Literal in Java.** Die 2 übrigen setzt `SlotLegende.java:21` dynamisch
  zusammen: `"elternsprechtag-view__legend-swatch--" + modifier` (`frei`, `belegt`).
- `addClassName(modifier)` in `AuswertungView.java:735` und `StatusBadge.java:12` nimmt Variablen
  entgegen; deren Werte sind Literale.
- **Screenshot-Module hängen an den Klassen:** 25 von 31 Skripten nutzen 53 verschiedene
  App-Selektoren an 131 Stellen, etwa `.sprechtag-table__row` oder
  `vaadin-dialog.cancel-sprechtag-dialog`. Eine CSS-Umbenennung ohne diese Dateien bricht den
  Screenshot-Lauf.
- CLAUDE.md nennt `styles/auswertung-view.css` als Vorlage. Der Verweis muss mitwandern.

### 7. Weitere namensabhängige Stellen

| Stelle | Befund [Repo] | Risiko |
|---|---|---|
| `ElternsprechtagProperties` | Binding über Feldnamen; `schoolname`, `stellvertreteradresse`, `impressumUrl`, `datenschutzerklaerungUrl` | **hoch, still**: Wert wäre `null` |
| Enum-Konstanten | gespeichert (`ZustellungenJdbcAdapter.java:44-45`), gelesen per `valueOf` (`BuchungsAnsichtenJdbcAdapter.java:230`), in `CHECK` (`V4:56,88`, `V7:14`, `V12:19-20`), in SQL (`SprechtagAnsichtenJdbcAdapter.java:50`) | **hoch** |
| Flyway-Dateien | Kommentar mit Klassenname in `V12:11`; Demo-Seed `R__demo_stammdaten.sql` enthält Enum-Werte | **hoch** bei Textersetzung |
| ArchUnit | Package-Strings (`ArchitekturTest.java:37` ff.), `haveSimpleNameEndingWith("Presenter")` (`:258`) | mittel: fällt laut im Build auf |
| Tests auf Texte | `SprechtagTest.java:101,494` prüfen deutschen Exception-Text; `ElternlinkTest.java:45-46` prüft Property-Namen, die **bleiben** | mittel |
| Constraint-Name im Test | `SprechtagePersistenceAdapterTest.java:176` | bleibt, Schema ist fest |
| Reflection | kein `Class.forName`, `getDeclared*`, `getSimpleName` | keins |
| Bean-Namen | kein `@Qualifier`, `getBean`, `@Resource`, `@DependsOn` | keins |
| Vorlagen | kein Thymeleaf; Mails entstehen in Java aus i18n-Texten | keins |
| Serialisierung | kein Jackson, kein eigenes `Serializable` im Code | keins gefunden |

`@ConfigurationProperties` bindet über Java-Bean-Properties
([Doku](https://docs.spring.io/spring-boot/reference/features/external-config.html)). Die
Env-Variable ist für Betreiber dokumentiert (`site/src/content/docs/betrieb/konfiguration.md:202`).
Sicherster Weg: Die Felder dieser Klasse bleiben. `schoolname` ist laut #244 ein englisches
Synonym und damit ein Kandidat für die Tabelle, darf dort aber nicht landen.

### 8. Teilweise Anwendung und Wiederholbarkeit

- **In Teilen anwendbar:** Jede Gruppe wird ein eigenes deklaratives Rezept, ausgewählt über
  `-Drewrite.activeRecipes=...`, ohne Änderung an der `pom.xml` [Lauf].
- **Idempotent:** Der zweite Lauf ließ die Prüfsumme über alle `.java`-Dateien unverändert [Lauf].
- **Reihenfolge im Rezept:** In der Probe stand `ChangeMethodName` vor dem `ChangeType` desselben
  Typs, weil das Muster den alten Namen nennt. Die umgekehrte Reihenfolge wurde nicht getestet.
- **Parallele Branches: nicht geprüft.** Plausibler Ablauf: Rezept auf dem Feature-Branch laufen
  lassen, dann mergen. Ketten wie A → B, B → C müssen in der Tabelle ausgeschlossen sein.

## Was tatsächlich ausgeführt wurde

Alle Läufe im Worktree, nichts davon committet.

| Lauf | Ergebnis |
|---|---|
| Ausgangs-Build `test-compile` mit JDK 25 | erfolgreich |
| `probe.Safe`: 2× `ChangeType`, 2× `ChangeMethodName`, 1× `ChangePackage` | 39 Dateien im `git diff` (einschließlich alter Pfade verschobener Dateien); Kompilat erfolgreich |
| Zweiter Lauf von `probe.Safe` | keine Änderung (gleiche SHA-1 über alle `.java`) |
| `ArchitekturTest`, `TerminTest` nach dem Lauf | 46 Tests grün |
| `ChangeMethodName` auf Lombok-Getter | nur Aufrufer geändert, Kompilat **fehlgeschlagen** |
| `ChangeMethodName` auf Record-Accessor | nur Aufrufer (`BuchenService.java:35`), Kompilat **fehlgeschlagen** |
| Mapping-Vergleich mit `JdbcMappingContext`, Feld von Hand umbenannt | ohne `@Column` abweichend, mit `@Column` identisch |
| Inventar für i18n, CSS und Selektoren (Node-Skript) | Zahlen in Punkt 5 und 6 |

**Nicht ausgeführt oder nicht belegt:**

- **Eigenes Rezept für Felder und Record-Komponenten.** Ein Entwurf wurde gebaut, der erste Lauf
  scheiterte an der Optionsbindung, der Lauf der Korrektur wurde nicht freigegeben. Ob der Ansatz
  trägt, ist **offen**.
- IntelliJ `rename_refactoring`: nur Doku, nie gestartet.
- Volle Testsuite (`./mvnw verify`) und alle Tests gegen Postgres: nicht gelaufen, um die lokale
  Entwicklungsdatenbank nicht anzufassen.
- Die Rezeptquellen wurden über eine zusammenfassende Abfrage des `main`-Stands gelesen, nicht
  zeilenweise. Eine Aussage daraus (keine Javadoc-Behandlung in `ChangeMethodName`) widerlegte der
  Lauf; bei Abweichungen gilt der Lauf.

## Risiken

| Risiko | Wirkung | Gegenmaßnahme |
|---|---|---|
| Feld ohne `@Column` umbenannt | Laufzeitfehler auf Produktivschema, Kompilat bleibt grün | `@Column` vorab, Mapping-Vergleich als Prüfung |
| Property-Feld umbenannt | Konfiguration wird still ignoriert | Sperrliste |
| Enum-Konstante umbenannt | gespeicherte Werte nicht mehr lesbar, `CHECK` verletzt | Sperrliste |
| Migration verändert | Start scheitert an der Flyway-Prüfsumme | `src/main/resources/db/**` vom Skript ausschließen |
| Dynamische Keys oder Klassen übersehen | fehlender Text oder fehlender Stil, kein Build-Fehler | Vollständigkeitsprüfung, 11 bekannte Stellen von Hand |
| Screenshot-Selektoren vergessen | Doku-Screenshots brechen | `site/screenshots` in die CSS-Spur |
| Feldspur trägt nicht | Hauptteil der Tabelle bleibt Handarbeit | Spike vor dem ersten PR, Ersatzweg IntelliJ |

## Offene Punkte für #250 (PR-Schnitt und Reihenfolge)

1. **Spike zuerst:** Eigenes Rezept für Record-Komponenten und Lombok-Felder zum Laufen bringen
   oder IntelliJ `rename_refactoring` an denselben zwei Beispielen prüfen. Davon hängt der Schnitt
   der Bezeichner-PRs ab.
2. **Vorbereitender PR ohne Umbenennung:** `@Column` an alle Felder der 8 Modelle, die umbenannt
   werden, plus eine Mapping-Prüfung als Test. Soll diese Prüfung dauerhaft bleiben?
3. **Sperrliste festlegen:** `ElternsprechtagProperties`, Enum-Konstanten, Routenwerte,
   `src/main/resources/db/**`. Bleiben die Enum-Konstanten auch dort unverändert, wo sie nicht
   Glossar sind (`EIN_TAG`, `ZWEI_TAGE`, `DREI_TAGE`, `KEINE`)? Das Schema ist gesetzt, daher
   wäre die Alternative ein Konverter.
4. **Schnittvorschlag zur Diskussion:** Typen und Packages, dann Methoden, dann Felder und Records,
   dann i18n, dann CSS mit Screenshots, zuletzt Kommentare und Texte.
5. **Parallele Branches:** Ablauf am Beispiel eines echten Feature-Branches prüfen.
6. **Wo die Tabelle und die Rezepte liegen:** eingecheckt im Repo oder nur im Umsetzungs-PR?
7. **`lehrer`-Bezeichner:** Bleibt `lehrerId` in den Persistenzmodellen, während Ports, Services
   und Views auf `lehrkraftId` wechseln?

## Anhang: Aufruf der Probe

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: probe.Safe
displayName: Probe safe renames
description: Types, overriding method, domain method and package.
recipeList:
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence.TerminZeile
      newFullyQualifiedTypeName: de.openclassware.elternsprechtag.sprechtag.adapter.out.persistence.TerminRow
  - org.openrewrite.java.ChangeMethodName:
      methodPattern: de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine speichere(..)
      newMethodName: save
      matchOverrides: true
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: de.openclassware.elternsprechtag.sprechtag.application.port.out.Termine
      newFullyQualifiedTypeName: de.openclassware.elternsprechtag.sprechtag.application.port.out.TerminRepository
  - org.openrewrite.java.ChangeMethodName:
      methodPattern: de.openclassware.elternsprechtag.sprechtag.domain.Termin buche(..)
      newMethodName: buchen
  - org.openrewrite.java.ChangePackage:
      oldPackageName: de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layouts
      newPackageName: de.openclassware.elternsprechtag.sprechtag.adapter.in.web.layout
      recursive: true
```

```bash
./mvnw -B org.openrewrite.maven:rewrite-maven-plugin:6.46.1:run \
  -Drewrite.configLocation=/pfad/zu/rewrite.yml \
  -Drewrite.activeRecipes=probe.Safe \
  -Drewrite.exportDatatables=false
```
