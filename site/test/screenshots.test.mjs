// Auswahl der Screenshot-Module: Der Runner bricht ab, bevor er einen Browser startet, wenn ein
// Modul keine Seite hat oder ein angefragter Slug kein Modul.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { waehleModule } from '../screenshots/auswahl.mjs';

const module = ['faelle/einen-sprechtag-absagen', 'faelle/eine-buchung-stornieren', 'referenz/welche-e-mails-eltern-bekommen'];
const seiten = [
	'anwendungsfaelle/einen-sprechtag-absagen',
	'anwendungsfaelle/eine-buchung-stornieren',
	'anwendungsfaelle/einen-entwurf-loeschen',
	'referenz/welche-e-mails-eltern-bekommen',
	'referenz/glossar',
];

test('ohne Argument laufen alle Module', () => {
	assert.deepEqual(waehleModule({ module, seiten, angefragt: [] }), module);
});

test('mit Slug läuft nur dieses Modul', () => {
	assert.deepEqual(waehleModule({ module, seiten, angefragt: ['eine-buchung-stornieren'] }), [
		'faelle/eine-buchung-stornieren',
	]);
});

test('ein Modul der Referenz wird über seinen Slug gefunden', () => {
	assert.deepEqual(waehleModule({ module, seiten, angefragt: ['welche-e-mails-eltern-bekommen'] }), [
		'referenz/welche-e-mails-eltern-bekommen',
	]);
});

test('eine Seite ohne Modul ist erlaubt — sie hat keine Bilder', () => {
	assert.doesNotThrow(() => waehleModule({ module, seiten, angefragt: [] }));
});

test('ein verwaistes Modul bricht ab und nennt die fehlende Seite', () => {
	assert.throws(
		() => waehleModule({ module: [...module, 'faelle/einen-sprechtag-verlegen'], seiten, angefragt: [] }),
		/faelle\/einen-sprechtag-verlegen\.mjs.*anwendungsfaelle\/einen-sprechtag-verlegen\.mdx/s,
	);
});

test('ein Modul der Referenz braucht seine Seite in der Referenz, nicht unter den Fällen', () => {
	assert.throws(
		() =>
			waehleModule({
				module: ['referenz/einen-sprechtag-absagen'],
				seiten,
				angefragt: [],
			}),
		/referenz\/einen-sprechtag-absagen\.mjs.*referenz\/einen-sprechtag-absagen\.mdx/s,
	);
});

test('ein verwaistes Modul bricht auch ab, wenn nur ein anderes angefragt ist', () => {
	assert.throws(
		() =>
			waehleModule({
				module: [...module, 'faelle/einen-sprechtag-verlegen'],
				seiten,
				angefragt: ['einen-sprechtag-absagen'],
			}),
		/einen-sprechtag-verlegen/,
	);
});

test('ein angefragter Slug ohne Modul bricht ab und nennt die vorhandenen', () => {
	assert.throws(
		() => waehleModule({ module, seiten, angefragt: ['einen-entwurf-loeschen'] }),
		/einen-entwurf-loeschen.*einen-sprechtag-absagen, eine-buchung-stornieren, welche-e-mails-eltern-bekommen/s,
	);
});
