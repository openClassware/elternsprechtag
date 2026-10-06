// Welche E-Mails Eltern bekommen: Jede Mail wird an einem eigenen Sprechtag ausgelöst und ihr Text
// aus der Mail-Ablage der App abgegriffen, nicht abgeschrieben. Ändert sich ein Text in der App,
// ändert er sich mit dem nächsten Lauf auch auf der Seite. Abgelegt wird je Mail eine Textdatei, die
// die Seite einbindet; fehlt eine, bricht der Build ab.
//
// Die Erinnerung kommt vom Erinnerungs-Scheduler, nicht von einem Klick. Damit sie im Lauf kommt,
// liegt der Sprechtag genau so viele Tage voraus, wie die Erinnerung vorher läuft, und die App
// läuft mit einem Takt von Sekunden statt einmal am Morgen (ELTERNSPRECHTAG_ERINNERUNG_CRON).
import { alsTextblock } from '../mails.mjs';
import { bucheAlsFamilie, legeSprechtagAn, sageAb } from '../eigener-sprechtag.mjs';

const FAMILIE = {
	name: 'Anna Müller',
	kind: 'Lukas Müller',
	email: 'anna.mueller@example.org',
	klasse: '8a',
	// Drei Lehrkräfte: Eine Buchung wird umgebucht, zwei entfallen nacheinander — einmal vor und
	// einmal nach dem Anmeldeschluss —, und für die Absage muss danach noch eine gelten.
	termine: [
		{ lehrkraft: 'Julia Schmidt', notiz: 'Leseförderung besprechen' },
		{ lehrkraft: 'Stefan Koch' },
		{ lehrkraft: 'Laura Fischer' },
	],
};

export default async function (werkzeug) {
	const { page, basis, bereit, ablegen, mails } = werkzeug;
	const sichere = async (name, mail) => ablegen(`${name}.txt`, alsTextblock(mail));

	const sprechtag = await legeSprechtagAn(page, werkzeug, {
		titel: 'Elternsprechtag Frühjahr',
		inTagen: 2,
		erinnerung: 2,
		anmeldefrist: 1,
	});
	const zurAuswertung = async () => {
		await page.goto(`${basis}/auswertung/${sprechtag.id}`, { waitUntil: 'networkidle' });
		await bereit(page);
	};
	const lehrkraft = (name) => page.locator('.auswertung__section').filter({ hasText: name });

	// Bestätigung und — mit dem nächsten Lauf des Schedulers — Erinnerung.
	let stand = await mails.stand();
	await bucheAlsFamilie(page, werkzeug, sprechtag.link, FAMILIE);
	await sichere('bestaetigung', await mails.warte(stand, /^Ihre Termine/));
	await sichere(
		'erinnerung',
		await mails.warte(stand, /^Erinnerung/, { timeout: 60_000 }).catch((fehler) => {
			throw new Error(`${fehler.message}\nLäuft die App mit ELTERNSPRECHTAG_ERINNERUNG_CRON='*/5 * * * * *'? Siehe site/README.md.`);
		}),
	);

	// Umbuchen: dieselbe Lehrkraft, ein anderer freier Termin.
	await zurAuswertung();
	await lehrkraft('Julia Schmidt').locator('.auswertung__row').filter({ hasText: FAMILIE.kind }).locator('.auswertung__umbuchen').click();
	const umbuchen = page.locator('vaadin-dialog.umbuchen-dialog');
	await umbuchen.locator('.umbuchen-dialog__auswahl input').click();
	await page.locator('vaadin-combo-box-item').nth(2).click();
	stand = await mails.stand();
	await umbuchen.getByRole('button', { name: 'Umbuchen' }).click();
	await sichere('umbuchung', await mails.warte(stand, /^Ihre Termine/));

	// Eine Lehrkraft fällt aus — solange die Anmeldung läuft, mit dem Link zurück in die Buchung …
	await lasseEntfallen('Stefan Koch', 'ausfall');

	// … und nach dem Anmeldeschluss ohne ihn. Die längere Frist schließt die Anmeldung sofort.
	await page.goto(`${basis}/sprechtag/${sprechtag.id}`, { waitUntil: 'networkidle' });
	await bereit(page);
	await page.getByLabel('Anmeldefrist (Tage vor dem Sprechtag)', { exact: true }).fill('3');
	await page.getByLabel('Anmeldefrist (Tage vor dem Sprechtag)', { exact: true }).blur();
	await bereit(page);
	await page.getByRole('button', { name: 'Speichern' }).click();
	await page.waitForURL((url) => !url.pathname.startsWith('/sprechtag/'));
	await lasseEntfallen('Laura Fischer', 'ausfall-nach-anmeldeschluss');

	// Die Absage trifft die eine Buchung, die noch gilt.
	stand = await mails.stand();
	await sageAb(page, werkzeug, sprechtag);
	await sichere('absage', await mails.warte(stand, /abgesagt$/));

	async function lasseEntfallen(name, datei) {
		await zurAuswertung();
		await lehrkraft(name).locator('.auswertung__ausfall-button').click();
		const dialog = page.locator('vaadin-dialog.ausfall-dialog');
		await dialog.locator('.ausfall-dialog__row').filter({ hasText: 'gebucht' }).first().locator('vaadin-checkbox').click();
		await bereit(page);
		stand = await mails.stand();
		await dialog.getByRole('button', { name: 'Termine entfallen lassen' }).click();
		await sichere(datei, await mails.warte(stand, /Termin entfällt$/));
	}
}
