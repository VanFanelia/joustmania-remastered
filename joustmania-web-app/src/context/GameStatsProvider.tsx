import {createContext, FC, ReactNode, useContext} from "react";
import {useSSE} from "../hooks/useSSE.tsx";

export class GameStats {
    constructor(
        public currentGameState: string,
        public activeController: string[],
        public playerInGame: string[],
        public playerLost: string[]
    ) {}

    static DEFAULT_GAME_STATS = new GameStats("Lobby", [], [], [])
}

const GameStatsContext = createContext<GameStats>(GameStats.DEFAULT_GAME_STATS);

export const GameStatsProvider: FC<{ children: ReactNode }> = ({children}) => {
    let config = useSSE<GameStats>(`http://${window.location.hostname}/api/sse/game`);

    console.log(config)

    if (config == null) {
        config = GameStats.DEFAULT_GAME_STATS
    }

    return (
        <GameStatsContext.Provider value={config}>
            {children}
        </GameStatsContext.Provider>
    );
};

export const useGameStatsContext = (): GameStats => {
    const ctx = useContext(GameStatsContext);
    if (!ctx) {
        throw new Error("useGameStatsContext must be used inside GameStatsProvider");
    }
    return ctx;
};