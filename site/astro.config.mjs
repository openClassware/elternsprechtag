// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightLinksValidator from 'starlight-links-validator';
import { satteri } from '@astrojs/markdown-satteri';
import { vorlagenPruefung, vorlagenPruefungMarkdown } from './src/vorlage/pruefung.mjs';

// Ohne Basis-Pfad: die Site liegt auf eigener Subdomain. Beim Deploy über Actions wirkt die
// Custom Domain aus den Pages-Einstellungen; public/CNAME hält sie nur im Repo fest.
export default defineConfig({
	site: 'https://docs.openclassware.de',
	// Die Vorlage der Fallseiten und die Alt-Texte der ganzen Site als Build-Prüfung: Ein Verstoß
	// lässt den Build scheitern. Starlight hängt seine eigenen Plugins an denselben Prozessor an.
	markdown: {
		processor: satteri({ mdastPlugins: [vorlagenPruefung()] }),
	},
	integrations: [
		starlight({
			title: 'Elternsprechtag',
			// Nur eine Sprache, als Root-Locale: keine /de/-Präfixe in den URLs. `lang` steht als
			// <html lang="de"> in jeder Seite, daraus wählt Pagefind die deutsche Stammformbildung
			// („Lehrkräfte“ trifft *Lehrkraft*).
			defaultLocale: 'root',
			locales: {
				root: { label: 'Deutsch', lang: 'de' },
			},
			social: [
				{ icon: 'github', label: 'GitHub', href: 'https://github.com/openClassware/elternsprechtag' },
			],
			components: {
				PageTitle: './src/components/PageTitle.astro',
			},
			// Die fünf Bereiche nach Dokumentart, in fester Reihenfolge. Seiten eines Bereichs
			// kommen von selbst hinzu, sobald sie in seinem Verzeichnis liegen.
			sidebar: [
				{ label: 'Einstieg', items: [{ autogenerate: { directory: 'einstieg' } }] },
				{ label: 'Anwendungsfälle', items: [{ autogenerate: { directory: 'anwendungsfaelle' } }] },
				{ label: 'Betrieb', items: [{ autogenerate: { directory: 'betrieb' } }] },
				{ label: 'Datenschutz', items: [{ autogenerate: { directory: 'datenschutz' } }] },
				{ label: 'Referenz', items: [{ autogenerate: { directory: 'referenz' } }] },
			],
			// Ein toter interner Link lässt den Build scheitern, lokal wie in CI.
			plugins: [starlightLinksValidator()],
		}),
		vorlagenPruefungMarkdown(),
	],
});
