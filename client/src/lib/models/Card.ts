export type Card = {
    expansions: string[]
    cardIds: string[]
    hp: string | null
    energy: string| null
    name: string
    cardType: string
    evolutionType: string
    attacks: Attack[]
    ability: Ability | null;
    weakness: string
    retreat: string | null
    rarity: string // todo replace with a rarity type
    ex: boolean
    alternateVersions: string[]
    artist: string
}

export type Attack = {
    cost: string[]
    name: string
    damage: string
    effect: string
}

export type Ability = {
    name: string,
    effect: string,
}