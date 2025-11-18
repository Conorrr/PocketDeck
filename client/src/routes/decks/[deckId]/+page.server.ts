
import type { PageServerLoad } from './$types';
import DeckCompressor from '$lib/DeckCompressor.server';;
import { genDeckName, getCardName } from '$lib/cardUtils';

const deckCompressor = new DeckCompressor();

export const load: PageServerLoad = async ({ params }) => {
	const deckDetails = deckCompressor.decompress(params.deckId).map(original => ({ ...original, name: getCardName(original.cardId) }));
	
	let title = "PocketDeck - Pokemon TCG Pocket Decks";
	let description = "Easily share decks for Pokemon TCG Pocket";

	const cardIds = deckDetails.map(cardSummary => cardSummary.cardId);

	const name = genDeckName(cardIds);
	if (name) {
		title = `PocketDeck - ${name}`;
		description += `. ${name} Deck`;
	}

	return {
		cards: deckDetails,
		deckId: params.deckId,
		title,
		description,
		name
	};
};