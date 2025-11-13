export type Card = {
    expansions: string[]
    cardIds: string[]
    hp: string | null
    energy: Energy | null
    name: string
    cardType: string
    evolutionType?: string
    attacks: Attack[]
    ability: Ability | null;
    weakness?: Energy | "none"
    retreat: string | null
    rarity: Rarity
    ex: boolean
    alternateVersions: string[]
    artist: string
}

export type Attack = {
    cost: Energy[]
    name: string
    damage: string
    effect: string
}

export type Ability = {
    name: string,
    effect: string,
}

export type Energy = 'colorless' | 'darkness' | 'fighting' | 'fire' | 'grass' | 'lightning' | 'metal' | 'psychic' | 'water'
export type Rarity = '1d'| '2d'| '3d'| '4d'| 'p'| 'sh'| '2sh'| '1s'| '2s'| '3s'| 'c';
