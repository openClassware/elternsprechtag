// Einen Sprechtag anlegen: der Block „Termin & Zeiten“ mit den errechneten Tagen unter Erinnerung
// und Anmeldefrist. Fotografiert wird das Formular des Entwurfs aus dem Demo-Seed — es ist dasselbe
// wie beim Anlegen, hat aber schon ein Datum. Der Lauf setzt nur die Erinnerung, gespeichert wird
// nie; der Demo-Seed bleibt unverändert.

/** Der Entwurf aus dem Demo-Seed. */
const SPRECHTAG = 'Elternsprechtag Sommer';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Bearbeiten' }).click();
	await page.waitForURL(/\/sprechtag\//);
	await bereit(page);

	// Mit Erinnerung zeigt das Feld darunter den errechneten Tag, ohne nur „0 = keine Erinnerung“.
	const erinnerung = page.locator('vaadin-integer-field').filter({ hasText: 'Erinnerung' }).locator('input');
	await erinnerung.fill('1');
	// Kein Fokus im Bild: Der Wert gilt mit dem Verlassen des Feldes, danach bleibt nichts markiert.
	await erinnerung.blur();
	await bereit(page);

	const block = page.locator('.form-panel').filter({ has: page.locator('.form-panel__title', { hasText: 'Termin & Zeiten' }) });
	await foto(block, 'termin-und-zeiten');
}
