
import type { PageServerLoad } from './$types';
import DeckCompressor from '$lib/DeckCompressor.server';

const deckCompressor = new DeckCompressor();

export const load: PageServerLoad = async ({ params }) => {
	return { cards: deckCompressor.decompress(params.deckId) };
};