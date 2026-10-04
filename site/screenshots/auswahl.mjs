// Welche Screenshot-Module ein Lauf ausführt. Rein und ohne Dateisystem, damit es per plain
// `node --test` prüfbar bleibt; der Runner liest die Verzeichnisse und reicht die Slugs herein.

/**
 * @param {{ module: string[], seiten: string[], angefragt: string[] }} stand
 *   `module`: Slugs der Module in `faelle/`, `seiten`: Slugs der Fallseiten, `angefragt`: Slugs
 *   von der Kommandozeile (leer = alle).
 * @returns {string[]} die auszuführenden Module
 */
export function waehleModule({ module, seiten, angefragt }) {
	// Verwaiste Module brechen jeden Lauf ab, auch einen für ein anderes Modul: Sonst fiele eine
	// umbenannte oder gelöschte Fallseite erst im vollen Lauf in CI auf.
	const verwaist = module.filter((slug) => !seiten.includes(slug));
	if (verwaist.length > 0) {
		throw new Error(
			'Verwaiste Screenshot-Module — zu jedem Modul gehört eine Fallseite:\n' +
				verwaist
					.map((slug) => `  - screenshots/faelle/${slug}.mjs hat keine src/content/docs/anwendungsfaelle/${slug}.mdx`)
					.join('\n'),
		);
	}

	if (angefragt.length === 0) return module;
	const unbekannt = angefragt.filter((slug) => !module.includes(slug));
	if (unbekannt.length > 0) {
		throw new Error(
			`Kein Screenshot-Modul für ${unbekannt.join(', ')}. Vorhanden: ${module.join(', ') || '(keine)'}`,
		);
	}
	return angefragt;
}
