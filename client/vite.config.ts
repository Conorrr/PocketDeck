import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import serveStatic from 'serve-static';
import path from 'path';

const externalImagesDir = path.resolve(__dirname, '../card-images/');

export default defineConfig({
	plugins: [
		tailwindcss(),
		sveltekit(),
		{
			name: 'serve-external-images',
			configureServer(server) {
				server.middlewares.use(
					'/card-images',
					serveStatic(externalImagesDir)
				);
			}
		},
		viteStaticCopy({
			targets: [
				{
					src: externalImagesDir,
					dest: ''
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
