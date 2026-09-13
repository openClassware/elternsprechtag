# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`CONTEXT-MAP.md`** at the repo root — it points at one `CONTEXT.md` per context. Read each one relevant to the topic.
- **`docs/adr/`** — read ADRs that touch the area you're about to work in.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

This is a **two-context** repo — `sprechtag` (the flow: create, publish, book, evaluate) and
`schulorganisation` (the master data: teacher, class, subject, teaching assignment). Start at
`CONTEXT-MAP.md`; it names both contexts and the one-way dependency between them.

```
/
├── CONTEXT-MAP.md                            ← start here
├── docs/
│   ├── contexts/
│   │   ├── sprechtag/CONTEXT.md
│   │   └── schulorganisation/CONTEXT.md
│   └── adr/
│       ├── 0001-....md
│       └── 0002-....md
└── src/
```

ADRs stay in one shared `docs/adr/`, not per context: the decisions that matter here (0003–0005)
were made across the boundary, and splitting them would tear them apart.

**Which glossary owns a term** is decided by "who owns this concept", not "where is it displayed".
`Lehrauftrag` belongs to `schulorganisation` even though only the `sprechtag` UI ever shows it.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in the owning context's `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (...) — but worth reopening because…_
