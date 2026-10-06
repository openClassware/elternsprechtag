// Was die Screenshot-Module teilen: Anmeldung, Warten auf Vaadin, Aufnahme eines Ausschnitts, Ablage
// erzeugter Dateien, der Hervorhebungsrahmen und das Postfach mit den Mails der App. Ein Modul bekommt
// das fertig verdrahtet vom Runner.
import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { leseMail, neueDateien } from './mails.mjs';

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
 * Gewartet wird, bis die Vaadin-Komponenten definiert sind, dazu ein kurzer Puffer fürs Layout.
 * Nur `vaadin-*`: Auf der Elternseite bleibt Flows eigener Container (`flow-container-root-…`) für
 * immer undefiniert — die Bedingung würde nie wahr, jeder Aufruf liefe in den Timeout, und das
 * Abfragen im Takt der Bildwiederholung brachte den Tab nach einer Weile zum Absturz.
 */
export async function bereit(page) {
	await page
		.waitForFunction(() => ![...document.querySelectorAll(':not(:defined)')].some((e) => e.localName.startsWith('vaadin-')))
		.catch(() => {});
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

/**
 * Liefert die Ablage eines Moduls für erzeugte Dateien, die keine Ausschnitte sind — etwa ein
 * Beispiel-PDF zum Herunterladen. Sie liegen neben den Bildern unter `<ziel>/<name>`, ebenso
 * gitignored und vor jedem Lauf des Moduls geleert.
 */
export function ablage(ziel) {
	return async (name, inhalt) => {
		await mkdir(ziel, { recursive: true });
		const datei = path.join(ziel, name);
		await writeFile(datei, inhalt);
		return datei;
	};
}

/**
 * Das Postfach eines Laufs: die Mail-Ablage der App, wenn sie mit ELTERNSPRECHTAG_MAIL_ABLAGE
 * läuft. `stand()` merkt sich, was schon da ist; `warte(stand, betreff)` liefert die erste danach
 * abgelegte Mail, deren Betreff passt. Die App verschickt nach dem Speichern im Hintergrund, die
 * Mail kommt also einen Moment nach dem Klick — beim Erinnerungs-Scheduler erst mit seinem
 * nächsten Lauf.
 *
 * Ohne Verzeichnis scheitert erst der Zugriff, nicht der Lauf: Nur ein Modul, das Mails braucht,
 * braucht die Ablage.
 */
export function postfach(verzeichnis) {
	const fehlt = () => {
		throw new Error(
			'MAIL_ABLAGE ist nicht gesetzt — ohne die Mail-Ablage der App gibt es keine Mailtexte.\n' +
				'Die App muss mit ELTERNSPRECHTAG_MAIL_ABLAGE auf dasselbe Verzeichnis laufen, siehe site/README.md.',
		);
	};
	if (!verzeichnis) return { stand: fehlt, warte: fehlt };

	const liste = async () => {
		await mkdir(verzeichnis, { recursive: true });
		return readdir(verzeichnis);
	};
	return {
		stand: liste,
		async warte(vorher, betreff, { timeout = 30_000 } = {}) {
			const ende = Date.now() + timeout;
			while (Date.now() < ende) {
				for (const datei of neueDateien(vorher, await liste())) {
					const mail = leseMail(await readFile(path.join(verzeichnis, datei), 'utf8'));
					if (betreff.test(mail.betreff)) return mail;
				}
				await new Promise((weiter) => setTimeout(weiter, 250));
			}
			throw new Error(`Keine Mail mit Betreff ${betreff} in ${verzeichnis} nach ${timeout / 1000} s.`);
		},
	};
}
