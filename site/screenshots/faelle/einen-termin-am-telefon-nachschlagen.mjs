// Einen Termin am Telefon nachschlagen: die Auswertung mit dem Nachnamen im Feld „Name“. Familie
// Hartmann hat im Demo-Seed Termine bei zwei Lehrkräften; beide Blöcke bleiben stehen, die übrigen
// fallen weg.

/** Der Sprechtag aus dem Demo-Seed, der in drei Tagen stattfindet. */
const SPRECHTAG = 'Elternsprechtag Klassen 7 und 8';

const NAME = 'Hartmann';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Auswerten' }).click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	const abschnitte = page.locator('.auswertung__section');
	const vorher = await abschnitte.count();
	const eingabe = page.locator('.auswertung__suche input');
	await eingabe.fill(NAME);
	// Gefiltert wird beim Tippen; fertig ist es, wenn nur noch die Blöcke mit Treffer stehen.
	await page.waitForFunction(
		([selektor, anzahl]) => document.querySelectorAll(selektor).length < anzahl,
		['.auswertung__section', vorher],
	);
	// Kein Fokusrahmen im Bild; die Suche gilt weiter.
	await eingabe.blur();
	await bereit(page);

	await foto(page.locator('.auswertung__filter'), 'suche');
	await foto(page.locator('.auswertung__sections'), 'treffer');
}
