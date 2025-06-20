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
