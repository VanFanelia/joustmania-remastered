export enum AkkuState {
    UNKNOWN = 8,
    LEVEL_0 = 0, // empty
    LEVEL_1 = 1,
    LEVEL_2 = 2,
    LEVEL_3 = 3,
    LEVEL_4 = 4,
    LEVEL_5 = 5, // full
    CHARGING = 6,
    CHARGING_DONE = 7,
}

export function toAkkuState(value?: number): AkkuState {
    if (value == undefined) {
        return AkkuState.UNKNOWN
    }
    if (value in AkkuState) {
        return value as AkkuState;
    }
    return AkkuState.UNKNOWN;
}

export function toAkkuStateInPercent(akkuState: AkkuState): number | null {
    switch (akkuState) {
        case AkkuState.UNKNOWN:
        case AkkuState.CHARGING:
        case AkkuState.CHARGING_DONE:
            return null;
        case AkkuState.LEVEL_0:
            return 0;
        case AkkuState.LEVEL_1:
            return 20;
        case AkkuState.LEVEL_2:
            return 40;
        case AkkuState.LEVEL_3:
            return 60;
        case AkkuState.LEVEL_4:
            return 80;
        case AkkuState.LEVEL_5:
            return 100;
    }
}

export function toAkkuStateLabel(akkuState: AkkuState) {
    switch (akkuState) {
        case AkkuState.LEVEL_0:
        case AkkuState.LEVEL_1:
        case AkkuState.LEVEL_2:
        case AkkuState.LEVEL_3:
        case AkkuState.LEVEL_4:
        case AkkuState.LEVEL_5:
            return `${toAkkuStateInPercent(akkuState)} %`
        case AkkuState.UNKNOWN:
            return "Unknown"
        case AkkuState.CHARGING:
            return "Charging"
        case AkkuState.CHARGING_DONE:
            return "Charging done"
    }
}

// Types for AdapterId and MacAddress
export type AdapterId = string;
export type MacAddress = string;

export class MotionControllerStats {
    adapterId: AdapterId;
    macAddress: MacAddress;
    connected?: boolean;
    isAdmin?: boolean;
    batteryLevel?: number;

    constructor(
        adapterId: AdapterId,
        macAddress: MacAddress,
        connected?: boolean,
        isAdmin?: boolean,
        batteryLevel?: number
    ) {
        this.adapterId = adapterId;
        this.macAddress = macAddress;
        this.connected = connected;
        this.isAdmin = isAdmin;
        this.batteryLevel = batteryLevel;
    }
}

export class BlueToothAdapterInfo {
    adapterId: AdapterId;
    macAddress: MacAddress;
    name: string;

    constructor(
        adapterId: AdapterId,
        macAddress: MacAddress,
        name: string
    ) {
        this.adapterId = adapterId
        this.macAddress = macAddress
        this.name = name
    }
}

export class MotionControllerStatsWithAdapterInfo {
    motionController: MotionControllerStats;
    bluetoothAdapter: BlueToothAdapterInfo;

    constructor(
        motionController: MotionControllerStats,
        adapterId: AdapterId,
        macAddress: MacAddress,
        name: string
    ) {
        this.motionController = motionController;
        this.bluetoothAdapter = {
            adapterId,
            macAddress,
            name
        }
    }
}

export class BlueToothControllerStats {
    adapterId: AdapterId;
    macAddress: MacAddress;
    name: string;
    pairedMotionController: MotionControllerStats[];

    constructor(
        adapterId: AdapterId,
        macAddress: MacAddress,
        name: string,
        pairedMotionController: MotionControllerStats[]
    ) {
        this.adapterId = adapterId;
        this.macAddress = macAddress;
        this.name = name;
        this.pairedMotionController = pairedMotionController;
    }
}