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

export async function setMusicVolume(newMusicVolume: number): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/settings/musicVolume`;
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({musicVolume: newMusicVolume}),
        })
        if (!response.ok) {
            const body = await response.text()
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to set the new music Volume. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }

    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to set the new music Volume. Reason: ${error}`
        }
    }
}

export async function setGlobalVolume(newGlobalVolume: number): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/settings/globalVolume`;
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({globalVolume: newGlobalVolume}),
        })
        if (!response.ok) {
            const body = await response.text()
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to set the new global Volume. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }

    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to set the new global Volume. Reason: ${error}`
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

export async function setGameOptionSortToddlerRoundDuration(newDuration: number): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/settings/sortToddler/duration`;
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({duration: newDuration}),
        })
        if (!response.ok) {
            const body = await response.text()
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to set the new duration for Sort Toddler Game. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }
    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to set the new duration for Sort Toddler Game. Reason: ${error}`
        }
    }
}

export async function setGameOptionSortToddlerAmountOfRounds(newAmount: number): Promise<ApiResult> {
    const url = `http://${window.location.hostname}/api/settings/sortToddler/amountOfRounds`;
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({amountOfRounds: newAmount}),
        })
        if (!response.ok) {
            const body = await response.text()
            console.error(body);
            return {
                status: ApiStatus.ERROR,
                reason: `Failed to set the new amount of rounds for Sort Toddler Game. Reason: ${body}`
            }
        } else {
            return {status: ApiStatus.OK}
        }
    } catch (error) {
        console.error(error)
        return {
            status: ApiStatus.ERROR,
            reason: `Failed to set the new amount of rounds for Sort Toddler Game. Reason: ${error}`
        }
    }
}