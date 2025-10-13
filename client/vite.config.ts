import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import path from 'path';

const externalImagesDir = path.resolve(__dirname, '../card-images/');

export default defineConfig({
	plugins: [
		tailwindcss(),
		sveltekit(),
		viteStaticCopy({
			targets: [
				{
					src: `${externalImagesDir}/**/*.webp`,
					dest: 'card-images',
					flatten: true,
				}
			]
		}),
	],
	server: {
		fs: {
			allow: ['..']
		}
	},
	// resolve: {
	//   alias: {
	// 	$images: path.resolve(__dirname, '../card-images'),
	// 	$collections: path.resolve(__dirname, '../collections'),
	//   }
	// }
});
