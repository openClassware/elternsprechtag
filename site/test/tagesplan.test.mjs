// Das Beispielblatt der Doku kommt aus dem ZIP, das die Auswertung herunterlädt: je Lehrkraft ein
// PDF, das Kürzel am Ende des Dateinamens.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { strToU8, zipSync } from 'fflate';
import { blattAusZip } from '../screenshots/tagesplan.mjs';

const zip = zipSync({
	'2026-10-02_1437_Herbstsprechtag_BAU.pdf': strToU8('%PDF Bauer'),
	'2026-10-02_1437_Herbstsprechtag_KRA.pdf': strToU8('%PDF Krause'),
});

test('liefert das Blatt der Lehrkraft mit dem Kürzel', () => {
	assert.equal(new TextDecoder().decode(blattAusZip(zip, 'KRA')), '%PDF Krause');
});

test('fehlt das Blatt, nennt der Fehler die Dateien im ZIP', () => {
	assert.throws(() => blattAusZip(zip, 'WAG'), /_WAG\.pdf.*Herbstsprechtag_BAU\.pdf.*Herbstsprechtag_KRA\.pdf/s);
});

test('ein Kürzel trifft nur am Ende des Dateinamens', () => {
	const kurz = zipSync({ '2026-10-02_1437_KRAnkenhaus_BAU.pdf': strToU8('%PDF') });
	assert.throws(() => blattAusZip(kurz, 'KRA'));
});
