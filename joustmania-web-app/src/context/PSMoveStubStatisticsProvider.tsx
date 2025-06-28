import {createContext, FC, ReactNode, useContext} from "react";
import {useSSE} from "../hooks/useSSE.tsx";

export class MoveStatistics {
    public firstPoll: number;
    public pollCount: number;
    public averagePollTime: number;
    public longPollingDistanceCounter: number;
    public longestPollingGap: number;

    constructor(firstPoll: number, pollCount: number, averagePollTime: number, longPollingDistanceCounter: number, longestPollingGap: number) {
        this.firstPoll = firstPoll;
        this.pollCount = pollCount;
        this.averagePollTime = averagePollTime;
        this.longPollingDistanceCounter = longPollingDistanceCounter;
        this.longestPollingGap = longestPollingGap;
    }
}

type MoveStatisticsMap = {
    [key: string]: MoveStatistics;
};

const PSMoveStubStatisticsContext = createContext<MoveStatisticsMap>({});

export const PSMoveStubStatisticsProvider: FC<{ children: ReactNode }> = ({children}) => {
    let config = useSSE<MoveStatisticsMap>(`http://${window.location.hostname}/api/sse/stubsStatistics`);

    if (config == null) {
        config = {}
    }

    return (
        <PSMoveStubStatisticsContext.Provider value={config}>
            {children}
        </PSMoveStubStatisticsContext.Provider>
    );
};

export const usePSMoveStubStatisticsContext = (): MoveStatisticsMap => {
    const ctx = useContext(PSMoveStubStatisticsContext);
    if (!ctx) {
        throw new Error("usePSMoveStubStatisticsContext must be used inside PSMoveStubStatisticsProvider");
    }
    return ctx;
};