import { defineCollection } from 'astro:content';
import { glob } from 'astro/loaders';
import { docsLoader } from '@astrojs/starlight/loaders';
import { docsSchema } from '@astrojs/starlight/schema';
import { fallseiteSchema } from './vorlage/schema.mjs';

export const collections = {
	docs: defineCollection({ loader: docsLoader(), schema: docsSchema() }),
	// Dieselben Dateien wie in `docs`, nur die Fallseiten und mit ihrem Steckbrief: `docs` rendert
	// sie, diese Collection prüft das Frontmatter und liefert es an Katalog und Steckbrief.
	anwendungsfaelle: defineCollection({
		loader: glob({ base: './src/content/docs/anwendungsfaelle', pattern: ['*.{md,mdx}', '!index.*'] }),
		schema: fallseiteSchema,
	}),
};
