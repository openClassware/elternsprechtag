// Eine Familie nachtragen: der Block „Ihre Angaben“ mit dem Schalter für Familien ohne eigene
// E-Mail-Adresse. Den Schalter gibt es nur mit konfigurierter Stellvertreteradresse — die setzt die
// Doku-Instanz (.github/actions/doku-site). Nachgetragen wird nie.

/** Der aktive Sprechtag aus dem Demo-Seed. */
const SPRECHTAG = 'Elternsprechtag Klassen 5 und 6';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Auswerten' }).click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	await page.locator('.auswertung__nachtragen-button').click();
	await page.waitForURL(/\/nachtragen\//);
	await bereit(page);

	const angaben = page.locator('.elternsprechtag-view__section').filter({ has: page.locator('.nachtragen-view__stellvertreter-schalter') });
	await foto(angaben, 'angaben');
}
