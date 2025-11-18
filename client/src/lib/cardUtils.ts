import type { Card } from "./models/Card";
import cards from '$lib/data/allCards.json';

const typedCards = cards as Card[];

const allCards: Map<string, Card> = new Map<string, Card>(
    typedCards.map(card => [card.cardIds[0], card] as const)
);

export function genDeckName(cardIds: string[]): string | null {
    const orderedCards = findTopCards(cardIds).map(card => card?.name);

    if (orderedCards.length > 0) {
        return `${orderedCards.splice(0, 3).join('/ ')} Deck`;
    }
    return null;
}

export function getCardName(cardId: string): string | undefined {
    return allCards.get(cardId)?.name;
}

export function findTopCards(cardIds: string[]): Card[] {
    const cardDetails = cardIds.map(cardId => allCards.get(cardId));
    const pokemon = cardDetails.filter(card => card?.cardType == 'pokémon');
    const d4Cards = pokemon.filter(card => card?.rarity == '4d');
    const d3Cards = pokemon.filter(card => card?.rarity == '3d');
    const d2Cards = pokemon.filter(card => card?.rarity == '2d');
    return [d4Cards, d3Cards, d2Cards].flat().filter(item => item !== undefined);
}