import type { PageServerLoad } from './$types';
import { PUBLIC_API_URL } from '$env/static/public';
import DeckCompressor from '$lib/DeckCompressor.server';
import { findTopCards, genDeckName } from '$lib/cardUtils';

const deckCompressor = new DeckCompressor();

export const load: PageServerLoad = async () => {
  const timeout = (ms: number) =>
    new Promise<never>((_, reject) => setTimeout(() => reject(new Error('timeout')), ms));

  try {
    const response = await Promise.race([
      fetch(`${PUBLIC_API_URL}/latest`),
      timeout(1000)
    ]);

    if (!response.ok) throw new Error('API error');

    const latestDeckIds: string[] = await response.json();

    const latestDeckSummaries = latestDeckIds.map(deckId => {
      const deckDetails = deckCompressor.decompress(deckId);
      const cardIds = deckDetails.map(card => card.cardId);
      const topCards = findTopCards(cardIds);
      const deckName = genDeckName(cardIds);
      return {
        deckId, topCards, deckName
      }
    })

    return { latest: latestDeckSummaries };
  } catch (error) {
    return { latest: [] };
  }
};
