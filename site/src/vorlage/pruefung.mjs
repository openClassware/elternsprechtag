// Build-Prüfung der Vorlage als Sätteri-mdast-Plugin: Ein Verstoß lässt den Build scheitern, lokal
// wie in CI. Fallseiten unter `anwendungsfaelle/` prüft es gegen die feste Abschnittsfolge, auf der
// ganzen Site prüft es die Alt-Texte. Alle Verstöße einer Datei stehen in einer Meldung.
import { readdirSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { markdownToHtml } from 'satteri';

/** Die erlaubten H2 einer Fallseite, wörtlich und in fester Reihenfolge. */
export const ABSCHNITTE = [
	{ titel: 'Ist das der richtige Fall?', pflicht: false },
	{ titel: 'Schritte', pflicht: true },
	{ titel: 'Was danach passiert', pflicht: true },
	{ titel: 'Regeln', pflicht: false },
	{ titel: 'Wenn …', pflicht: false },
	{ titel: 'Wege drumherum', pflicht: false },
	{ titel: 'Verwandte Fälle', pflicht: true },
];

const INHALTSVERZEICHNIS = 'src/content/docs/';
const FALLSEITEN = `${INHALTSVERZEICHNIS}anwendungsfaelle/`;

/** Alt-Texte, die nichts beschreiben und deshalb als leer gelten. */
const NICHTSSAGEND = new Set(['', 'screenshot', 'bild']);

/** Das Plugin für `satteri({ mdastPlugins })`: wirft je Datei mit allen ihren Verstößen. */
export function vorlagenPruefung() {
	return ({ fileURL }) => {
		if (!fileURL) return;
		const pfad = fileURLToPath(fileURL).replaceAll('\\', '/');
		return {
			name: 'vorlagen-pruefung',
			options: { position: true },
			before(root, ctx) {
				const verstoesse = [...pruefeAltTexte(root)];
				if (istFallseite(pfad)) verstoesse.push(...pruefeAbschnitte(root));
				if (verstoesse.length === 0) return;
				const versatz = frontmatterZeilen(pfad, ctx.source);
				throw new Error(
					`${anzeigename(pfad)} verletzt die Vorlage der Doku-Site (siehe site/README.md):\n` +
						verstoesse
							.map((verstoss) => verstoss.replace(/^Zeile (\d+)/, (_, nummer) => `Zeile ${Number(nummer) + versatz}`))
							.map((verstoss) => `  - ${verstoss}`)
							.join('\n'),
				);
			},
		};
	};
}

/**
 * Prüft vor jedem Build alle `.md`-Seiten selbst. Im Astro-Build reicht das Plugin dafür nicht:
 * Starlights Loader rendert `.md` schon beim Laden, meldet einen Fehler dort nur, und lässt
 * unveränderte Dateien im nächsten Build ganz aus. `.mdx` läuft jedes Mal durch das Plugin.
 */
export function vorlagenPruefungMarkdown() {
	let inhalt;
	return {
		name: 'vorlagen-pruefung-markdown',
		hooks: {
			'astro:config:setup': ({ config }) => {
				inhalt = new URL('content/docs/', config.srcDir);
			},
			'astro:build:start': () => {
				const meldungen = [];
				for (const datei of readdirSync(inhalt, { recursive: true })) {
					if (!datei.endsWith('.md')) continue;
					const fileURL = new URL(datei.replaceAll('\\', '/'), inhalt);
					try {
						markdownToHtml(readFileSync(fileURL, 'utf8'), { mdastPlugins: [vorlagenPruefung()], fileURL });
					} catch (fehler) {
						meldungen.push(fehler.message);
					}
				}
				if (meldungen.length > 0) throw new Error(meldungen.join('\n\n'));
			},
		},
	};
}

/**
 * Bei `.md` bekommt das Plugin den Text ohne Frontmatter. Damit die Zeilenangaben in der Datei
 * stimmen, werden dessen Zeilen dazugezählt.
 */
function frontmatterZeilen(pfad, quelle) {
	try {
		const datei = readFileSync(pfad, 'utf8').replaceAll('\r\n', '\n');
		const text = quelle.replaceAll('\r\n', '\n');
		const kern = text.trim();
		const inDatei = datei.lastIndexOf(kern);
		if (!kern || inDatei < 0) return 0;
		return zeilenVor(datei, inDatei) - zeilenVor(text, text.indexOf(kern));
	} catch {
		return 0;
	}
}

function zeilenVor(text, stelle) {
	return text.slice(0, stelle).split('\n').length - 1;
}

function istFallseite(pfad) {
	const stelle = pfad.lastIndexOf(FALLSEITEN);
	return stelle >= 0 && !/^index\.mdx?$/.test(pfad.slice(stelle + FALLSEITEN.length));
}

function anzeigename(pfad) {
	const stelle = pfad.lastIndexOf(INHALTSVERZEICHNIS);
	return stelle >= 0 ? pfad.slice(stelle) : pfad;
}

/** Die Regeln für die Abschnitte einer Fallseite. */
export function pruefeAbschnitte(root) {
	const verstoesse = [];
	const abschnitte = teileNach(root.children, 2);
	let letzter;

	for (const { ueberschrift, inhalt } of abschnitte) {
		if (!ueberschrift) continue; // Einleitung vor der ersten H2
		const titel = textVon(ueberschrift);
		const stelle = ABSCHNITTE.findIndex((abschnitt) => abschnitt.titel === titel);
		const ort = zeile(ueberschrift);

		if (stelle < 0) {
			verstoesse.push(
				`${ort}Abschnitt „${titel}“ ist in der Vorlage nicht vorgesehen. Erlaubt sind nur: ` +
					ABSCHNITTE.map((abschnitt) => `„${abschnitt.titel}“`).join(', '),
			);
			continue;
		}
		if (letzter && stelle <= letzter.stelle) {
			verstoesse.push(`${ort}„${titel}“ muss vor „${letzter.titel}“ stehen (feste Reihenfolge der Vorlage)`);
		}
		letzter = { stelle, titel };

		if (inhalt.length === 0) {
			verstoesse.push(`${ort}Abschnitt „${titel}“ ist leer. Ein optionaler Abschnitt entfällt dann ganz`);
			continue;
		}
		if (titel === 'Schritte' && !enthaelt(inhalt, istSteps)) {
			verstoesse.push(`${ort}„Schritte“ braucht eine <Steps>-Liste`);
		}
		if (titel === 'Wenn …') verstoesse.push(...pruefeWenn(inhalt, ort));
	}

	const vorhanden = new Set(abschnitte.filter((a) => a.ueberschrift).map((a) => textVon(a.ueberschrift)));
	for (const { titel, pflicht } of ABSCHNITTE) {
		if (pflicht && !vorhanden.has(titel)) verstoesse.push(`Pflichtabschnitt „${titel}“ fehlt`);
	}
	return verstoesse;
}

/** Unter „Wenn …“ steht je Variante ein `### … <Bedingung>`, mindestens einer. */
function pruefeWenn(inhalt, ort) {
	const verstoesse = [];
	const faelle = teileNach(inhalt, 3).filter((teil) => teil.ueberschrift);
	if (faelle.length === 0) {
		verstoesse.push(`${ort}„Wenn …“ braucht mindestens eine ###-Überschrift der Form „… <Bedingung>“`);
	}
	for (const knoten of inhalt) {
		if (knoten.type === 'heading' && knoten.depth !== 3) {
			verstoesse.push(`${zeile(knoten)}„${textVon(knoten)}“ unter „Wenn …“ muss eine ###-Überschrift sein`);
		}
	}
	for (const { ueberschrift, inhalt: rumpf } of faelle) {
		const titel = textVon(ueberschrift);
		if (!titel.startsWith('…')) {
			verstoesse.push(`${zeile(ueberschrift)}„${titel}“ unter „Wenn …“ muss mit „…“ beginnen`);
		}
		if (rumpf.length === 0) verstoesse.push(`${zeile(ueberschrift)}„${titel}“ unter „Wenn …“ ist leer`);
	}
	return verstoesse;
}

/** Jedes Bild der Site hat einen Alt-Text, der etwas beschreibt. */
export function pruefeAltTexte(root) {
	const verstoesse = [];
	besuche(root, (knoten) => {
		for (const { quelle, alt } of bilderIn(knoten)) {
			if (alt !== undefined && NICHTSSAGEND.has(alt.trim().toLowerCase())) {
				verstoesse.push(
					`${zeile(knoten)}Bild „${quelle}“ hat keinen Alt-Text. „Screenshot“ oder „Bild“ allein ` +
						'zählen als leer — beschreiben Sie, was zu sehen ist',
				);
			}
		}
	});
	return verstoesse;
}

/**
 * Die Bilder eines Knotens mit ihrem Alt-Text. Fehlt das Attribut, ist er leer (`''`); ein
 * JSX-Ausdruck (`alt={…}`) lässt sich nicht auswerten und bleibt `undefined`.
 */
function bilderIn(knoten) {
	switch (knoten.type) {
		case 'image':
		case 'imageReference':
			return [{ quelle: knoten.url ?? knoten.identifier, alt: knoten.alt ?? '' }];
		case 'mdxJsxFlowElement':
		case 'mdxJsxTextElement': {
			if (!['img', 'Image', 'Picture'].includes(knoten.name)) return [];
			const attribut = (name) => knoten.attributes.find((a) => a.type === 'mdxJsxAttribute' && a.name === name);
			const quelle = attribut('src')?.value;
			const alt = attribut('alt');
			return [
				{
					quelle: typeof quelle === 'string' ? quelle : `{${quelle?.value ?? ''}}`,
					alt: alt === undefined ? '' : typeof alt.value === 'string' ? alt.value : alt.value == null ? '' : undefined,
				},
			];
		}
		case 'html':
			return [...knoten.value.matchAll(/<img\b[^>]*>/gi)].map(([tag]) => ({
				// `\s` davor statt `\b`: sonst träfe `data-alt=` als Alt-Text.
				quelle: /\ssrc\s*=\s*["']([^"']*)["']/i.exec(tag)?.[1] ?? '',
				alt: /\salt\s*=\s*["']([^"']*)["']/i.exec(tag)?.[1] ?? '',
			}));
		default:
			return [];
	}
}

/** Teilt eine Knotenfolge an den Überschriften einer Ebene; was davor steht, hat keine Überschrift. */
function teileNach(knoten, ebene) {
	const teile = [{ ueberschrift: undefined, inhalt: [] }];
	for (const kind of knoten) {
		if (kind.type === 'heading' && kind.depth === ebene) teile.push({ ueberschrift: kind, inhalt: [] });
		else teile.at(-1).inhalt.push(kind);
	}
	return teile;
}

function istSteps(knoten) {
	return knoten.type === 'mdxJsxFlowElement' && knoten.name === 'Steps';
}

function enthaelt(knoten, bedingung) {
	return knoten.some((kind) => bedingung(kind) || enthaelt(kind.children ?? [], bedingung));
}

function besuche(knoten, besucher) {
	besucher(knoten);
	for (const kind of knoten.children ?? []) besuche(kind, besucher);
}

function textVon(knoten) {
	if ('value' in knoten && typeof knoten.value === 'string') return knoten.value;
	return (knoten.children ?? []).map(textVon).join('').trim();
}

function zeile(knoten) {
	const nummer = knoten.position?.start?.line;
	return nummer ? `Zeile ${nummer}: ` : '';
}
