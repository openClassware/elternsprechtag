// Eine Buchung umbuchen: der Dialog mit den freien Terminen derselben Lehrkraft. Der Lauf bricht im
// Dialog ab — umgebucht wird nie, der Demo-Seed bleibt unverändert.

/** Der aktive Sprechtag aus dem Demo-Seed. */
const SPRECHTAG = 'Elternsprechtag Klassen 5 und 6';

/** Eine geltende Buchung an diesem Sprechtag. */
const KIND = 'Emre Yilmaz';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Auswerten' }).click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	const buchung = page.locator('.auswertung__row').filter({ hasText: KIND }).first();
	await buchung.locator('.auswertung__umbuchen').click();

	const dialog = page.locator('vaadin-dialog.umbuchen-dialog');
	await dialog.locator('.umbuchen-dialog__auswahl').waitFor();
	// Die Auswahl „Neuer Termin“ hat ein eigenes Overlay; das erste ist das des Dialogs.
	await foto(dialog.locator('[part="overlay"]').first(), 'dialog');

	await dialog.getByRole('button', { name: 'Abbrechen' }).click();
}
