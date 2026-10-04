// Einen Sprechtag bearbeiten: der Block „Termin & Zeiten“ eines veröffentlichten Sprechtags, mit
// gesperrtem Datum, gesperrten Zeiten und der Begründung darunter. Gespeichert wird nie.

/** Der aktive Sprechtag aus dem Demo-Seed. */
const SPRECHTAG = 'Elternsprechtag Klassen 5 und 6';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Bearbeiten' }).click();
	await page.waitForURL(/\/sprechtag\//);
	await bereit(page);

	const block = page.locator('.form-panel').filter({ has: page.locator('.form-panel__title', { hasText: 'Termin & Zeiten' }) });
	await block.getByText('Liegt seit dem Veröffentlichen fest').waitFor();
	await foto(block, 'gesperrte-zeiten');
}
