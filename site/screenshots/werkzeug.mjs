// Was die Screenshot-Module teilen: Anmeldung, Warten auf Vaadin, Aufnahme eines Ausschnitts und
// der Hervorhebungsrahmen. Ein Modul bekommt das fertig verdrahtet vom Runner.
import { mkdir } from 'node:fs/promises';
import path from 'node:path';

/** Wie die Seiten am Desktop erscheinen: ein Laptop-Fenster, scharf für Retina-Anzeigen. */
export const FENSTER = { viewport: { width: 1280, height: 800 }, deviceScaleFactor: 2 };

/**
 * Meldet sich als Organisator an. Die Zugangsdaten kommen aus der Umgebung und werden nie
 * ausgegeben — wie bei `tools/screenshots`.
 */
export async function anmelden(page, { basis, benutzer, passwort }) {
	await page.goto(`${basis}/login`, { waitUntil: 'networkidle' });
	// <vaadin-login-form> kapselt die Felder im offenen Shadow DOM; Playwright durchdringt es.
	await page.fill('input[name="username"]', benutzer);
	await page.fill('input[name="password"]', passwort);
	await page.press('input[name="password"]', 'Enter');
	await page
		.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 15_000 })
		.catch(() => {
			throw new Error('Anmeldung fehlgeschlagen: die App blieb auf /login. Stimmen ORGANIZER_USERNAME/ORGANIZER_PASSWORD?');
		});
	await bereit(page);
}

/**
 * Vaadin baut die View erst nach dem ersten Roundtrip auf, `networkidle` allein greift zu früh.
 * Gewartet wird, bis die Custom Elements definiert sind, dazu ein kurzer Puffer fürs Layout.
 */
export async function bereit(page) {
	await page.waitForFunction(() => document.querySelectorAll(':not(:defined)').length === 0).catch(() => {});
	await page.waitForTimeout(300);
}

/**
 * Hebt ein Element mit einem schlichten Rahmen hervor. Pfeile und Nummern gibt es nicht — der
 * Text der Fallseite sagt, worauf es ankommt.
 */
export async function rahmen(locator) {
	await locator.evaluate((element) => {
		element.style.outline = '3px solid #d97706';
		element.style.outlineOffset = '4px';
	});
}

/**
 * Liefert die Aufnahmefunktion eines Moduls: Sie fotografiert den Ausschnitt eines Locators nach
 * `<ziel>/<name>.png`. Ganze Seiten gibt es nicht. Greift der Selektor nicht, scheitert der Lauf.
 */
export function aufnahme(ziel) {
	return async (locator, name) => {
		await mkdir(ziel, { recursive: true });
		await locator.waitFor({ state: 'visible', timeout: 10_000 });
		const datei = path.join(ziel, `${name}.png`);
		await locator.screenshot({ path: datei, animations: 'disabled' });
		return datei;
	};
}
