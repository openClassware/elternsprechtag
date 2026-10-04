# Anwender-Doku

Quelle der Doku-Site unter <https://docs.openclassware.de>, gebaut mit
[Astro Starlight](https://starlight.astro.build/). Eigenes `package.json` wie `tools/screenshots`:
das Root-`package.json` wird von Vaadin generiert und ist gitignored.

## Lokal bauen

Voraussetzung ist Node ≥ 22.12.

```sh
cd site
npm ci
npm run dev     # Vorschau unter http://localhost:4321
npm run build   # wie in CI, Ergebnis in dist/
npm test        # Prüfregeln der Vorlage gegen die Beispiele in test/beispiele/
```

Der Build bricht ab bei einem toten internen Link, bei einer Fallseite, die die Vorlage verletzt,
und bei einem Bild ohne Alt-Text — „Screenshot“ oder „Bild“ allein zählen als leer. Die Meldung
nennt Datei, Zeile und verletzte Regel.

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
  - Vorbild ist `anwendungsfaelle/einen-sprechtag-absagen.mdx`.

## Versionen

Starlight steht vor 1.0. Die Versionen in `package.json` sind deshalb exakt gepinnt; Upgrades
werden gesammelt gehoben, nicht einzeln — monatlich als ein Dependabot-Pull-Request.
