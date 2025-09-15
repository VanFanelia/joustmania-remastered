import {createContext, FC, ReactNode, useContext} from "react";
import {useSSE} from "../hooks/useSSE.tsx";
import {EmptyDefaultThreadHierarchy, ThreadHierarchy} from "../dto/HardwareDTOs.tsx";

const ThreadContext = createContext<ThreadHierarchy>(EmptyDefaultThreadHierarchy);

export const ThreadProvider: FC<{ children: ReactNode }> = ({children}) => {
    let threadHierarchy = useSSE<ThreadHierarchy>(`http://${window.location.hostname}/api/sse/threads`);

    if (threadHierarchy == null) {
        threadHierarchy = EmptyDefaultThreadHierarchy
    }

    return (
        <ThreadContext.Provider value={threadHierarchy}>
            {children}
        </ThreadContext.Provider>
    );
};

export const useThreadContext = (): ThreadHierarchy => {
    const ctx = useContext(ThreadContext);
    if (!ctx) {
        throw new Error("useBluetoothContext must be used inside BluetoothProvider");
    }
    return ctx;
};