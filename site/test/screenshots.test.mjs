// Auswahl der Screenshot-Module: Der Runner bricht ab, bevor er einen Browser startet, wenn ein
// Modul keine Fallseite hat oder ein angefragter Slug kein Modul.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { waehleModule } from '../screenshots/auswahl.mjs';

const module = ['einen-sprechtag-absagen', 'eine-buchung-stornieren'];
const seiten = ['einen-sprechtag-absagen', 'eine-buchung-stornieren', 'einen-entwurf-loeschen'];

test('ohne Argument laufen alle Module', () => {
	assert.deepEqual(waehleModule({ module, seiten, angefragt: [] }), module);
});

test('mit Slug läuft nur dieses Modul', () => {
	assert.deepEqual(waehleModule({ module, seiten, angefragt: ['eine-buchung-stornieren'] }), [
		'eine-buchung-stornieren',
	]);
});

test('eine Seite ohne Modul ist erlaubt — sie hat keine Bilder', () => {
	assert.doesNotThrow(() => waehleModule({ module, seiten, angefragt: [] }));
});

test('ein verwaistes Modul bricht ab und nennt die fehlende Seite', () => {
	assert.throws(
		() => waehleModule({ module: [...module, 'einen-sprechtag-verlegen'], seiten, angefragt: [] }),
		/einen-sprechtag-verlegen\.mjs.*anwendungsfaelle\/einen-sprechtag-verlegen\.mdx/s,
	);
});

test('ein verwaistes Modul bricht auch ab, wenn nur ein anderes angefragt ist', () => {
	assert.throws(
		() =>
			waehleModule({
				module: [...module, 'einen-sprechtag-verlegen'],
				seiten,
				angefragt: ['einen-sprechtag-absagen'],
			}),
		/einen-sprechtag-verlegen/,
	);
});

test('ein angefragter Slug ohne Modul bricht ab und nennt die vorhandenen', () => {
	assert.throws(
		() => waehleModule({ module, seiten, angefragt: ['einen-entwurf-loeschen'] }),
		/einen-entwurf-loeschen.*einen-sprechtag-absagen, eine-buchung-stornieren/s,
	);
});
