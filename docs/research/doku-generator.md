# Recherche: Generator für die Doku-Site

Frage aus [#192](https://github.com/openClassware/elternsprechtag/issues/192) (Karte
[#190](https://github.com/openClassware/elternsprechtag/issues/190)): Welcher statische Site-Generator
baut die deutschsprachige Anwender-Doku für Organizer/Sekretariat, Lehrkräfte, Schulleitung und
Schul-IT?

Stand der Recherche: **2026-10-04**. Versionen und Daten stammen aus den GitHub-, npm- und
PyPI-APIs bzw. den offiziellen Dokus der Projekte; Quellen stehen direkt an den Aussagen und
gesammelt am Ende.

## Kurzfassung

- **Empfehlung: Astro Starlight.** Als einziger Kandidat bringt Starlight eine Suche ohne externen
  Dienst mit deutschem Stemming **und** deutscher Such-Oberfläche mit (Pagefind), dazu fertige
  Karten (`CardGrid`, `LinkCard`), Schritt-Anleitungen (`Steps`) und Hinweisboxen. Es läuft auf Node,
  das das Repo ohnehin hat.
- **Material for MkDocs scheidet aus**: Es ist seit November 2025 im Wartungsmodus, das Ende der
  Wartung steht auf dem **5. Mai 2027**. Der Nachfolger **Zensical** ist vielversprechend, aber noch
  `0.0.x`/Alpha, die Such-Oberfläche ist nur englisch, und er bringt Python als dritte Toolchain.
- **Antora scheidet aus**, weil die Quelle AsciiDoc ist, nicht Markdown.
- **VitePress** und **Docusaurus** wären machbar, verlieren aber bei der deutschen Suche (VitePress:
  kein Stemming; Docusaurus: offiziell nur Algolia, lokal nur per Community-Plugin) und beim
  Karten-Layout (VitePress: keins für Inhaltsseiten).

## Kandidaten im Überblick (Stand 2026-10-04)

| | Letzte Version | Lizenz | Laufzeit | Status |
|---|---|---|---|---|
| Material for MkDocs | 9.7.7 (2026-07-17) | MIT | Python | Wartungsmodus, Ende 2027-05-05 |
| Zensical | 0.0.67 (2026-09-30) | MIT | Python (Rust-Kern, Wheels) | Alpha, 0.1.0 für 2026-11-05 angekündigt |
| Docusaurus | 3.10.2 (2026-07-10) | MIT | Node ≥ 20, React | aktiv, stabil |
| VitePress | 1.6.4 (2025-08-05); 2.0.0-alpha.20 (2026-09-04) | MIT | Node, Vue/Vite | 1.x ruht, 2.0 seit Jan. 2025 Alpha |
| Astro Starlight | 0.42.5 (2026-10-01) | MIT | Node ≥ 22.12, Astro 7 | aktiv, noch vor 1.0 |
| Antora | 3.2.1 (2026-10-01) | MPL-2.0 | Node | aktiv, stabil |

Quellen: GitHub-Releases der Repos
[squidfunk/mkdocs-material](https://github.com/squidfunk/mkdocs-material/releases),
[zensical/zensical](https://github.com/zensical/zensical/releases),
[facebook/docusaurus](https://github.com/facebook/docusaurus/releases),
[vuejs/vitepress](https://github.com/vuejs/vitepress/releases),
[withastro/starlight](https://github.com/withastro/starlight/releases); npm-Registry
(`vitepress`, `@docusaurus/core`, `@astrojs/starlight`, `@antora/cli`, `astro`); PyPI
(`mkdocs-material`, `zensical`).

### Zur Zukunft von MkDocs / Material

- MkDocs selbst hat seit **1.6.1 (2024-08-30)** kein Release mehr
  ([mkdocs/mkdocs Releases](https://github.com/mkdocs/mkdocs/releases)). Das Material-Team nennt MkDocs
  ausdrücklich ein Lieferketten-Risiko: „MkDocs must be considered a supply chain risk, since it's
  unmaintained since August 2024.“
  ([Blog, 2025-11-05](https://squidfunk.github.io/mkdocs-material/blog/2025/11/05/zensical/))
- Material for MkDocs ist im Wartungsmodus: „supporting Material for MkDocs for at least the next 12
  months, fixing critical bugs and security vulnerabilities as needed“ (ebd.). Inzwischen verlängert:
  „Material for MkDocs will receive a six-month end-of-life extension, with critical maintenance
  continuing until May 5, 2027.“ ([zensical.org/upcoming-changes](https://zensical.org/upcoming-changes/))
- Zensical ist der Nachfolger desselben Teams, eine Neuentwicklung (kein Fork), liest `mkdocs.yml`
  direkt und hat eine „classic“-Variante im Material-Look
  ([Compatibility](https://zensical.org/docs/compatibility/mkdocs/)). Am **2026-11-05** soll mit
  0.1.0 „a dependable release line“ beginnen ([upcoming-changes](https://zensical.org/upcoming-changes/));
  PyPI führt das Paket als „Development Status :: 3 - Alpha“.

## Vergleich je Kriterium

Reihenfolge nach Gewicht aus dem Issue.

### 1. Markdown im Repo, lesbar auf GitHub

| | Bewertung |
|---|---|
| Material / Zensical | ✅ Python-Markdown. Hinweisboxen als `!!! note` und Karten als `<div class="grid cards" markdown>` — auf GitHub als Text bzw. Liste lesbar, aber nicht als Box gerendert. |
| Docusaurus | ⚠️ `.md` wird in v3 **standardmäßig als MDX geparst** („Docusaurus v3 uses the MDX format for all files (including `.md` files)“, [Markdown features](https://docusaurus.io/docs/markdown-features)). Hinweisboxen `:::note`. |
| VitePress | ✅ Bestwert: versteht **GitHub-Alerts** (`> [!NOTE]`) nativ, „rendered the same as the custom containers“ ([Markdown](https://vitepress.dev/guide/markdown)) — dieselbe Datei sieht auf GitHub und auf der Site gleich aus. |
| Starlight | ✅/⚠️ Normale Seiten sind `.md`; Hinweisboxen als `:::note` ([Authoring](https://starlight.astro.build/guides/authoring-content/)), GitHub-Alerts sind nicht dokumentiert. Komponenten (Karten, Steps, Tabs) gehen **nur in MDX oder Markdoc** ([Using components](https://starlight.astro.build/components/using-components/)) — auf GitHub erscheinen dort JSX-Tags. |
| Antora | ❌ Quelle ist **AsciiDoc** („authoring content in AsciiDoc, Antora's content markup language“, [Antora-Doku](https://docs.antora.org/antora/latest/)). Verfehlt das Hauptkriterium. |

### 2. Volltextsuche auf Deutsch ohne externen Dienst

| | Engine | Deutsches Stemming | Deutsche Such-UI | Bewertung |
|---|---|---|---|---|
| Material | lunr.js + lunr-languages, im Browser | ✅ `lang: de` | ✅ | ✅ ([Search-Plugin](https://squidfunk.github.io/mkdocs-material/plugins/search/)) |
| Zensical | eigene Neuentwicklung, im Browser | ❓ „multilingual“, Lunr-`pipeline` entfällt; Stemming nicht dokumentiert | ❌ „Search is currently only available in English … the search interface is not localized“ | ⚠️ ([Site search](https://zensical.org/docs/setup/search/)) |
| Docusaurus | offiziell **Algolia DocSearch** (externer Dienst); lokal nur Community-Plugins | ✅ über [`@easyops-cn/docusaurus-search-local`](https://github.com/easyops-cn/docusaurus-search-local) (alle lunr-languages, also `de`) | ✅ per Übersetzung `theme.SearchBar.*` | ⚠️ Plugin eines Dritten ([Search](https://docusaurus.io/docs/search)) |
| VitePress | MiniSearch, im Browser | ❌ „No stemming is performed, and no stop-word list is applied.“ ([MiniSearch](https://lucaong.github.io/minisearch/)); nur Eigenbau über `processTerm` | ✅ über `locales` | ⚠️ Fuzzy + Präfix statt Stemming ([Local search](https://vitepress.dev/reference/default-theme-search)) |
| Starlight | **Pagefind**, eingebaut, ohne Konfiguration | ✅ Deutsch hat Stemming | ✅ Deutsch hat UI-Übersetzung | ✅ ([Site search](https://starlight.astro.build/guides/site-search/), [Pagefind multilingual](https://pagefind.app/docs/multilingual/)) |
| Antora | [Antora-Lunr-Extension](https://gitlab.com/antora/antora-lunr-extension), im Browser | ✅ über lunr-languages | — | ⚠️ Extension noch `1.0.0-alpha.13` (npm) |

Umlaute sind bei allen Engines unkritisch (Unicode-Tokenisierung). Offen bleibt bei allen die
**Kompositum-Zerlegung** (siehe offene Punkte).

### 3. Screenshots, Hinweisboxen, Schritt-Anleitungen, Katalog-/Karten-Layout

| | Hinweisboxen | Schritte | Karten/Katalog ohne Eigenbau | Bilder |
|---|---|---|---|---|
| Material / Zensical | ✅ Admonitions | Listen | ✅ Grid-Cards ([Material](https://squidfunk.github.io/mkdocs-material/reference/grids/), [Zensical](https://zensical.org/docs/authoring/grids/)) | Markdown; Lightbox per Plugin |
| Docusaurus | ✅ `:::note` | Listen | ✅ automatische Kategorie-Indexseiten mit Karten ([Generated index](https://docusaurus.io/docs/sidebar/items#embedding-generated-index)) | MDX/Markdown |
| VitePress | ✅ Container + GitHub-Alerts | Listen | ❌ Karten nur als `features` der Startseite; für Inhaltsseiten Vue-Eigenbau ([Markdown](https://vitepress.dev/guide/markdown)) | Markdown |
| Starlight | ✅ Asides | ✅ eigene `Steps`-Komponente | ✅ `Card`, `CardGrid`, `LinkCard` ([Components](https://starlight.astro.build/components/using-components/)) | ✅ relative Pfade, von Astro optimiert ([Authoring](https://starlight.astro.build/guides/authoring-content/)) |
| Antora | ✅ AsciiDoc-Admonitions | ✅ AsciiDoc | ⚠️ UI-Bundle anpassen | AsciiDoc |

### 4. Pflegeaufwand und Toolchain

Das Repo hat Maven und Node — Node allerdings nur indirekt: Das Root-`package.json` erzeugt Vaadin
und es ist gitignored; für Werkzeuge gibt es den Präzedenzfall eines **eigenen `package.json`**
(`tools/screenshots`). Eine Doku-Site bekäme genauso ein eigenes Verzeichnis mit eigenem
`package.json` und Lockfile.

| | Toolchain | Veralten / Upgrade-Last |
|---|---|---|
| Material | ➕ Python (dritte Toolchain) | ❌ Wartungsende 2027-05-05 |
| Zensical | ➕ Python (dritte Toolchain) | ⚠️ Alpha, Schnittstellen „still changing too much“ (Search-Doku); stabile Linie erst ab 0.1.0 |
| Docusaurus | Node + React | ✅ stabil, Meta als Träger |
| VitePress | Node + Vue | ⚠️ stabiles 1.x seit Aug. 2025 ohne Release, 2.0 seit 20 Monaten Alpha |
| Starlight | Node + Astro | ⚠️ aktiv (Astro-Team), aber vor 1.0: Minor-Releases etwa alle 1–2 Monate (0.38 bis 0.42 zwischen März und Sept. 2026), die Breaking Changes enthalten können; Node ≥ 22.12 |
| Antora | Node | ✅ stabil (Spring Boot nutzt es) |

Alle bauen in GitHub Actions mit einem einzigen Build-Befehl.

### 5. Versionierung je Release möglich

| | Versionierung |
|---|---|
| Material | ✅ über `mike` (Deploy-Branches) |
| Zensical | ⚠️ übergangsweise über einen `mike`-Fork; „We will maintain the fork until Zensical provides native versioning support“ ([mike](https://zensical.org/docs/compatibility/mkdocs/mike/)) |
| Docusaurus | ✅ eingebaut, Bestwert ([Versioning](https://docusaurus.io/docs/versioning)) |
| VitePress | ❌ nichts eingebaut |
| Starlight | ⚠️ Community-Plugin [`starlight-versions`](https://github.com/HiDeoo/starlight-versions) (MIT): „opinionated plugin that is still in early development“ |
| Antora | ✅ eingebaut über Git-Branches/-Tags ([Antora-Doku](https://docs.antora.org/antora/latest/)) |

Das Kriterium lautet nur „möglich“; ob gebraucht, entscheidet das Hosting-Ticket.

### 6. Lizenz und Hosting als statische Dateien

Alle sechs erzeugen rein statisches HTML/JS/CSS und sind frei lizenziert: MIT (Material, Zensical,
Docusaurus, VitePress, Starlight, Pagefind), MPL-2.0 (Antora, Antora-Lunr-Extension), MPL-1.1
(lunr-languages). Kein Unterschied in der Entscheidung.

## Gesamtbild

| Kriterium (nach Gewicht) | Material | Zensical | Docusaurus | VitePress | Starlight | Antora |
|---|---|---|---|---|---|---|
| Markdown, auf GitHub lesbar | ✅ | ✅ | ⚠️ | ✅✅ | ✅/⚠️ | ❌ |
| Deutsche Suche, lokal | ✅ | ⚠️ | ⚠️ | ⚠️ | ✅✅ | ⚠️ |
| Screenshots, Boxen, Schritte, Karten | ✅ | ✅ | ✅ | ⚠️ | ✅✅ | ⚠️ |
| Pflege / Toolchain / Veralten | ❌ | ⚠️ | ✅ | ⚠️ | ⚠️ | ✅ |
| Versionierung möglich | ✅ | ⚠️ | ✅✅ | ❌ | ⚠️ | ✅✅ |
| Lizenz / statisch | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

## Empfehlung: Astro Starlight

1. **Suche** — das schwerste Kriterium nach der Quelle: Pagefind ist eingebaut, läuft ohne Dienst
   und bringt für Deutsch sowohl Stemming als auch eine übersetzte Oberfläche mit. Kein anderer
   Kandidat erfüllt beides ohne Plugin eines Dritten oder Eigenbau.
2. **Karten-Katalog ohne Eigenbau**: `CardGrid`/`LinkCard` für den Use-Case-Bereich, `Steps` für
   Anleitungen, Asides für Hinweise, Astro-Bildoptimierung für Screenshots.
3. **Keine dritte Toolchain**: Node, eigenes `package.json` wie bei `tools/screenshots`. Die
   bestehenden Playwright-Skripte dort könnten später auch Doku-Screenshots liefern.
4. **Lebendig**: Wöchentliche Releases vom Astro-Team.

**Die Kosten**, ehrlich benannt:

- **Komponenten nur in `.mdx`/`.mdoc`.** Vorschlag als Pflegeregel: Anleitungsseiten bleiben reines
  `.md` (auf GitHub gut lesbar, `:::note` erscheint dort als Text), nur Katalog-/Indexseiten werden
  `.mdx`.
- **Vor 1.0**: Minor-Upgrades können brechen. Version pinnen, per Dependabot/Renovate gesammelt
  heben.
- **Versionierung nur über ein frühes Community-Plugin.** Falls das Hosting-Ticket Versionen je
  Release *verlangt*, ist **Docusaurus** (eingebaut, plus `docusaurus-search-local` mit `de`) die
  Ausweichwahl.

**Im Blick behalten: Zensical.** Wer GitHub-Lesbarkeit und Material-Optik höher gewichtet, findet
dort die reifere Autoren-Syntax. Neu prüfen lohnt sich, sobald drei Dinge da sind: eine stabile
Release-Linie (angekündigt ab 0.1.0, 2026-11-05), eine lokalisierte Such-UI und native
Versionierung. Python bliebe als dritte Toolchain.

## Offene Punkte

- **Komposita in der Suche**: Keine der Engines zerlegt deutsche Komposita dokumentiert. Ob
  „Sprechtag“ die Seite zum „Elternsprechtag“ findet, hängt von Präfix- bzw. Teilwortsuche ab. Im
  Prototyp mit Pagefind ausprobieren (z. B. „Sprechtag“, „Anmeldeschluss“, „Lehrkräfte“/„Lehrkraft“).
- **Wird Versionierung gebraucht?** Das entscheidet das Hosting-Ticket — und kann die Empfehlung
  zu Docusaurus kippen (siehe oben).
- **Wie viel GitHub-Lesbarkeit ist Pflicht?** Muss jede Seite auf GitHub *gerendert* gut aussehen
  oder nur lesbar sein? Bei „gerendert“ gewinnt VitePress (GitHub-Alerts), verliert aber Suche und
  Karten.
- **Node-Version in CI**: Starlight braucht Node ≥ 22.12; der Doku-Job bekommt ein eigenes
  `setup-node`, getrennt vom Vaadin-Build.
- **Seitenvorlage gegenprüfen**: Die Beispielseite aus #190 (Prototyp) zuerst in Starlight bauen,
  bevor die Entscheidung als ADR festgeschrieben wird.

## Quellen

- Material for MkDocs: [Blog „Zensical“ (2025-11-05)](https://squidfunk.github.io/mkdocs-material/blog/2025/11/05/zensical/),
  [Search-Plugin](https://squidfunk.github.io/mkdocs-material/plugins/search/),
  [Grids](https://squidfunk.github.io/mkdocs-material/reference/grids/),
  [Releases](https://github.com/squidfunk/mkdocs-material/releases)
- MkDocs: [Releases](https://github.com/mkdocs/mkdocs/releases)
- Zensical: [Upcoming changes](https://zensical.org/upcoming-changes/),
  [Site search](https://zensical.org/docs/setup/search/),
  [MkDocs-Kompatibilität](https://zensical.org/docs/compatibility/mkdocs/),
  [mike](https://zensical.org/docs/compatibility/mkdocs/mike/),
  [Grids](https://zensical.org/docs/authoring/grids/),
  [Releases](https://github.com/zensical/zensical/releases), [PyPI](https://pypi.org/project/zensical/)
- Docusaurus: [Search](https://docusaurus.io/docs/search),
  [Markdown features](https://docusaurus.io/docs/markdown-features),
  [Versioning](https://docusaurus.io/docs/versioning),
  [Generated index](https://docusaurus.io/docs/sidebar/items#embedding-generated-index),
  [docusaurus-search-local](https://github.com/easyops-cn/docusaurus-search-local)
- VitePress: [Local search](https://vitepress.dev/reference/default-theme-search),
  [Markdown](https://vitepress.dev/guide/markdown), [MiniSearch](https://lucaong.github.io/minisearch/),
  [Releases](https://github.com/vuejs/vitepress/releases)
- Starlight: [Site search](https://starlight.astro.build/guides/site-search/),
  [Authoring content](https://starlight.astro.build/guides/authoring-content/),
  [Using components](https://starlight.astro.build/components/using-components/),
  [Releases](https://github.com/withastro/starlight/releases),
  [starlight-versions](https://github.com/HiDeoo/starlight-versions)
- Pagefind: [Multilingual](https://pagefind.app/docs/multilingual/)
- Antora: [Doku](https://docs.antora.org/antora/latest/),
  [Antora-Lunr-Extension](https://gitlab.com/antora/antora-lunr-extension)
