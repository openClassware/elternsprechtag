// Die Tagespläne drucken: der Kopf der Auswertung mit dem Knopf „Pläne herunterladen (ZIP)“,
// gerahmt. Heruntergeladen wird hier nicht; das Blatt selbst zeigt die Seite „Das Tagesplan-Blatt
// lesen“.

/** Der Sprechtag aus dem Demo-Seed, der in drei Tagen stattfindet. */
const SPRECHTAG = 'Elternsprechtag Klassen 7 und 8';

export default async function ({ page, basis, foto, rahmen, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Auswerten' }).click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	await rahmen(page.locator('.auswertung__druck-link vaadin-button'));
	await foto(page.locator('.auswertung__header'), 'kopf');
}
