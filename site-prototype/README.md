# PROTOTYP — Vorlage einer Use-Case-Seite

Wegwerf-Code zum Ticket „Vorlage einer Use-Case-Seite“ (#195) der Karte „Fachliche Anwender-Doku als
Doku-Site“ (#190). Nicht für `main`.

- `src/content/docs/anwendungsfaelle/einen-sprechtag-absagen.mdx` — **die gewählte Vorlage** (A + Weiche aus C)
- `…-a.mdx`, `…-b.mdx`, `…-c.mdx` — die drei verglichenen Fassungen (Steckbrief · Anleitung zuerst · Erst prüfen)
- `anwendungsfaelle/index.mdx` — Katalog mit zwei Kartenformen (gewählt: Form 1)

Starten (Node ≥ 22): `npm install && npm run build && npx astro preview` → http://localhost:4321/anwendungsfaelle/
