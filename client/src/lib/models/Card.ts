export type Card = {
    expansions: string[]
    cardIds: string[]
    hp: string
    energy: string
    name: string
    cardType: string
    evolutionType: string
    attacks: Attack[]
    ability: Ability | undefined;
    weakness: string
    retreat: string
    rarity: string // todo replace with a rarity type
    ex: boolean
    pack: string
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