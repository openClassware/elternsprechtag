// Ein Sprechtag, den nur ein Modul anfasst. Wer etwas auslösen muss, das den Datenstand ändert —
// eine Buchung, eine Absage —, tut es hier statt an einem Sprechtag des Demo-Seeds: Die anderen
// Module fotografieren den Seed und müssen ihn so vorfinden, wie er geschrieben ist, in jeder
// Reihenfolge.
//
// Angelegt wird per „Duplizieren“ aus dem abgesagten Sprechtag des Seeds. Den fotografiert kein
// Modul über seinen Titel; eine Kopie, die ein abgebrochener Lauf unter seinem Namen hinterlässt,
// stört also niemanden. Am Ende sagt das Modul seinen Sprechtag ab — er bleibt als *Abgesagt*
// stehen, wie jeder veröffentlichte Sprechtag.

/** Der abgesagte Sprechtag aus dem Demo-Seed, Vorlage jeder Kopie. */
const VORLAGE = 'Elternsprechtag Winter';

/** Titel des eigenen Sprechtags. Alle Module, die einen brauchen, teilen ihn — `legeSprechtagAn` räumt
 * vorher auf, was ein anderes Modul darunter hinterlassen hat. */
export const EIGENER_SPRECHTAG = 'Elternsprechtag Frühjahr';

/** Die Familie, die an einem eigenen Sprechtag bucht. */
export const FAMILIE = { name: 'Anna Müller', kind: 'Lukas Müller', email: 'anna.mueller@example.org' };

/** Eine Zeile der Sprechtag-Liste, deren Titel genau so lautet. */
function zeileMit(page, titel) {
	return page
		.locator('.sprechtag-table__row')
		.filter({ has: page.locator('.sprechtag-table__title').getByText(titel, { exact: true }) });
}

async function zurListe(page, { basis, bereit }) {
	await page.goto(`${basis}/sprechtage`, { waitUntil: 'networkidle' });
	await bereit(page);
}

async function waehleImMenu(page, zeile, eintrag) {
	await zeile.locator('.sprechtag-table__menu').click();
	await page.locator('vaadin-context-menu-item').filter({ hasText: eintrag }).click();
}

/**
 * Räumt auf, was ein abgebrochener Lauf unter diesem Titel hinterlassen hat: Ein Entwurf wird
 * gelöscht, ein aktiver Sprechtag abgesagt. Danach gibt es höchstens noch abgesagte.
 */
async function raeumeAuf(page, werkzeug, titel) {
	for (;;) {
		await zurListe(page, werkzeug);
		const offen = zeileMit(page, titel).filter({ hasNotText: 'Abgesagt' }).first();
		if ((await offen.count()) === 0) return;
		if ((await offen.locator('.status-badge').innerText()).includes('Entwurf')) {
			await waehleImMenu(page, offen, 'Löschen');
			await page.locator('vaadin-dialog.delete-sprechtag-dialog').getByRole('button', { name: 'Löschen' }).click();
		} else {
			await waehleImMenu(page, offen, 'Absagen');
			await page.locator('vaadin-dialog.cancel-sprechtag-dialog').getByRole('button', { name: 'Absagen' }).click();
		}
		await werkzeug.bereit(page);
	}
}

/** Ein Datum als `TT.MM.JJJJ`, wie das Datumsfeld es tippen lässt. */
function alsEingabe(datum) {
	const zweistellig = (zahl) => String(zahl).padStart(2, '0');
	return `${zweistellig(datum.getDate())}.${zweistellig(datum.getMonth() + 1)}.${datum.getFullYear()}`;
}

/** Füllt ein Feld des Formulars und verlässt es, damit der Wert gilt. */
export async function setzeFeld(eingabe, wert) {
	await eingabe.fill(wert);
	await eingabe.press('Enter');
	await eingabe.blur();
}

/**
 * Legt einen veröffentlichten Sprechtag an, `inTagen` Tage ab heute, mit Erinnerung und
 * Anmeldefrist in Tagen davor.
 *
 * @returns {Promise<{ id: string, link: string, titel: string }>} die Id (für `/auswertung/<id>`)
 *   und den Zugangs-Link der Eltern
 */
export async function legeSprechtagAn(page, werkzeug, { titel, inTagen, erinnerung, anmeldefrist }) {
	const { bereit } = werkzeug;
	await raeumeAuf(page, werkzeug, titel);

	await waehleImMenu(page, zeileMit(page, VORLAGE).first(), 'Duplizieren');
	await page.waitForURL(/\/sprechtag\/[0-9a-f-]{36}$/);
	await bereit(page);
	const id = page.url().split('/').at(-1);

	const datum = new Date();
	datum.setDate(datum.getDate() + inTagen);
	const feld = (label) => page.getByLabel(label, { exact: true });
	await setzeFeld(feld('Titel'), titel);
	await setzeFeld(feld('Datum'), alsEingabe(datum));
	await setzeFeld(feld('Erinnerung (Tage vor dem Sprechtag)'), String(erinnerung));
	await setzeFeld(feld('Anmeldefrist (Tage vor dem Sprechtag)'), String(anmeldefrist));
	const link = await feld('Link für Eltern').inputValue();
	await bereit(page);
	await page.getByRole('button', { name: 'Speichern' }).click();
	await page.waitForURL((url) => !url.pathname.startsWith('/sprechtag/'));
	await zurListe(page, werkzeug);

	await waehleImMenu(page, zeileMit(page, titel).filter({ hasText: 'Entwurf' }), 'Aktiv setzen');
	await zeileMit(page, titel).filter({ hasText: 'Aktiv' }).waitFor();
	return { id, link, titel };
}

/** Sagt den eigenen Sprechtag ab — die Absage-Mail geht dabei an alle, die noch gebucht haben. */
export async function sageAb(page, werkzeug, { titel }) {
	await zurListe(page, werkzeug);
	await waehleImMenu(page, zeileMit(page, titel).filter({ hasText: 'Aktiv' }), 'Absagen');
	await page.locator('vaadin-dialog.cancel-sprechtag-dialog').getByRole('button', { name: 'Absagen' }).click();
	await zeileMit(page, titel).filter({ hasText: 'Aktiv' }).waitFor({ state: 'detached' });
}

/**
 * Bucht über den Zugangs-Link, wie eine Familie es tut: Angaben, Klasse, je Lehrkraft ein Termin,
 * auf Wunsch mit Notiz. Die i-te Lehrkraft bekommt ihren i-ten freien Termin — so fallen keine zwei
 * auf dieselbe Uhrzeit. Bleibt mit der Bestätigungsseite stehen.
 *
 * @param {{ name: string, kind: string, email: string, klasse: string,
 *   termine: { lehrkraft: string, notiz?: string }[] }} familie
 */
export async function bucheAlsFamilie(page, { bereit }, link, familie) {
	await page.goto(link, { waitUntil: 'networkidle' });
	await bereit(page);
	await fuelleAus(page, { bereit }, familie);
	await page.locator('.elternsprechtag-view__footer vaadin-button').click();
	await page.getByText('Ihre Termine sind gebucht').waitFor();
	await bereit(page);
}

/** Füllt die Buchungsseite aus, ohne abzuschicken. */
export async function fuelleAus(page, { bereit }, { name, kind, email, klasse, termine }) {
	const feld = (label) => page.locator(`vaadin-text-field, vaadin-email-field`).filter({ hasText: label }).locator('input');
	await feld('Ihr Name').fill(name);
	await feld('Name des Kindes').fill(kind);
	await feld('Ihre E-Mail-Adresse').fill(email);
	await feld('E-Mail-Adresse wiederholen').fill(email);
	await page.locator('vaadin-select').click();
	await page.locator('vaadin-select-item[role=option]').filter({ hasText: klasse }).last().click();
	await bereit(page);

	for (const [i, { lehrkraft, notiz }] of termine.entries()) {
		const eintrag = page.locator('.elternsprechtag-view__lehrkraft-item').filter({ hasText: lehrkraft });
		await eintrag.locator('.elternsprechtag-view__lehrkraft').click();
		await bereit(page);
		await eintrag.locator('.elternsprechtag-view__slot:not(.elternsprechtag-view__slot--belegt)').nth(i).click();
		await bereit(page);
		if (notiz) {
			await eintrag.locator('.elternsprechtag-view__notiz-field textarea').fill(notiz);
			await eintrag.locator('.elternsprechtag-view__notiz-field textarea').blur();
		}
	}
	await page.keyboard.press('Tab');
	await bereit(page);
}
