// Steckbrief einer Fallseite: das Frontmatter unter `anwendungsfaelle/`. Die Content Collection
// `anwendungsfaelle` prüft es beim Build, Katalog und Steckbrief-Tabelle lesen es aus.
import { z } from 'astro/zod';

/** Die Phasen des Schulablaufs, in der Reihenfolge des Katalogs. `id` ist zugleich der Anker. */
export const PHASEN = [
	{ id: 'vorbereiten', titel: 'Vorbereiten & veröffentlichen' },
	{ id: 'buchungsphase', titel: 'Buchungsphase' },
	{ id: 'kurz-vor-dem-termin', titel: 'Kurz vor dem Termin' },
	{ id: 'am-tag', titel: 'Am Tag' },
	{ id: 'danach', titel: 'Danach' },
];

/** Genau ein Akteur je Fall. Der Katalog kennzeichnet nur, was nicht der Organisator erledigt. */
export const AKTEURE = {
	organisator: 'Organisator',
	lehrkraft: 'Lehrkraft',
};

/** Fehlt das Feld, sagt die Meldung das — sonst nennt sie, was erwartet war. */
function meldung(erwartet) {
	return (issue) => (issue.input === undefined ? 'Pflichtfeld des Steckbriefs fehlt' : erwartet);
}

function text() {
	return z.string({ error: meldung('muss Text sein') }).trim().min(1, { error: 'darf nicht leer sein' });
}

function auswahl(werte) {
	return z.enum(werte, { error: meldung(`muss eines von ${werte.join(', ')} sein`) });
}

export const fallseiteSchema = z.object({
	title: text(),
	/** Ein Satz: Ziel und wichtigste Folge. Zugleich der Text der Katalogkarte. */
	description: text(),
	phase: auswahl(PHASEN.map((phase) => phase.id)),
	/** Reihenfolge innerhalb der Phase. */
	order: z.number({ error: meldung('muss eine Zahl sein') }).int(),
	wer: auswahl(Object.keys(AKTEURE)),
	wann: text(),
	voraussetzung: text(),
	dauer: text().optional(),
});
