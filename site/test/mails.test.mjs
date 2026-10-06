// Die Mail-Ablage der App lesen: Aufbau wie in `DateiBenachrichtigungSender` (An, Betreff,
// Leerzeile, Text). Die Seite zeigt Betreff und Text, die Adresse nicht.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { alsTextblock, leseMail, neueDateien } from '../screenshots/mails.mjs';

const ABSAGE = [
	'An: mueller@example.org',
	'Betreff: Sprechtag „Frühjahr“ am 12. März 2027 abgesagt',
	'',
	'Guten Tag,',
	'',
	'der Sprechtag muss leider abgesagt werden.',
].join('\n');

test('liest Empfänger, Betreff und Text', () => {
	assert.deepEqual(leseMail(ABSAGE), {
		an: 'mueller@example.org',
		betreff: 'Sprechtag „Frühjahr“ am 12. März 2027 abgesagt',
		text: 'Guten Tag,\n\nder Sprechtag muss leider abgesagt werden.',
	});
});

test('liest auch Dateien mit Windows-Zeilenenden', () => {
	assert.equal(leseMail(ABSAGE.replaceAll('\n', '\r\n')).text, 'Guten Tag,\n\nder Sprechtag muss leider abgesagt werden.');
});

test('eine Datei ohne Kopf ist ein Fehler, keine leere Mail', () => {
	assert.throws(() => leseMail('Guten Tag,'), /An:.*Betreff:/s);
});

test('der Textblock zeigt Betreff und Text, aber keine Adresse', () => {
	const block = alsTextblock(leseMail(ABSAGE));
	assert.equal(block, 'Betreff: Sprechtag „Frühjahr“ am 12. März 2027 abgesagt\n\nGuten Tag,\n\nder Sprechtag muss leider abgesagt werden.\n');
	assert.doesNotMatch(block, /mueller@example\.org/);
});

test('neu sind nur fertige Mails, die vorher nicht da waren, in Versandreihenfolge', () => {
	const vorher = ['0001727000000000-000001.txt'];
	const jetzt = ['0001727000000002-000003.txt', '0001727000000000-000001.txt', '0001727000000001-000002.txt', '0001727000000003-000004.schreibt'];
	assert.deepEqual(neueDateien(vorher, jetzt), ['0001727000000001-000002.txt', '0001727000000002-000003.txt']);
});
