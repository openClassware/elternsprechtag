// Die Buchungen eines Sprechtags einsehen: die Filterzeile der Auswertung mit eingeschaltetem
// „Stornierte anzeigen“ und der Block einer Lehrkraft, in dem eine stornierte Zeile steht. Den
// Schalter gibt es nur, weil der Demo-Seed am aktiven Sprechtag eine stornierte Buchung hat.

/** Der aktive Sprechtag aus dem Demo-Seed. */
const SPRECHTAG = 'Elternsprechtag Klassen 5 und 6';

/** Die Lehrkraft, bei der der Demo-Seed eine stornierte Buchung hat. */
const LEHRKRAFT = 'Anna Krause';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Auswerten' }).click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	await page.locator('.auswertung__stornierte-schalter').click();
	const block = page.locator('.auswertung__section').filter({ hasText: LEHRKRAFT });
	await block.locator('.auswertung__row--storniert').waitFor();
	await bereit(page);

	await foto(page.locator('.auswertung__filter'), 'filter');
	await foto(block, 'plan-mit-stornierter');
}
