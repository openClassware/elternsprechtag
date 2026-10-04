# Anwender-Doku

Quelle der Doku-Site unter <https://docs.openclassware.de>, gebaut mit
[Astro Starlight](https://starlight.astro.build/). Eigenes `package.json` wie `tools/screenshots`:
das Root-`package.json` wird von Vaadin generiert und ist gitignored.

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

1. App mit Profil `demo` starten. Der Demo-Seed ist der einzige Datenstand; fehlt einem Fall ein
   Zustand, wird `db/demo/R__demo_stammdaten.sql` erweitert. Damit die Entwicklungsdatenbank
   unberührt bleibt, am besten gegen eine eigene Datenbank, einmalig angelegt:

   ```sh
   docker compose exec database psql -U myuser -d elternsprechtag -c 'CREATE DATABASE elternsprechtag_doku'

   SPRING_DOCKER_COMPOSE_ENABLED=false    SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/elternsprechtag_doku    ELTERNSPRECHTAG_OEFFENTLICHE_URL=http://localhost:8080      ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
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
exportiert eine Funktion, die `{ page, basis, foto, rahmen, bereit }` bekommt, schon angemeldet,
in einem eigenen Browser-Kontext:

- **Ausschnitte statt ganzer Seiten:** `foto(locator, name)` nimmt genau ein Element auf, etwa
  einen Dialog, einen Formularblock oder eine Tabellenzeile. Greift der Selektor nicht, scheitert
  der Lauf — in CI wird der PR damit rot.
- **Hervorheben** nur mit `rahmen(locator)`, einem schlichten Rahmen. Pfeile und Nummern gibt es nicht.
- **Zerstörerisches** nur bis zum offenen Dialog fotografieren, dann abbrechen. Endzustände kommen
  aus Seed-Sprechtagen, die schon so sind. So bleibt der Seed unverändert und die Reihenfolge
  der Module beliebig.
- Fenster 1280 × 800 bei `deviceScaleFactor: 2` (`screenshots/werkzeug.mjs`).

Die Fallseite bindet das Bild mit Markdown-Syntax und beschreibendem Alt-Text ein:
`![Dialog „…“: …](../../../assets/screenshots/<slug>/<name>.png)`. Fehlt die Datei, bricht der
Build ab.

`tools/screenshots/` bleibt das Werkzeug für die Responsive-Arbeit an der App.

## Aufbau

- Seiten liegen unter `src/content/docs/`, je Bereich ein Verzeichnis: `einstieg`,
  `anwendungsfaelle`, `betrieb`, `datenschutz`, `referenz`. Eine neue Seite erscheint von selbst in
  der Navigation ihres Bereichs.
- Jede Seite trägt unter dem Titel den Hinweis, dass sie den aktuellen Stand beschreibt
  (`src/components/PageTitle.astro`).
- **Fallseiten** liegen als `anwendungsfaelle/<slug>.mdx`, eine Datei je Fall. Der Katalog auf
  der Seite *Anwendungsfälle* nimmt sie von selbst auf (`src/components/Katalog.astro`).
  Die Vorlage steht als Code in `src/vorlage/`:
  - `schema.mjs` prüft das Frontmatter: `title`, `description` (zugleich Text der Katalogkarte),
    `phase`, `order`, `wer`, `wann`, `voraussetzung`, optional `dauer`. Daraus entsteht der
    Steckbrief unter dem Titel.
  - `pruefung.mjs` prüft die Abschnitte: nur die erlaubten H2, wörtlich und in fester Reihenfolge,
    kein leerer Abschnitt, `<Steps>` unter *Schritte*, unter *Wenn …* nur `### …`-Überschriften.
  - Vorbild ist `anwendungsfaelle/einen-sprechtag-absagen.mdx`, samt Screenshot-Modul.

## Versionen

Starlight steht vor 1.0. Die Versionen in `package.json` sind deshalb exakt gepinnt; Upgrades
werden gesammelt gehoben, nicht einzeln — monatlich als ein Dependabot-Pull-Request.
