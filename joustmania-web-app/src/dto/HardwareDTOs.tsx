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

// Types for AdapterId and MacAddress
type AdapterId = string;
type MacAddress = string;

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