# Anwender-Doku

Quelle der Doku-Site unter <https://docs.openclassware.de>, gebaut mit
[Astro Starlight](https://starlight.astro.build/). Eigenes `package.json` wie `tools/screenshots`:
das Root-`package.json` wird von Vaadin generiert und ist gitignored.

Diese Datei ist die Schreibanleitung. **Wann** ein PR die Doku mitzieht, steht in
[`CLAUDE.md`](../CLAUDE.md#anwender-doku): Was ein Anwender sieht oder erlebt, ändert die Seite im
selben PR.

## Aufbau

- Seiten liegen unter `src/content/docs/`, je Bereich ein Verzeichnis: `einstieg`,
  `anwendungsfaelle`, `betrieb`, `datenschutz`, `referenz`. Eine neue Seite erscheint von selbst in
  der Navigation ihres Bereichs.
- Jede Seite trägt unter dem Titel den Hinweis, dass sie den aktuellen Stand beschreibt
  (`src/components/PageTitle.astro`).
- **Fallseiten** liegen als `anwendungsfaelle/<slug>.mdx`, eine Datei je Fall. Der Katalog auf
  der Seite *Anwendungsfälle* nimmt sie von selbst auf (`src/components/Katalog.astro`): je Phase
  ein Block mit Anker (`/anwendungsfaelle/#<phase>`), darin eine Karte je Fall. Phasen ohne
  Fallseite entfallen.
- Die Vorlage der Fallseiten steht als Code in `src/vorlage/` und wird beim Build geprüft:
  - `schema.mjs` prüft das [Frontmatter](#frontmatter). Daraus entstehen Katalogkarte und
    Steckbrief unter dem Titel (`src/components/Steckbrief.astro`).
  - `pruefung.mjs` prüft die [Abschnitte](#abschnitte) und auf der ganzen Site die Alt-Texte.

  Wer die Vorlage ändert, ändert Code, Beispiele unter `test/beispiele/` und diese Datei zusammen.
- Vorbild ist `anwendungsfaelle/einen-sprechtag-absagen.mdx`, samt Screenshot-Modul.

## Eine Fallseite anlegen

1. **Ein Ziel, eine Seite.** Prüfen Sie zuerst, ob der Fall eine eigene Seite ist. Für die Fälle
   aus [`docs/arc/ABDECKUNG.md`](../docs/arc/ABDECKUNG.md) gilt:
   - Ein eigenes Ziel wird eine eigene Seite.
   - Eine Variante wird ein `### …`-Abschnitt unter *Wenn …* der Seite, zu der sie gehört.
   - Eine Schutzregel ohne eigene Handlung steht unter *Regeln* bei der Handlung, an der man an
     die Grenze stößt.
   - „Darf fehlen“ erscheint nur als *Weg drumherum*, ohne „fehlt noch“ und ohne Roadmap.
   - „Bewusst nein“ steht gesammelt auf *Was die Anwendung kann – und was nicht* und, wo ein Leser
     danach sucht, als ein Satz unter *Regeln*.

   Automatische Abläufe (Erinnerung, Abschluss, Anonymisierung) bekommen keine eigene Seite.
2. **Datei anlegen:** `src/content/docs/anwendungsfaelle/<slug>.mdx`. Der Slug ist der Titel in
   Kleinbuchstaben, mit Bindestrichen, Umlaute ausgeschrieben: *Eine Buchung stornieren* →
   `eine-buchung-stornieren.mdx`. Weitere Dateien braucht es nicht, der Katalog nimmt die Seite
   von selbst auf.
3. **Gerüst kopieren** und füllen. Optionale Abschnitte, die nichts zu sagen haben, löschen Sie
   ganz — leere Abschnitte lässt der Build nicht durch.

   ````mdx
   ---
   title: Eine Buchung stornieren
   description: Eine einzelne Buchung stornieren; der Termin wird wieder frei.
   phase: buchungsphase
   order: 4
   wer: organisator
   wann: Buchungsphase — eine Familie kann nicht kommen
   voraussetzung: Der Sprechtag ist veröffentlicht (Status Aktiv)
   dauer: unter einer Minute
   ---

   import { Steps } from '@astrojs/starlight/components';

   Ein, zwei Sätze: Ziel und wichtigste Folge.

   ## Ist das der richtige Fall?

   | Ihre Lage | Ihr Weg |
   |---|---|
   | … | **…** — diese Seite |
   | … | *Andere Fallseite* |

   ## Schritte

   <Steps>

   1. Öffnen Sie **Elternsprechtage verwalten**.

   2. …

   </Steps>

   ## Was danach passiert

   - **Die Familie** bekommt …
   - **Der Zugangs-Link** zeigt …
   - Die Anwendung **benachrichtigt nicht** …

   ## Regeln

   - **Regel.** Begründung.

   ## Wenn …

   ### … die Bedingung eintritt

   Was dann zu tun ist.

   ## Wege drumherum

   - **Was fehlt.** Der Weg, oder ein Verweis auf dessen Fallseite.

   ## Verwandte Fälle

   - *Andere Fallseite* — wann statt dieser Seite
   ````

4. **Screenshots**, falls die Seite Bilder braucht: ein Modul `screenshots/faelle/<slug>.mjs`,
   siehe [Screenshots](#screenshots). Eine Seite ohne Bilder braucht kein Modul.
5. **Bauen:** `npm run build`, siehe [Lokal bauen](#lokal-bauen).
6. **`ABDECKUNG.md` gegenlesen:** Steht ein Fall dort auf **erfüllt**, muss die Seite zu dem
   passen, was dort steht.

## Vorlage

### Frontmatter

Geprüft durch `src/vorlage/schema.mjs`. Ein fehlendes oder falsches Feld lässt den Build scheitern.

| Feld | | Inhalt |
|---|---|---|
| `title` | Pflicht | Das Ziel als Verb im Infinitiv: *Einen Sprechtag absagen* |
| `description` | Pflicht | Ein Satz: Ziel und wichtigste Folge. Er ist zugleich der Text der Katalogkarte, die einzige Quelle |
| `phase` | Pflicht | Eine der fünf Phasen, siehe unten |
| `order` | Pflicht | Ganze Zahl, Reihenfolge innerhalb der Phase |
| `wer` | Pflicht | `organisator` oder `lehrkraft`, genau ein Akteur. Bei `lehrkraft` setzt der Katalog „(Lehrkraft)“ hinter den Titel |
| `wann` | Pflicht | Phase und Anlass |
| `voraussetzung` | Pflicht | Was gegeben sein muss, meist ein Status |
| `dauer` | optional | Etwa „unter einer Minute“ |

Wer, Wann, Voraussetzung und Dauer setzt der Steckbrief als Tabelle unter den Titel — von Hand
schreiben Sie ihn nicht.

| `phase` | Katalogblock | Phase in `ABDECKUNG.md` |
|---|---|---|
| `vorbereiten` | Vorbereiten & veröffentlichen | 2 |
| `buchungsphase` | Buchungsphase | 3 |
| `kurz-vor-dem-termin` | Kurz vor dem Termin | 4 |
| `am-tag` | Am Tag | 5 |
| `danach` | Danach | 6 |

Die `phase`-Werte sind zugleich die Anker der Katalogblöcke, auf die `ABDECKUNG.md` je Phase
verlinkt. Wer einen umbenennt, zieht dort mit — die Links sind absolut, der Build sieht sie nicht.

### Abschnitte

Nur diese H2, **wörtlich und in dieser Reihenfolge**. Geprüft durch `src/vorlage/pruefung.mjs`.
Optionale Abschnitte entfallen ganz, leere gibt es nicht.

| Abschnitt | | Inhalt |
|---|---|---|
| `## Ist das der richtige Fall?` | optional | Weiche als Tabelle *Ihre Lage → Ihr Weg*, nur bei Verwechslungsgefahr |
| `## Schritte` | Pflicht | Eine Starlight-`<Steps>`-Liste. Ein Bild nur, wo die Oberfläche mehr zeigt als der Text |
| `## Was danach passiert` | Pflicht | Je Beteiligtem: welche E-Mail, was der Zugangs-Link zeigt. Dazu ausdrücklich, was die Anwendung **nicht** tut |
| `## Regeln` | optional | Schutzregeln mit Begründung. „Bewusst nein“ steht hier als ein Satz |
| `## Wenn …` | optional | Je Variante oder Problem ein `### … <Bedingung>`, mindestens eines, nur H3 |
| `## Wege drumherum` | optional | Lücken der Anwendung als Weg. Ist der Weg selbst eine Aufgabe, ein Verweis auf deren Fallseite |
| `## Verwandte Fälle` | Pflicht | Verweise, jeweils mit einem Halbsatz, *wann statt dieser Seite* |

Das „…“ ist das Auslassungszeichen (U+2026), nicht drei Punkte.

Nicht geprüft wird, ob der Inhalt stimmt, ob die Oberflächenbegriffe zu `translations.properties`
passen, ob die Weiche nötig ist und ob vor der ersten H2 der kurze Einleitungssatz steht, den jede
Fallseite hat. Das bleibt beim Review.

## Leitregeln

- **Ohne Vorwissen lesbar.** Jede Seite muss für sich verständlich sein, für jemanden, der direkt
  über die Suche oder einen Link einsteigt. Keine Verweise wie „wie oben beschrieben“ über die
  Seite hinaus, keine Begriffe aus Code oder Architektur.
- **Oberflächenbegriffe fett und wörtlich** aus
  [`translations.properties`](../src/main/resources/vaadin-i18n/translations.properties):
  **Elternsprechtage verwalten**, **Absagen**. Was dort steht, steht auch hier — Buchstabe für
  Buchstabe.
- **Status kursiv:** *Entwurf*, *Aktiv*, *Abgesagt*.
- **Fachsprache aus den Glossaren** (Sprechtag, Termin, Buchung, Lehrauftrag …), eingedeutscht
  sind nur Anglizismen und Technikwörter: **Organisator** statt Organizer, **Zugangs-Link** statt
  Access-Token.
- **Leser direkt ansprechen**, mit „Sie“ und im Präsens: „Öffnen Sie …“, „Der Status wechselt …“.
- **Beschreibender Alt-Text** für jedes Bild: was zu sehen ist und worauf es ankommt, etwa
  `Dialog „Sprechtag absagen“: Er nennt die Zahl der Kinder, deren Eltern eine E-Mail bekommen …`.
  Ein leerer Alt-Text oder „Screenshot“ bzw. „Bild“ allein lässt den Build scheitern.
- **Verweise auf andere Fallseiten** als Link, sobald die Seite existiert; bis dahin der Titel
  *kursiv*. Ein toter interner Link lässt den Build scheitern.

## Lokal bauen

Voraussetzung ist Node ≥ 22.12. Die Fallseiten binden Screenshots ein, die nicht im Repo
liegen — vor dem ersten Build also einmal [Screenshots erzeugen](#screenshots).

```sh
cd site
npm ci
npm run screenshots   # braucht die laufende App, siehe unten
npm run dev           # Vorschau unter http://localhost:4321
npm run build         # wie in CI, Ergebnis in dist/
npm test              # Prüfregeln der Vorlage und der Screenshot-Auswahl
```

Der Build bricht ab bei einem toten internen Link, bei einer Fallseite, die die Vorlage verletzt,
bei einem Bild ohne Alt-Text — „Screenshot“ oder „Bild“ allein zählen als leer — und bei einem
fehlenden Screenshot. Die Meldung nennt Datei, Zeile und verletzte Regel.

## Screenshots

Die Bilder der Fallseiten entstehen per Playwright aus einer laufenden `demo`-Instanz und liegen
unter `src/assets/screenshots/<slug>/<name>.png` (gitignored). CI erzeugt sie bei jedem Lauf neu.
**Ändert sich eine Seite, zieht ihr Screenshot-Modul mit.**

1. App mit Profil `demo` starten. Der Demo-Seed ist der einzige Datenstand; fehlt einem Fall ein
   Zustand, wird `db/demo/R__demo_stammdaten.sql` erweitert. Damit die Entwicklungsdatenbank
   unberührt bleibt, am besten gegen eine eigene Datenbank, einmalig angelegt:

   ```sh
   docker compose exec database psql -U myuser -d elternsprechtag -c 'CREATE DATABASE elternsprechtag_doku'

   SPRING_DOCKER_COMPOSE_ENABLED=false \
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/elternsprechtag_doku \
   ELTERNSPRECHTAG_OEFFENTLICHE_URL=http://localhost:8080 \
   ELTERNSPRECHTAG_STELLVERTRETERADRESSE=sekretariat@example.org \
     ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
   ```

2. Screenshots erzeugen — alle oder einzelne:

   ```sh
   ORGANIZER_PASSWORD=... npm run screenshots
   ORGANIZER_PASSWORD=... npm run screenshots -- einen-sprechtag-absagen
   ```

   `ORGANIZER_USERNAME` (Default `user`) und `BASE_URL` (Default `http://localhost:8080`) gehen
   ebenfalls. Gebraucht wird ein lokal installiertes Chrome.

**Ein Modul je Fallseite**, `screenshots/faelle/<slug>.mjs`, und nur, wenn die Seite Bilder hat.
Ein Modul ohne passende `anwendungsfaelle/<slug>.mdx` lässt den Runner abbrechen. Das Modul
exportiert eine Funktion, die `{ page, basis, foto, ablegen, rahmen, bereit }` bekommt, schon
angemeldet, in einem eigenen Browser-Kontext:

- **Ausschnitte statt ganzer Seiten:** `foto(locator, name)` nimmt genau ein Element auf, etwa
  einen Dialog, einen Formularblock oder eine Tabellenzeile. Greift der Selektor nicht, scheitert
  der Lauf — in CI wird der PR damit rot.
- **Hervorheben** nur mit `rahmen(locator)`, einem schlichten Rahmen. Pfeile und Nummern gibt es nicht.
- **Zerstörerisches** nur bis zum offenen Dialog fotografieren, dann abbrechen. Endzustände kommen
  aus Seed-Sprechtagen, die schon so sind. So bleibt der Seed unverändert und die Reihenfolge
  der Module beliebig.
- Fenster 1280 × 800 bei `deviceScaleFactor: 2` (`screenshots/werkzeug.mjs`).

- **Erzeugte Dateien**, die keine Ausschnitte sind, legt `ablegen(name, inhalt)` neben die Bilder,
  etwa das Beispiel-PDF des Tagesplans. Das Blatt selbst zeigt die Seite als Bild: Das Modul lädt
  das ZIP aus der Auswertung und zeichnet die erste PDF-Seite mit pdf.js im Browser
  (`screenshots/tagesplan.mjs`).

Die Fallseite bindet das Bild mit Markdown-Syntax und beschreibendem Alt-Text ein:
`![Dialog „…“: …](../../../assets/screenshots/<slug>/<name>.png)`. Fehlt die Datei, bricht der
Build ab. Eine abgelegte Datei zum Herunterladen kommt per Import mit `?url` auf die Seite:

```mdx
import beispielBlatt from '../../../assets/screenshots/<slug>/<datei>.pdf?url';

<a href={beispielBlatt} download="<datei>.pdf">… herunterladen (PDF)</a>
```

`tools/screenshots/` bleibt das Werkzeug für die Responsive-Arbeit an der App.

## Versionen

Starlight steht vor 1.0. Die Versionen in `package.json` sind deshalb exakt gepinnt; Upgrades
werden gesammelt gehoben, nicht einzeln — monatlich als ein Dependabot-Pull-Request.
