import type { Card } from "$lib/models/Card.js";
import { error } from "@sveltejs/kit";
import cards from '$lib/data/allCards.json';

const typedCard = cards as Card[];

const allCards: Map<string, Card> = new Map<string, Card>(
  typedCard.map(card => [card.cardIds[0], card] as const)
);

export async function GET({ params }) {
  const { cardId } = params;

  const cardDetails = allCards.get(cardId);

  if (!cardDetails) {
    return error(404, 'Card not found');
  }

  return new Response(JSON.stringify(cardDetails), {
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "public, max-age=86400"
     }
  });
}
