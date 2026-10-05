// Einem Löschverlangen nachkommen: die Rückfrage „Angaben der Familie entfernen“ an einer Zeile des
// abgeschlossenen Sprechtags, und der Block einer Lehrkraft mit einer Zeile, deren Angaben schon
// entfernt sind. Der Lauf bricht im Dialog ab — entfernt wird nie; die entfernte Zeile kommt fertig
// aus dem Demo-Seed.

/** Der abgeschlossene Sprechtag aus dem Demo-Seed. */
const SPRECHTAG = 'Elternsprechtag Herbst';

/** Eine Familie mit geltenden Buchungen an diesem Sprechtag. */
const KIND = 'Jonas Wolf';

/** Der Platzhalter der Buchung, deren Angaben der Demo-Seed schon entfernt hat. */
const PLATZHALTER = 'Schueler-7c1e4a90-001';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Auswerten' }).click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	const buchung = page.locator('.auswertung__row').filter({ hasText: KIND }).first();
	await buchung.locator('.auswertung__entfernen').click();

	const dialog = page.locator('vaadin-dialog.angaben-entfernen-dialog');
	await dialog.locator('.angaben-entfernen-dialog__weitere').waitFor();
	await foto(dialog.locator('[part="overlay"]'), 'dialog');
	await dialog.getByRole('button', { name: 'Abbrechen' }).click();
	await dialog.waitFor({ state: 'hidden' });

	const block = page.locator('.auswertung__section').filter({ hasText: PLATZHALTER });
	await foto(block, 'zeile');
}
