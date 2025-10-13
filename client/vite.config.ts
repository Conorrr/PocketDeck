import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import serveStatic from 'serve-static';
import path from 'path';

export default defineConfig({
	plugins: [
		tailwindcss(),
		sveltekit(),
		{
			name: 'serve-external-images',
			configureServer(server) {
			  const imagesDir = path.resolve(__dirname, '../card-images');
			  server.middlewares.use(
				'/card-images',
				serveStatic(imagesDir)
			  );
			}
		  },
		  viteStaticCopy({
			targets: [
			  {
				src: path.resolve(__dirname, '../card-images'),
				dest: 'card-images'
			  }
			]
		  }),
	],
	server:{
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
