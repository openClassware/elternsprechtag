// Eine Buchung stornieren: der Bestätigungsdialog mit Kind, Uhrzeit, Lehrkraft und dem Schalter für
// das Löschverlangen. Der Lauf bricht im Dialog ab — storniert wird nie, der Demo-Seed bleibt
// unverändert.

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
	await buchung.locator('.auswertung__storno').click();

	const dialog = page.locator('vaadin-dialog.storno-buchung-dialog');
	await dialog.locator('.storno-buchung-dialog__entfernen').waitFor();
	await foto(dialog.locator('[part="overlay"]'), 'dialog');

	await dialog.getByRole('button', { name: 'Abbrechen' }).click();
}
