import {ApiResult, ApiStatus} from "./api.definitions";

export async function setGameMode(newGameMode: string): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/settings/set-game-mode`;
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({gameMode: newGameMode}),
        })
        if (!response.ok) {
            const body = await response.text()
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to set the new game Mode. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }
    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to set the new game Mode. Reason: ${error}`
        }
    }
}

export async function setSensitivity(newSensitivity: string): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/settings/sensitivity`;
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({sensitivity: newSensitivity}),
        })
        if (!response.ok) {
            const body = await response.text()
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to set the new sensitivity. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }
        
    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to set the new sensitivity. Reason: ${error}`
        }
    }
}

export async function setLanguage(newLanguage: string): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/settings/language`;
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({language: newLanguage}),
        })
        if (!response.ok) {
            const body = await response.text()
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to set the new language. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }
    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to set the new language. Reason: ${error}`
        }
    }
}

