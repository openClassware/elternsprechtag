// Nicht erreichte Familien nachfassen: der Hinweis in der Liste und der Block „Nicht erreicht“ oben
// in der Auswertung. Beides gibt es nur, weil der Demo-Seed am Sprechtag mit beendeter Anmeldung
// eine gescheiterte Ausfall-Mail hat (Issue #207). Der Lauf ändert nichts.

/** Der Sprechtag aus dem Demo-Seed mit der gescheiterten Ausfall-Mail. */
const SPRECHTAG = 'Elternsprechtag Klassen 7 und 8';

export default async function ({ page, basis, foto, rahmen, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	const hinweis = zeile.locator('.sprechtag-table__nicht-erreicht');
	await rahmen(hinweis);
	await foto(zeile, 'hinweis-in-der-liste');

	await hinweis.click();
	await page.waitForURL(/\/auswertung\//);
	await bereit(page);

	await foto(page.locator('.auswertung__nicht-erreicht'), 'block-nicht-erreicht');
}
