// Welche Screenshot-Module ein Lauf ausführt. Rein und ohne Dateisystem, damit es per plain
// `node --test` prüfbar bleibt; der Runner liest die Verzeichnisse und reicht die Pfade herein.

/**
 * Welches Modulverzeichnis zu welchem Bereich der Site gehört: `faelle/<slug>.mjs` fotografiert
 * `anwendungsfaelle/<slug>.mdx`, `referenz/<slug>.mjs` die gleichnamige Seite der Referenz.
 */
export const BEREICHE = { faelle: 'anwendungsfaelle', referenz: 'referenz' };

const slug = (pfad) => pfad.slice(pfad.indexOf('/') + 1);
const seiteZu = (modul) => `${BEREICHE[modul.slice(0, modul.indexOf('/'))]}/${slug(modul)}`;

/**
 * @param {{ module: string[], seiten: string[], angefragt: string[] }} stand
 *   `module`: Module als `<verzeichnis>/<slug>` (etwa `faelle/einen-sprechtag-absagen`),
 *   `seiten`: Seiten als `<bereich>/<slug>`, `angefragt`: Slugs von der Kommandozeile (leer = alle).
 * @returns {string[]} die auszuführenden Module als `<verzeichnis>/<slug>`
 */
export function waehleModule({ module, seiten, angefragt }) {
	// Verwaiste Module brechen jeden Lauf ab, auch einen für ein anderes Modul: Sonst fiele eine
	// umbenannte oder gelöschte Seite erst im vollen Lauf in CI auf.
	const verwaist = module.filter((modul) => !seiten.includes(seiteZu(modul)));
	if (verwaist.length > 0) {
		throw new Error(
			'Verwaiste Screenshot-Module — zu jedem Modul gehört eine Seite:\n' +
				verwaist
					.map((modul) => `  - screenshots/${modul}.mjs hat keine src/content/docs/${seiteZu(modul)}.mdx`)
					.join('\n'),
		);
	}

	if (angefragt.length === 0) return module;
	const unbekannt = angefragt.filter((gesucht) => !module.some((modul) => slug(modul) === gesucht));
	if (unbekannt.length > 0) {
		throw new Error(
			`Kein Screenshot-Modul für ${unbekannt.join(', ')}. Vorhanden: ${module.map(slug).join(', ') || '(keine)'}`,
		);
	}
	return angefragt.map((gesucht) => module.find((modul) => slug(modul) === gesucht));
}
