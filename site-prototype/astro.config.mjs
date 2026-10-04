// @ts-check
// PROTOTYP — Wegwerf-Code für das Ticket „Vorlage einer Use-Case-Seite“ (#195).
// Drei Fassungen derselben Fallseite, umschaltbar über die Leiste unten oder ←/→.
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
	integrations: [
		starlight({
			title: 'Elternsprechtag',
			defaultLocale: 'root',
			locales: { root: { label: 'Deutsch', lang: 'de' } },
			sidebar: [
				{
					label: 'Anwendungsfälle',
					items: [
						{ label: 'Katalog', slug: 'anwendungsfaelle' },
						{ label: 'Vorlage: Einen Sprechtag absagen', slug: 'anwendungsfaelle/einen-sprechtag-absagen' },
						{
							label: 'Kurz vor dem Termin',
							items: [
								{ label: 'Einen Sprechtag absagen — A', slug: 'anwendungsfaelle/einen-sprechtag-absagen-a' },
								{ label: 'Einen Sprechtag absagen — B', slug: 'anwendungsfaelle/einen-sprechtag-absagen-b' },
								{ label: 'Einen Sprechtag absagen — C', slug: 'anwendungsfaelle/einen-sprechtag-absagen-c' },
							],
						},
					],
				},
			],
		}),
	],
});
