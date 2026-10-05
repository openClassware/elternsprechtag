// Das Tagesplan-Blatt lesen: lädt das ZIP der Tagespläne herunter, wie es der Organisator tut, und
// zeigt die erste Seite des Blatts von Anna Krause als Bild. Dasselbe PDF liegt daneben als
// Beispiel zum Herunterladen — erzeugt in diesem Lauf, nicht im Repo.
import { readFile } from 'node:fs/promises';
import { blattAusZip, fotografiereErsteSeite } from '../tagesplan.mjs';

/** Der Sprechtag aus dem Demo-Seed, der in drei Tagen stattfindet. */
const SPRECHTAG = 'Elternsprechtag Klassen 7 und 8';

/** Anna Krause hat dort im Demo-Seed belegte Termine, einen mit Notiz, und freie dazwischen. */
const KUERZEL = 'KRA';

export default async function ({ page, basis, foto, ablegen, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Auswerten' }).click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	const [download] = await Promise.all([
		page.waitForEvent('download'),
		page.locator('.auswertung__druck-link').click(),
	]);
	const pdf = blattAusZip(await readFile(await download.path()), KUERZEL);

	await ablegen('tagesplan-beispiel.pdf', pdf);
	await fotografiereErsteSeite({ kontext: page.context(), pdf, foto, name: 'blatt' });
}
