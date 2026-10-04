// Einen Sprechtag absagen: der Bestätigungsdialog mit der Zahl der betroffenen Kinder. Der Lauf
// bricht im Dialog ab — abgesagt wird nie, der Demo-Seed bleibt unverändert.

/** Der aktive Sprechtag aus dem Demo-Seed, der Buchungen hat. */
const SPRECHTAG = 'Elternsprechtag Klassen 5 und 6';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Absagen' }).click();

	const dialog = page.locator('vaadin-dialog.cancel-sprechtag-dialog');
	await dialog.locator('.cancel-sprechtag-dialog__affected').waitFor();
	await foto(dialog.locator('[part="overlay"]'), 'dialog');

	await dialog.getByRole('button', { name: 'Abbrechen' }).click();
}
