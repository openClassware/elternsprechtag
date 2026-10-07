// Der Bereich Einstieg und die Startseite. Jede Karte der Startseite führt auf eine Seite, die es
// gibt. Jedes „bewusst nein“ aus `ABDECKUNG.md` steht auf *Was die Anwendung kann – und was nicht*,
// als eine H3 unter „Was sie bewusst nicht kann“. Geprüft wird dort nur die Zahl: Die Seite spricht
// Anwendersprache, der Maßstab Glossarsprache — wörtlich gleich sind die beiden nie.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

const ABDECKUNG = new URL('../../docs/arc/ABDECKUNG.md', import.meta.url);
const DOCS = new URL('../src/content/docs/', import.meta.url);
const SEITE = new URL('einstieg/was-die-anwendung-kann-und-was-nicht.md', DOCS);
const ABSCHNITT = '## Was sie bewusst nicht kann';

/** Die Zeilen der Falltabellen, deren Spalte „Stufe“ auf „bewusst nein“ steht. */
function bewusstNeinImMassstab() {
	return readFileSync(ABDECKUNG, 'utf8')
		.split('\n')
		.filter((zeile) => zeile.startsWith('|'))
		.map((zeile) => zeile.split('|').map((zelle) => zelle.trim()))
		.filter((zellen) => zellen[4] === 'bewusst nein')
		.map((zellen) => zellen[1]);
}

/** Die H3 zwischen „Was sie bewusst nicht kann“ und der nächsten H2. */
function eintraegeAufDerSeite() {
	const zeilen = readFileSync(SEITE, 'utf8').split('\n');
	const anfang = zeilen.indexOf(ABSCHNITT);
	assert.notEqual(anfang, -1, `Die Seite hat keinen Abschnitt „${ABSCHNITT.slice(3)}“`);
	const rest = zeilen.slice(anfang + 1);
	const ende = rest.findIndex((zeile) => zeile.startsWith('## '));
	return (ende === -1 ? rest : rest.slice(0, ende)).filter((zeile) => zeile.startsWith('### '));
}

test('jede Karte der Startseite führt auf eine existierende Seite', () => {
	const startseite = readFileSync(new URL('index.mdx', DOCS), 'utf8');
	const ziele = [...startseite.matchAll(/<LinkCard[^>]*?href="\/([^"]*)\/"/gs)].map((treffer) => treffer[1]);
	assert.equal(ziele.length, 4, 'Die Startseite hat vier Einstiegskarten, eine je Lesergruppe');
	for (const ziel of ziele) {
		const kandidaten = ['.md', '.mdx', '/index.md', '/index.mdx'].map((endung) => new URL(ziel + endung, DOCS));
		assert.ok(kandidaten.some(existsSync), `Die Karte auf /${ziel}/ führt auf keine Seite`);
	}
});

test('jedes „bewusst nein“ aus ABDECKUNG.md steht auf „Was die Anwendung kann – und was nicht“', () => {
	const massstab = bewusstNeinImMassstab();
	assert.ok(massstab.length > 0, 'ABDECKUNG.md hat keine Zeile „bewusst nein“ — Tabellenformat geändert?');
	assert.equal(
		eintraegeAufDerSeite().length,
		massstab.length,
		`ABDECKUNG.md stuft ${massstab.length} Fälle als „bewusst nein“ ein:\n  ${massstab.join('\n  ')}`,
	);
});
