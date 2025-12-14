

export enum GameMode {
    FREE_FOR_ALL = "FreeForAll",
    SORTING_TODDLER = "SortingToddler",
    WEREWOLF = "Werewolf",
    ZOMBIE = "Zombie",
    RED_ALERT = "RedAlert",
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
        [GameMode.RED_ALERT, 'Red Alert'],
    ]
);