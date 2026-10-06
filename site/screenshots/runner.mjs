#!/usr/bin/env node
/**
 * Erzeugt die Screenshots und Mailtexte der Site aus einer laufenden `demo`-Instanz.
 *
 *   npm run screenshots                 alle Module
 *   npm run screenshots -- <slug> ...   nur diese
 *
 * Je Seite höchstens ein Modul: `faelle/<slug>.mjs` für eine Fallseite, `referenz/<slug>.mjs` für
 * eine Seite der Referenz. Was es erzeugt, landet unter `src/assets/screenshots/<slug>/` (gitignored)
 * und wird vor jedem Lauf des Moduls geleert.
 * Jedes Modul läuft in einem eigenen, frisch angemeldeten Browser-Kontext — die Reihenfolge ist
 * also beliebig. Zerstörerische Abläufe fotografiert ein Modul nur bis zum offenen Dialog, damit
 * der Demo-Seed unverändert bleibt. Wer etwas auslösen muss — die Mails —, tut es an einem eigenen
 * Sprechtag (`eigener-sprechtag.mjs`), den kein anderes Modul anfasst.
 *
 * Umgebung: ORGANIZER_PASSWORD (Pflicht, wird nie ausgegeben), ORGANIZER_USERNAME (Default
 * `user`), BASE_URL (Default `http://localhost:8080`), MAIL_ABLAGE (die Mail-Ablage der App, nur
 * für Module, die Mails abgreifen).
 */
import { chromium } from 'playwright-core';
import { readdir, rm } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { BEREICHE, waehleModule } from './auswahl.mjs';
import { FENSTER, ablage, anmelden, aufnahme, bereit, postfach, rahmen } from './werkzeug.mjs';

const SITE = path.resolve(import.meta.dirname, '..');
const SEITEN = path.join(SITE, 'src/content/docs');
const ABLAGE = path.join(SITE, 'src/assets/screenshots');

const basis = (process.env.BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const benutzer = process.env.ORGANIZER_USERNAME ?? 'user';
const passwort = process.env.ORGANIZER_PASSWORD;
const mails = postfach(process.env.MAIL_ABLAGE);

/** Die Dateien eines Verzeichnisses als `<präfix>/<slug>`, ohne Überblicksseite `index`. */
const pfade = async (verzeichnis, praefix, endung) =>
	(await readdir(verzeichnis).catch(() => []))
		.filter((datei) => endung.test(datei) && !datei.startsWith('index.'))
		.map((datei) => `${praefix}/${datei.replace(endung, '')}`);
const alle = async (liste) => (await Promise.all(liste)).flat();

let auswahl;
try {
	auswahl = waehleModule({
		module: await alle(Object.keys(BEREICHE).map((modul) => pfade(path.join(import.meta.dirname, modul), modul, /\.mjs$/))),
		seiten: await alle(Object.values(BEREICHE).map((bereich) => pfade(path.join(SEITEN, bereich), bereich, /\.mdx?$/))),
		angefragt: process.argv.slice(2),
	});
} catch (fehler) {
	abbrechen(fehler.message);
}
if (!passwort) {
	abbrechen(
		'ORGANIZER_PASSWORD ist nicht gesetzt — ohne Anmeldung sind die Organisator-Seiten nicht erreichbar.\n' +
			"PowerShell:  $env:ORGANIZER_PASSWORD = '...'\n" +
			'bash:        export ORGANIZER_PASSWORD=...',
	);
}

const browser = await chromium.launch({ channel: 'chrome' });
let gescheitert = 0;
try {
	for (const modulpfad of auswahl) {
		const slug = path.basename(modulpfad);
		const ziel = path.join(ABLAGE, slug);
		await rm(ziel, { recursive: true, force: true });
		const kontext = await browser.newContext(FENSTER);
		try {
			const page = await kontext.newPage();
			await anmelden(page, { basis, benutzer, passwort });
			const { default: modul } = await import(pathToFileURL(path.join(import.meta.dirname, `${modulpfad}.mjs`)).href);
			await modul({ page, basis, foto: aufnahme(ziel), ablegen: ablage(ziel), rahmen, bereit, mails });
			console.log(`✔ ${slug}`);
		} catch (fehler) {
			gescheitert++;
			console.error(`✖ ${slug}: ${fehler.message}`);
		} finally {
			await kontext.close();
		}
	}
} finally {
	await browser.close();
}
if (gescheitert > 0) abbrechen(`${gescheitert} von ${auswahl.length} Screenshot-Modulen gescheitert.`);

function abbrechen(meldung) {
	console.error(meldung);
	process.exit(1);
}
