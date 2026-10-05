// Das Tagesplan-Blatt als Bild: Die Auswertung liefert ein ZIP mit einem PDF je Lehrkraft. Daraus
// wird das Blatt einer Lehrkraft gezogen und seine erste Seite im Browser mit pdf.js gezeichnet —
// so braucht es kein natives Canvas in Node, nur das Chrome, das ohnehin läuft.
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { unzipSync } from 'fflate';

/**
 * Das PDF der Lehrkraft aus dem ZIP der Tagespläne. Das Kürzel steht am Ende des Dateinamens
 * (`<Stand>_<Sprechtag>_<Kürzel>.pdf`); fehlt das Blatt, nennt der Fehler, was im ZIP liegt.
 *
 * @param {Uint8Array} zip
 * @param {string} kuerzel
 * @returns {Uint8Array}
 */
export function blattAusZip(zip, kuerzel) {
	const dateien = unzipSync(zip);
	const name = Object.keys(dateien).find((datei) => datei.endsWith(`_${kuerzel}.pdf`));
	if (!name) {
		throw new Error(`Kein Blatt *_${kuerzel}.pdf im ZIP. Darin liegen:\n  ${Object.keys(dateien).join('\n  ')}`);
	}
	return dateien[name];
}

/** Eine Herkunft, die nie ins Netz geht: Jede Anfrage dorthin beantwortet die Route unten. */
const HERKUNFT = 'http://tagesplan.invalid';

const SEITE = `<!doctype html>
<meta charset="utf-8">
<style>
	body { margin: 0; padding: 16px; background: #fff; }
	.blatt { display: inline-block; line-height: 0; border: 1px solid #c8c8c8; }
</style>
<div class="blatt"><canvas></canvas></div>
<script type="module">
	import * as pdfjs from './pdf.mjs';
	pdfjs.GlobalWorkerOptions.workerSrc = './pdf.worker.mjs';
	try {
		const dokument = await pdfjs.getDocument({ url: './blatt.pdf' }).promise;
		const seite = await dokument.getPage(1);
		// Im Layout so groß wie das Blatt in Punkt, gezeichnet in der Pixeldichte des Fensters.
		const ansicht = seite.getViewport({ scale: 1 });
		const canvas = document.querySelector('canvas');
		const dichte = window.devicePixelRatio;
		canvas.width = Math.round(ansicht.width * dichte);
		canvas.height = Math.round(ansicht.height * dichte);
		canvas.style.width = ansicht.width + 'px';
		canvas.style.height = ansicht.height + 'px';
		await seite.render({ canvasContext: canvas.getContext('2d'), viewport: seite.getViewport({ scale: dichte }) }).promise;
		document.body.dataset.stand = 'fertig';
	} catch (fehler) {
		document.body.dataset.stand = 'Fehler: ' + fehler;
	}
</script>`;

const PDFJS = (datei) => fileURLToPath(import.meta.resolve(`pdfjs-dist/build/${datei}`));

/**
 * Zeichnet die erste Seite des PDFs in einer eigenen Seite des Kontexts und fotografiert sie als
 * `<name>.png`. Scheitert pdf.js, scheitert der Lauf mit seiner Meldung.
 */
export async function fotografiereErsteSeite({ kontext, pdf, foto, name }) {
	const antworten = {
		'/blatt.html': { body: SEITE, contentType: 'text/html' },
		'/blatt.pdf': { body: Buffer.from(pdf), contentType: 'application/pdf' },
		'/pdf.mjs': { body: await readFile(PDFJS('pdf.min.mjs')), contentType: 'text/javascript' },
		'/pdf.worker.mjs': { body: await readFile(PDFJS('pdf.worker.min.mjs')), contentType: 'text/javascript' },
	};
	const seite = await kontext.newPage();
	try {
		await seite.route(`${HERKUNFT}/**`, (route) => {
			const antwort = antworten[new URL(route.request().url()).pathname];
			return antwort ? route.fulfill({ status: 200, ...antwort }) : route.fulfill({ status: 404 });
		});
		await seite.goto(`${HERKUNFT}/blatt.html`);
		const stand = await seite.waitForFunction(() => document.body.dataset.stand, null, { timeout: 15_000 });
		const ergebnis = await stand.jsonValue();
		if (ergebnis !== 'fertig') throw new Error(`pdf.js konnte das Blatt nicht zeichnen. ${ergebnis}`);
		await foto(seite.locator('.blatt'), name);
	} finally {
		await seite.close();
	}
}
