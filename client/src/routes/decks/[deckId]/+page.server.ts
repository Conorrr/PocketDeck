
import type { PageServerLoad } from './$types';
import DeckCompressor from '$lib/DeckCompressor.server';
import cards from '$lib/data/allCards.json';
import type { Card } from '$lib/models/Card';

const typedCards = cards as Card[];

const allCards: Map<string, Card> = new Map<string, Card>(
	typedCards.map(card => [card.cardIds[0], card] as const)
);

const deckCompressor = new DeckCompressor();

export const load: PageServerLoad = async ({ params, url }) => {
	const cardIds = deckCompressor.decompress(params.deckId).map(original => ({ ...original, name: allCards.get(original.cardId)?.name }));
	const cardDetails = cardIds.map(card => allCards.get(card.cardId));
	const pokemon = cardDetails.filter(card => card?.cardType == 'pokémon');
	const d4Cards = pokemon.filter(card => card?.rarity == '4d').map(card => card?.name);
	const d3Cards = pokemon.filter(card => card?.rarity == '3d').map(card => card?.name);
	const d2Cards = pokemon.filter(card => card?.rarity == '2d').map(card => card?.name);
	var orderedCards = [d4Cards, d3Cards, d2Cards].flat();

	let title = "PocketDeck - Pokemon TCG Pocket Decks";
	let description = "Easily share decks for Pokemon TCG Pocket";
	let name = null;
	if (orderedCards.length > 0) {
		name = `${orderedCards.splice(0, 3).join('/ ')} Deck`;
		title = `PocketDeck - ${name}`;
		description += `. ${d4Cards} Deck`;
	}

	return {
		cards: cardIds,
		deckId: params.deckId,
		title,
		description,
		name
	};
};