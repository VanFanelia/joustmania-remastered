

export enum GameMode {
    FREE_FOR_ALL = "FreeForAll",
    SORTING_TODDLER = "SortingToddler",
    WEREWOLF = "Werewolf",
    ZOMBIE = "Zombie",
}

export function getDisplayName(gameMode: GameMode): string {
    return possibleGames.get(gameMode) ?? gameMode
};

export const possibleGames: Map<string, string> = new Map<string, string>(
    [
        [GameMode.FREE_FOR_ALL, "Free For All"],
        [GameMode.SORTING_TODDLER, 'Sorting Toddler'],
        [GameMode.WEREWOLF, 'Werewolf'],
        [GameMode.ZOMBIE, 'Zombie'],
    ]
);