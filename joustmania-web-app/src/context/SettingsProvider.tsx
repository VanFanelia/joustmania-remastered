import {createContext, FC, ReactNode, useContext} from "react";
import {useSSE} from "../hooks/useSSE.tsx";

export enum Language {
    EN = "EN",
    DE = "DE",
}

export enum SensibilityLevel {
    VERY_LOW = "VERY_LOW",
    LOW = "LOW",
    MEDIUM = "MEDIUM",
    HIGH = "HIGH",
    VERY_HIGH = "VERY_HIGH",
}

class SortToddlerGameOptions {
    roundDuration: number;
    amountOfRounds: number;

    constructor(roundDuration: number, amountOfRounds: number) {
        this.roundDuration = roundDuration;
        this.amountOfRounds = amountOfRounds;
    }
}

class Config {
    sensibility: SensibilityLevel;
    language: Language;
    enableAP: boolean;
    sortToddlerGameOptions: SortToddlerGameOptions;
    globalVolume: number;
    musicVolume: number;

    constructor(
        sensibility: SensibilityLevel,
        language: Language,
        enableAP: boolean,
        sortToddlerGameOptions: SortToddlerGameOptions,
        globalVolume: number,
        musicVolume: number,
    ) {
        this.sensibility = sensibility;
        this.language = language;
        this.enableAP = enableAP;
        this.sortToddlerGameOptions = sortToddlerGameOptions;
        this.globalVolume = globalVolume;
        this.musicVolume = musicVolume;
    }

    static DEFAULT_CONFIG = new Config(
        SensibilityLevel.MEDIUM,
        Language.EN,
        false,
        new SortToddlerGameOptions(30, 10),
        50,
        50,
    );
}

const SettingsContext = createContext<Config>(Config.DEFAULT_CONFIG);

export const SettingsProvider: FC<{ children: ReactNode }> = ({children}) => {
    let config = useSSE<Config>(`http://${window.location.hostname}/api/sse/settings`);

    if (config == null) {
        config = Config.DEFAULT_CONFIG
    }

    return (
        <SettingsContext.Provider value={config}>
            {children}
        </SettingsContext.Provider>
    );
};

export const useSettingsContext = (): Config | null => {
    const ctx = useContext(SettingsContext);
    if (!ctx) {
        throw new Error("useSettingsContext must be used inside SettingsProvider");
    }
    return ctx;
};