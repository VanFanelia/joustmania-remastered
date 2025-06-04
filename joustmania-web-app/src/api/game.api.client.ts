import {ApiResult, ApiStatus} from "./api.definitions";

export async function forceStartGame(): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/game/force-start`;
    try {
        const response = await fetch(url, {
            method: 'POST',
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
                reason: `Failed to force start the game. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }
    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to force start the game. Reason: ${error}`
        }
    }
}

export async function forceStopGame(): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/game/force-stop`;
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({}),
        })
        if (!response.ok) {
            const body = await response.text();
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to force stop the game. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }
    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to force stop the game. Reason: ${error}`
        }
    }
}
