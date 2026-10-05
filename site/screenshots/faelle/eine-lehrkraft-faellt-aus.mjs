// Eine Lehrkraft fällt aus: der Dialog mit den Terminen der Lehrkraft, zwei gebuchte angehakt, damit
// die Zusammenfassung „… Termine ausgewählt · etwa … Kinder betroffen“ etwas zeigt. Der Lauf bricht
// im Dialog ab — entfallen lässt er nie, der Demo-Seed bleibt unverändert.

/** Der aktive Sprechtag aus dem Demo-Seed: Die Anmeldung läuft, die Mail führt zurück in die Buchung. */
const SPRECHTAG = 'Elternsprechtag Klassen 5 und 6';

/** Die Lehrkraft, bei der der Demo-Seed gleich zu Beginn zwei geltende Buchungen hat. */
const LEHRKRAFT = 'Anna Krause';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Auswerten' }).click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	const block = page.locator('.auswertung__section').filter({ hasText: LEHRKRAFT });
	await block.locator('.auswertung__ausfall-button').click();

	const dialog = page.locator('vaadin-dialog.ausfall-dialog');
	const gebucht = dialog.locator('.ausfall-dialog__row').filter({ hasText: 'gebucht' });
	await gebucht.first().waitFor();
	for (let i = 0; i < 2; i++) {
		await gebucht.nth(i).locator('vaadin-checkbox').click();
		await bereit(page);
	}
	await foto(dialog.locator('[part="overlay"]'), 'dialog');

	await dialog.getByRole('button', { name: 'Abbrechen' }).click();
}
