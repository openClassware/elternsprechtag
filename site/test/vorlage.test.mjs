// Prüfregeln der Fallseiten-Vorlage, gegen Beispieldateien in `beispiele/`. Das Plugin läuft hier
// durch denselben Sätteri-Compile wie im Astro-Build, nur ohne Astro drumherum.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { basename } from 'node:path';
import { markdownToHtml, mdxToJs } from 'satteri';
import { vorlagenPruefung } from '../src/vorlage/pruefung.mjs';
import { fallseiteSchema } from '../src/vorlage/schema.mjs';

const ALS_FALLSEITE = new URL('../src/content/docs/anwendungsfaelle/', import.meta.url);
const ALS_ANDERE_SEITE = new URL('../src/content/docs/einstieg/', import.meta.url);

function baue(beispiel, ablage = ALS_FALLSEITE) {
	const quelle = readFileSync(new URL(`beispiele/${beispiel}`, import.meta.url), 'utf8');
	const optionen = { mdastPlugins: [vorlagenPruefung()], fileURL: new URL(basename(beispiel), ablage) };
	return beispiel.endsWith('.mdx') ? mdxToJs(quelle, optionen) : markdownToHtml(quelle, optionen);
}

function bautNicht(beispiel, regel, ablage) {
	assert.throws(
		() => baue(beispiel, ablage),
		(fehler) => {
			assert.match(fehler.message, new RegExp(beispiel.replace('.', '\\.')), 'Meldung nennt die Datei');
			assert.match(fehler.message, regel, 'Meldung nennt die verletzte Regel');
			return true;
		},
	);
}

test('eine Fallseite mit allen Abschnitten in der festen Reihenfolge baut', () => {
	assert.doesNotThrow(() => baue('gueltig.mdx'));
});

test('die echte Fallseite „Einen Sprechtag absagen“ hält die Vorlage ein', () => {
	assert.doesNotThrow(() => baue('../../src/content/docs/anwendungsfaelle/einen-sprechtag-absagen.mdx'));
});

test('ein fehlender Pflichtabschnitt bricht den Build', () => {
	bautNicht('fehlende-pflicht-h2.mdx', /Pflichtabschnitt „Was danach passiert“ fehlt/);
});

test('eine fremde H2 bricht den Build', () => {
	bautNicht('fremde-h2.mdx', /Abschnitt „Tipps“ ist in der Vorlage nicht vorgesehen/);
});

test('eine falsche Reihenfolge bricht den Build', () => {
	bautNicht('falsche-reihenfolge.mdx', /„Schritte“ muss vor „Was danach passiert“ stehen/);
});

test('ein leerer Abschnitt bricht den Build', () => {
	bautNicht('leerer-abschnitt.mdx', /Abschnitt „Regeln“ ist leer/);
});

test('Schritte ohne <Steps> brechen den Build', () => {
	bautNicht('schritte-ohne-steps.mdx', /„Schritte“ braucht eine <Steps>-Liste/);
});

test('„Wenn …“ ohne H3 bricht den Build', () => {
	bautNicht('wenn-ohne-h3.mdx', /„Wenn …“ braucht mindestens eine ###-Überschrift/);
});

test('eine H3 unter „Wenn …“, die nicht mit „…“ beginnt, bricht den Build', () => {
	bautNicht('wenn-h3-ohne-auslassung.mdx', /„Etwas schiefgeht“ unter „Wenn …“ muss mit „…“ beginnen/);
});

test('ein Bild ohne Alt-Text bricht den Build', () => {
	bautNicht('bild-ohne-alt.mdx', /Bild „\/dialog\.png“ hat keinen Alt-Text/);
});

test('„Screenshot“ allein zählt als leerer Alt-Text', () => {
	bautNicht('bild-alt-screenshot.mdx', /Bild „\/dialog\.png“ hat keinen Alt-Text/);
});

test('der Alt-Text wird auf der ganzen Site geprüft, nicht nur bei Fallseiten', () => {
	bautNicht('seite-mit-leerem-alt.md', /Bild „\/uebersicht\.png“ hat keinen Alt-Text/, ALS_ANDERE_SEITE);
});

test('ein data-alt in rohem HTML zählt nicht als Alt-Text', () => {
	bautNicht('seite-mit-data-alt.md', /Bild „\/dialog\.png“ hat keinen Alt-Text/, ALS_ANDERE_SEITE);
});

test('die Zeilenangabe zählt in der Datei, auch wenn das Plugin den Text ohne Frontmatter bekommt', () => {
	const datei = new URL('beispiele/seite-mit-leerem-alt.md', import.meta.url);
	const ohneFrontmatter = readFileSync(datei, 'utf8').replace(/^---[\s\S]*?---\r?\n/, '');
	assert.throws(
		() => markdownToHtml(ohneFrontmatter, { mdastPlugins: [vorlagenPruefung()], fileURL: datei }),
		/Zeile 7: Bild „\/uebersicht\.png“/,
	);
});

test('die Abschnittsregeln gelten nur für Fallseiten', () => {
	assert.doesNotThrow(() => baue('fremde-h2.mdx', ALS_ANDERE_SEITE));
});

const steckbrief = {
	title: 'Einen Sprechtag absagen',
	description: 'Ein Satz.',
	phase: 'kurz-vor-dem-termin',
	order: 2,
	wer: 'organisator',
	wann: 'Kurz vor dem Termin',
	voraussetzung: 'Der Sprechtag ist veröffentlicht',
};

function fehlerZu(frontmatter) {
	const ergebnis = fallseiteSchema.safeParse(frontmatter);
	assert.equal(ergebnis.success, false);
	return ergebnis.error.issues.map((issue) => `${issue.path.join('.')}: ${issue.message}`).join('\n');
}

test('ein vollständiger Steckbrief ist gültig, die Dauer ist optional', () => {
	assert.equal(fallseiteSchema.safeParse(steckbrief).success, true);
	assert.equal(fallseiteSchema.safeParse({ ...steckbrief, dauer: 'unter einer Minute' }).success, true);
});

test('ein fehlendes Pflichtfeld ist ungültig', () => {
	const { voraussetzung, ...ohne } = steckbrief;
	assert.match(fehlerZu(ohne), /voraussetzung: Pflichtfeld des Steckbriefs fehlt/);
});

test('eine unbekannte Phase ist ungültig und nennt die erlaubten', () => {
	assert.match(fehlerZu({ ...steckbrief, phase: 'irgendwann' }), /phase: .*vorbereiten, buchungsphase, kurz-vor-dem-termin, am-tag, danach/);
});

test('ein unbekannter Akteur ist ungültig', () => {
	assert.match(fehlerZu({ ...steckbrief, wer: 'eltern' }), /wer: .*organisator, lehrkraft/);
});
