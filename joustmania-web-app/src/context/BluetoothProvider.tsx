import {createContext, FC, ReactNode, useContext} from "react";
import {useSSE} from "../hooks/useSSE.tsx";

type AdapterId = string;  // oder ein spezifischer Typ, falls bekannt
type MacAddress = string; // ebenfalls anpassen, wenn es eine spezielle Struktur gibt

interface PairedDevice {
    adapterId: AdapterId;
    macAddress: MacAddress;
    name: string;
    paired?: boolean;
    connected?: boolean;
}

const BluetoothContext = createContext<PairedDevice[]>([]);

export const BluetoothProvider: FC<{ children: ReactNode }> = ({children}) => {
    let bluetoothDevices = useSSE<PairedDevice[]>("http://localhost:80/api/sse/bluetooth");

    if (bluetoothDevices == null) {
        bluetoothDevices = []
    }

    return (
        <BluetoothContext.Provider value={bluetoothDevices}>
            {children}
        </BluetoothContext.Provider>
    );
};

export const useBluetoothContext = (): PairedDevice[] => {
    const ctx = useContext(BluetoothContext);
    if (!ctx) {
        throw new Error("useSettingsContext must be used inside SettingsProvider");
    }
    return ctx;
};