// Den Zugangs-Link an die Eltern weitergeben: der Dialog „Link teilen“ mit dem vollständigen Link.
// Der Lauf schließt den Dialog wieder, ohne zu kopieren.

/** Der aktive Sprechtag aus dem Demo-Seed — nur ein veröffentlichter hat „Link kopieren“. */
const SPRECHTAG = 'Elternsprechtag Klassen 5 und 6';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Link kopieren' }).click();

	const dialog = page.locator('vaadin-dialog').filter({ has: page.locator('.share-link-dialog__link') });
	await dialog.locator('.share-link-dialog__link').waitFor();
	await foto(dialog.locator('[part="overlay"]'), 'dialog');

	await dialog.getByRole('button', { name: 'Schließen' }).click();
}
