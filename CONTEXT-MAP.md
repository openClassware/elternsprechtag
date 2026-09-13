# Context Map — Elternsprechtag

Das Projekt hat **zwei fachliche Kontexte**. Diese Datei sagt, welche es gibt, wo sie liegen und
wie sie zueinander stehen. Das Vokabular selbst steht je Kontext in dessen `CONTEXT.md`.

Wer hier landet, weil er einen Begriff sucht: Er steht in genau einem der beiden Glossare — und
welches, entscheidet die Frage „wer besitzt diesen Begriff", nicht „wo wird er angezeigt".

## Die beiden Kontexte

| Kontext              | Glossar                                                                  | Code                                                    |
|----------------------|--------------------------------------------------------------------------|---------------------------------------------------------|
| **Sprechtag**        | [`docs/contexts/sprechtag/CONTEXT.md`](docs/contexts/sprechtag/CONTEXT.md)               | `…/elternsprechtag/sprechtag/`         |
| **Schulorganisation**| [`docs/contexts/schulorganisation/CONTEXT.md`](docs/contexts/schulorganisation/CONTEXT.md) | `…/elternsprechtag/schulorganisation/` |

**Sprechtag** ist der Kontext des Ablaufs: Ein Sprechtag wird angelegt, veröffentlicht, Eltern
buchen Termine, der Organizer wertet aus. Alles mit Oberfläche gehört hierher.

**Schulorganisation** ist der Kontext der Stammdaten: Lehrkräfte, Klassen, Fächer und die
Lehraufträge, die sie verbinden. Er hat **keine Oberfläche** und ist das Ziel des später kommenden
Stammdaten-Imports.

## Wie sie zueinander stehen

```
  ┌──────────────────┐                          ┌──────────────────────┐
  │    Sprechtag     │ ──── liest, nie ────────▶│  Schulorganisation   │
  │                  │      schreibt            │                      │
  │  Oberfläche      │      über deren          │  keine Oberfläche    │
  │  Buchen          │      port/in             │  Ziel des Imports    │
  │  Auswerten       │                          │                      │
  └──────────────────┘                          └──────────────────────┘
```

Die Abhängigkeit ist **einseitig**: Der Sprechtag liest Stammdaten, die Schulorganisation weiß von
Sprechtagen nichts. Wer das umdrehen wollte, müsste erst erklären, warum eine Klasse wissen sollte,
an welchen Sprechtagen sie teilnimmt.

Drei Regeln halten die Grenze — alle drei stehen als ArchUnit-Test im Build (`ArchitekturTest`),
nicht nur hier:

1. **Nur über `port/in`.** Der Sprechtag ruft die Use-Case-Ports der Schulorganisation. Ihre
   Aggregate, Ports out, Services und Persistenzmodelle sind für ihn unsichtbar.
2. **Kein SQL joint über die Grenze.** Read-Modelle, die beide Seiten brauchen, holen ihre Teile
   aus zwei Ports und werden in Java zusammengefügt.
3. **Keine geteilten Typen.** Beide Kontexte haben ihre eigene `KlasseId`, ihren eigenen
   `Lehrauftrag`. Über die Grenze gehen nackte `UUID`s und Text — ein geteilter Typ wäre eine
   geteilte Abhängigkeit.

Die dritte Regel kostet Tipparbeit und ist der Punkt, an dem die Trennung sonst als Erstes
aufweicht. Der Gewinn steht in [ADR 0003](docs/adr/0003-hexagonale-architektur-mit-zwei-kontexten.md).

## Was die Grenze trägt

Eine **Buchung friert ihr Ziel ein**: Lehrkraft, Klasse und Fach stehen an der Buchung mit dem
Stand vom Buchungszeitpunkt, nicht als Verweis. Deshalb darf die Schulorganisation ihre Stammdaten
verändern und stilllegen, ohne dass eine vergangene Auswertung unlesbar wird — und genau das ist
die Voraussetzung dafür, dass ein Import hier überhaupt Datensätze entfernen darf.

## Wo die Entscheidungen stehen

Architektur- und Grundsatzentscheidungen liegen gemeinsam in [`docs/adr/`](docs/adr/) — beide
Kontexte sind aus derselben Codebasis geschnitten, und eine Aufteilung nach Kontext würde die
Entscheidungen zerreißen, die genau über die Grenze hinweg getroffen wurden (ADR 0003, 0004, 0005).

Architekturregeln im Ganzen: [`docs/arc/ARCHITECTURE.md`](docs/arc/ARCHITECTURE.md), Kurzfassung in
[`CLAUDE.md`](CLAUDE.md).
