import type { Card } from "$lib/models/Card.js";
import { error } from "@sveltejs/kit";
import { readFile } from "fs/promises";

const allCards: Map<string, Card> = new Map<string, Card>(
  (await loadCardJson(`../allCards.json`)).map(card => [card.cardIds[0], card] as const)
);

async function loadCardJson(filename: string): Promise<Card[]> {
  return JSON.parse(await readFile(filename, "utf-8"));
}

export async function GET({ params }) {
  const { cardId } = params;

  const cardDetails = allCards.get(cardId);

  if (!cardDetails) {
    return error(404, 'Card not found');
  }

  return new Response(JSON.stringify(cardDetails), {
    headers: { "Content-Type": "application/json" }
  });
}
