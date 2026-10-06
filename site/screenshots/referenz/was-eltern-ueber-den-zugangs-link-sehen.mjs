// Was Eltern über den Zugangs-Link sehen: die Buchungsseite, ausgefüllt bis kurz vor dem Abschicken,
// die Bestätigung danach und die Hinweisseiten. Die Buchungsseite und die Hinweisseiten kommen aus
// den Sprechtagen des Demo-Seeds, deren Zugangs-Links sprechend sind (`demo-aktiv` …) — dort wird
// nichts abgeschickt. Die Bestätigung braucht eine echte Buchung, die entsteht an einem eigenen
// Sprechtag, der am Ende abgesagt wird.
import { EIGENER_SPRECHTAG, FAMILIE, bucheAlsFamilie, fuelleAus, legeSprechtagAn, sageAb } from '../eigener-sprechtag.mjs';


export default async function (werkzeug) {
	const { page, basis, foto, bereit } = werkzeug;
	const elternlink = async (token) => {
		await page.goto(`${basis}/elternsprechtag/${token}`, { waitUntil: 'networkidle' });
		await bereit(page);
	};

	// Die Buchungsseite: Kopf mit Datum und Anmeldeschluss, dann die gewählten Termine.
	await elternlink('demo-aktiv');
	await foto(page.locator('.elternsprechtag-view__kopf'), 'kopf');
	await fuelleAus(page, werkzeug, {
		...FAMILIE,
		klasse: '5a',
		termine: [{ lehrkraft: 'Anna Krause', notiz: 'Leseförderung besprechen' }, { lehrkraft: 'Michael Kern' }],
	});
	await foto(page.locator('.elternsprechtag-view__section').nth(1), 'termine');
	await foto(page.locator('.elternsprechtag-view__section').nth(2), 'auswahl');
	await foto(page.locator('.elternsprechtag-view__footer'), 'buchen');

	// Die Hinweisseiten, je eine je Zustand des Sprechtags.
	for (const [token, name] of [
		['demo-anmeldung-beendet', 'anmeldung-beendet'],
		['demo-abgeschlossen', 'vorbei'],
		['demo-abgesagt', 'abgesagt'],
	]) {
		await elternlink(token);
		await foto(page.locator('.elternsprechtag-view__card'), name);
	}
	await elternlink('demo-entwurf');
	await foto(page.locator('.elternsprechtag-view__message'), 'nicht-verfuegbar');

	// Die Bestätigung nach dem Buchen, an einem eigenen Sprechtag.
	const sprechtag = await legeSprechtagAn(page, werkzeug, {
		titel: EIGENER_SPRECHTAG,
		inTagen: 14,
		erinnerung: 1,
		anmeldefrist: 1,
	});
	await bucheAlsFamilie(page, werkzeug, sprechtag.link, {
		...FAMILIE,
		klasse: '8a',
		termine: [{ lehrkraft: 'Julia Schmidt' }, { lehrkraft: 'Stefan Koch' }],
	});
	await foto(page.locator('.elternsprechtag-view__card'), 'bestaetigung');
	await sageAb(page, werkzeug, sprechtag);
}
