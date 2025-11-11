// map from raw data type to human readable values

const expansionMap = new Map(
    [
        ["P-A", "Promo-A"],
        ["P-B", "Promo-B"],
        ["A1", "Genetic Apex"],
        ["A1a", "Mythical Island"],
        ["A2", "Space-Time Smackdown"],
        ["A2a", "Triumphant Light"],
        ["A2b", "Shining Revelry"],
        ["A3", "Celestial Guardians"],
        ["A3a", "Extradimensional Crisis"],
        ["A3b", "Eevee Grove"],
        ["A4", "Wisdom of Sea and Sky"],
        ["A4a", "Secluded Springs"],
        ["A4b", "Deluxe Pack: ex"],
        ["B1", "Mega Rising"],
    ]
)

export function nameExpansion(expansion: string): string {
    return expansionMap.get(expansion) || expansion;
}