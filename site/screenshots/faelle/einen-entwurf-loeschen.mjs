// Einen Entwurf löschen: der Bestätigungsdialog mit dem Titel des Entwurfs. Der Lauf bricht im
// Dialog ab — gelöscht wird nie, der Demo-Seed bleibt unverändert.

/** Der Entwurf aus dem Demo-Seed. */
const SPRECHTAG = 'Elternsprechtag Sommer';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Löschen' }).click();

	const dialog = page.locator('vaadin-dialog.delete-sprechtag-dialog');
	await dialog.locator('.delete-sprechtag-dialog__message').waitFor();
	await foto(dialog.locator('[part="overlay"]'), 'dialog');

	await dialog.getByRole('button', { name: 'Abbrechen' }).click();
}
