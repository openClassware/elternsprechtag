// Ein Auskunftsverlangen beantworten: die Suche nach Familie Wolf am abgeschlossenen Sprechtag. Sie
// hat dort drei geltende Termine und eine stornierte Buchung; der Hinweis unter der Suche nennt die
// stornierte, ein Klick holt sie in den Plan.

/** Der abgeschlossene Sprechtag aus dem Demo-Seed. */
const SPRECHTAG = 'Elternsprechtag Herbst';

const NAME = 'Wolf';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Auswerten' }).click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	const eingabe = page.locator('.auswertung__suche input');
	await eingabe.fill(NAME);
	// Gefiltert wird beim Tippen; fertig ist es, wenn der Hinweis auf die stornierte Buchung steht.
	const hinweis = page.locator('.auswertung__suchhinweis');
	await hinweis.waitFor({ state: 'visible' });
	// Kein Fokusrahmen im Bild; die Suche gilt weiter.
	await eingabe.blur();
	await bereit(page);
	await foto(hinweis, 'hinweis');

	await hinweis.locator('.auswertung__suchhinweis-button').click();
	await page.locator('.auswertung__row--storniert').first().waitFor({ state: 'visible' });
	await bereit(page);
	await foto(page.locator('.auswertung__sections'), 'treffer');
}
