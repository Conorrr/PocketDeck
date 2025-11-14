
import type { LayoutServerLoad } from './$types';

export const load: LayoutServerLoad = async ({ url }) => {
	
	const currentUrl = url.href;
	return {
		currentUrl,
	};
};