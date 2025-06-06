import {MacAddress} from "../dto/HardwareDTOs.tsx";
import {ApiResult, ApiStatus} from "./api.definitions.tsx";


export async function blinkMoveController(macAddress: MacAddress): Promise<ApiResult>  {
    const url = `http://${window.location.hostname}/api/setRainbowAnimation/${macAddress}/8000`
    try {
        const response = await fetch(url, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({}),
        })
        if (!response.ok) {
            const body = await response.text()
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to blink move with address: ${macAddress}. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }
    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to blink move with address: ${macAddress}. Reason: ${error}`
        }
    }
}

export async function rumbleMoveController(macAddress: MacAddress): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/setRumble/${macAddress}`
    try {
        const response = await fetch(url, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({}),
        })
        if (!response.ok) {
            const body = await response.text()
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to rumble move with address: ${macAddress}. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }
    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to rumble move with address: ${macAddress}. Reason: ${error}`
        }
    }
}