#!/usr/bin/env node
/**
 * Erzeugt die Screenshots der Fallseiten aus einer laufenden `demo`-Instanz.
 *
 *   npm run screenshots                 alle Module
 *   npm run screenshots -- <slug> ...   nur diese
 *
 * Je Fallseite höchstens ein Modul `faelle/<slug>.mjs`; seine Bilder landen unter
 * `src/assets/screenshots/<slug>/` (gitignored) und werden vor jedem Lauf des Moduls geleert.
 * Jedes Modul läuft in einem eigenen, frisch angemeldeten Browser-Kontext — die Reihenfolge ist
 * also beliebig. Zerstörerische Abläufe fotografiert ein Modul nur bis zum offenen Dialog, damit
 * der Demo-Seed unverändert bleibt.
 *
 * Umgebung: ORGANIZER_PASSWORD (Pflicht, wird nie ausgegeben), ORGANIZER_USERNAME (Default
 * `user`), BASE_URL (Default `http://localhost:8080`).
 */
import { chromium } from 'playwright-core';
import { readdir, rm } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { waehleModule } from './auswahl.mjs';
import { FENSTER, ablage, anmelden, aufnahme, bereit, rahmen } from './werkzeug.mjs';

const SITE = path.resolve(import.meta.dirname, '..');
const MODULE = path.join(import.meta.dirname, 'faelle');
const SEITEN = path.join(SITE, 'src/content/docs/anwendungsfaelle');
const ABLAGE = path.join(SITE, 'src/assets/screenshots');

const basis = (process.env.BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const benutzer = process.env.ORGANIZER_USERNAME ?? 'user';
const passwort = process.env.ORGANIZER_PASSWORD;

const slugs = async (verzeichnis, endung) =>
	(await readdir(verzeichnis))
		.filter((datei) => datei.endsWith(endung) && !datei.startsWith('index.'))
		.map((datei) => datei.slice(0, -endung.length));

let auswahl;
try {
	auswahl = waehleModule({
		module: await slugs(MODULE, '.mjs'),
		seiten: await slugs(SEITEN, '.mdx'),
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
	for (const slug of auswahl) {
		const ziel = path.join(ABLAGE, slug);
		await rm(ziel, { recursive: true, force: true });
		const kontext = await browser.newContext(FENSTER);
		try {
			const page = await kontext.newPage();
			await anmelden(page, { basis, benutzer, passwort });
			const { default: modul } = await import(pathToFileURL(path.join(MODULE, `${slug}.mjs`)).href);
			await modul({ page, basis, foto: aufnahme(ziel), ablegen: ablage(ziel), rahmen, bereit });
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
