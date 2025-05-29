import {createContext, FC, ReactNode, useContext} from "react";
import {useSSE} from "../hooks/useSSE.tsx";
import {BlueToothControllerStats} from "../dto/HardwareDTOs.tsx";

const BluetoothContext = createContext<BlueToothControllerStats[]>([]);

export const BluetoothProvider: FC<{ children: ReactNode }> = ({children}) => {
    let bluetoothDevices = useSSE<BlueToothControllerStats[]>("http://localhost:80/api/sse/bluetooth");

    if (bluetoothDevices == null) {
        bluetoothDevices = []
    }

    return (
        <BluetoothContext.Provider value={bluetoothDevices}>
            {children}
        </BluetoothContext.Provider>
    );
};

export const useBluetoothContext = (): BlueToothControllerStats[] => {
    const ctx = useContext(BluetoothContext);
    if (!ctx) {
        throw new Error("useSettingsContext must be used inside SettingsProvider");
    }
    return ctx;
};