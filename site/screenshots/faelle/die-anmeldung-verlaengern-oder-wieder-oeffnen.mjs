// Die Anmeldung verlängern oder wieder öffnen: das Feld „Anmeldefrist“ des Sprechtags, dessen
// Anmeldung im Demo-Seed beendet ist, mit einer kürzeren Frist und dem errechneten neuen
// Anmeldeschluss darunter. Gespeichert wird nie; der Demo-Seed bleibt unverändert.

/** Der Sprechtag aus dem Demo-Seed, dessen Anmeldung beendet ist — er liegt drei Tage voraus. */
const SPRECHTAG = 'Elternsprechtag Klassen 7 und 8';

export default async function ({ page, basis, foto, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);

	const zeile = page.locator('.sprechtag-table__row').filter({ hasText: SPRECHTAG });
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: 'Bearbeiten' }).click();
	await page.waitForURL(/\/sprechtag\//);
	await bereit(page);

	// Zwei Tage statt sieben: Der Anmeldeschluss rückt hinter heute, die Anmeldung wäre wieder offen.
	const feld = page.locator('vaadin-integer-field').filter({ hasText: 'Anmeldefrist' });
	const eingabe = feld.locator('input');
	await eingabe.fill('2');
	// Kein Fokus im Bild: Der Wert gilt mit dem Verlassen des Feldes, danach bleibt nichts markiert.
	await eingabe.blur();
	await feld.getByText('Anmeldung bis').waitFor();
	await bereit(page);

	await foto(feld, 'anmeldefrist');
}
