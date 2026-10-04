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
```

Der Build bricht bei einem toten internen Link ab.

## Aufbau

- Seiten liegen unter `src/content/docs/`, je Bereich ein Verzeichnis: `einstieg`,
  `anwendungsfaelle`, `betrieb`, `datenschutz`, `referenz`. Eine neue Seite erscheint von selbst in
  der Navigation ihres Bereichs.
- Jede Seite trägt unter dem Titel den Hinweis, dass sie den aktuellen Stand beschreibt
  (`src/components/PageTitle.astro`).

## Versionen

Starlight steht vor 1.0. Die Versionen in `package.json` sind deshalb exakt gepinnt; Upgrades
werden gesammelt gehoben, nicht einzeln — monatlich als ein Dependabot-Pull-Request.
