import cardList from './cardList.json';

export interface DeckCard {
  cardId: string;
  count: number;
}

export default class DeckCompressor {
    private idMap: Map<number, string>;
    private cardMap: Map<string, number>;
  
    constructor() {
      this.idMap = new Map<number, string>();
      this.cardMap = new Map<string, number>();
  
      for (const [key, value] of Object.entries(cardList)) {
        const id = parseInt(key, 10);
        this.idMap.set(id, value);
        this.cardMap.set(value, id);
      }
    }
  
    compress(cards: string[]): string {
      const cardIds = cards.map(card => {
        const id = this.cardMap.get(card);
        if (id === undefined) throw new Error(`Unknown card: ${card}`);
        return id;
      });
      return this.encode(this.collapse(cardIds));
    }
  
    decompress(compressedDeck: string): {cardId: string, count: number}[] {
      const expandedIds = this.expand(this.decode(compressedDeck));
      const countMap = new Map<number, number>();

      // Count occurrences
      for (const id of expandedIds) {
        countMap.set(id, (countMap.get(id) || 0) + 1);
      }

      // Convert to {cardId, count} objects
      const result: DeckCard[] = [];
      for (const [id, count] of countMap) {
        const cardId = this.idMap.get(id);
        if (!cardId) throw new Error(`Unknown card ID: ${id}`);
        result.push({ cardId, count });
      }

      return result;
    }
  
    private collapse(numbers: number[]): number[] {
      if (!numbers.length) return [];
  
      const result: number[] = [];
      let prev = numbers[0];
      let repeated = false;
  
      for (let i = 1; i < numbers.length; i++) {
        const curr = numbers[i];
        if (curr === prev) {
          repeated = true;
        } else {
          result.push(repeated ? -prev : prev);
          prev = curr;
          repeated = false;
        }
      }
  
      result.push(repeated ? -prev : prev);
      return result;
    }
  
    private expand(numbers: number[]): number[] {
      const result: number[] = [];
      for (const n of numbers) {
        if (n < 0) {
          const value = -n;
          result.push(value, value);
        } else {
          result.push(n);
        }
      }
      return result;
    }
  
    private encode(cards: number[]): string {
      if (cards.length > 20) throw new Error('List cannot contain more than 20 cards');
  
      const bytes = new Uint8Array(cards.length * 2);
  
      cards.forEach((card, index) => {
        const isDouble = card < 0;
        const cardId = isDouble ? -card : card;
  
        if (cardId <= 0 || cardId > 32767) {
          throw new Error(`Card ID must be between 1 and 32767, got: ${cardId}`);
        }
  
        const encoded = cardId | (isDouble ? 0x8000 : 0);
        bytes[index * 2] = encoded >> 8;
        bytes[index * 2 + 1] = encoded & 0xff;
      });
  
      return DeckCompressor.base64UrlEncode(bytes);
    }
  
    private decode(encoded: string): number[] {
      const bytes = DeckCompressor.base64UrlDecode(encoded);
  
      if (bytes.length % 2 !== 0) throw new Error('Invalid encoded string length');
  
      const cards: number[] = [];
  
      for (let i = 0; i < bytes.length; i += 2) {
        const encodedVal = (bytes[i] << 8) | bytes[i + 1];
        const isDouble = (encodedVal & 0x8000) !== 0;
        const cardId = encodedVal & 0x7fff;
        cards.push(isDouble ? -cardId : cardId);
      }
  
      return cards;
    }
  
    // URL-safe Base64 helpers
    private static base64UrlEncode(bytes: Uint8Array): string {
      let binary = '';
      for (const b of bytes) binary += String.fromCharCode(b);
      return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }
  
    private static base64UrlDecode(base64: string): Uint8Array {
      // Convert URL-safe to standard base64
      base64 = base64.replace(/-/g, '+').replace(/_/g, '/');
      while (base64.length % 4) base64 += '=';
      const binary = atob(base64);
      const bytes = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
      return bytes;
    }
  }