import Box from '@mui/material/Box';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import {useEffect, useState} from "react";
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import {Alert, Avatar, ListItemAvatar, ListItemText} from "@mui/material";
import TransferWithinAStationIcon from '@mui/icons-material/TransferWithinAStation';
import SportsHandballIcon from '@mui/icons-material/SportsHandball';
import Diversity3Icon from '@mui/icons-material/Diversity3';
// @ts-ignore
import PSMoveController from '../assets/PSMoveController.svg?react';
import {useBluetoothContext} from "../context/BluetoothProvider.tsx";
import {useGameStatsContext} from "../context/GameStatsProvider.tsx";
import ConfirmGameStartButtonWithDialog from "../components/ConfirmGameStartButtonWithDialog.tsx";
import AbortGameButtonWithDialog from "../components/AbortGameButtonWithDialog.tsx";

function Game() {
    const possibleGames: Map<string, string> = new Map<string, string>(
        [
            ['ffa', "FreeForAll"],
            //['toddler', 'Sorting Toddler']
        ]
    );

    const [currentGame, setCurrentGame] = useState<string>('ffa');
    const [gameState, setGameState] = useState<string>("Lobby");
    const [activePlayer, setActivePlayer] = useState<number>(0);
    const [adminCount, setAdminCount] = useState<number>(0);
    const [connectedController, setConnectedController] = useState<number>(0);
    const [pairedController, setPairedController] = useState<number>(0);

    const [error, setError] = useState<string | null>(null);
    const [showAlert, setShowAlert] = useState(false);

    useEffect(() => {
        if (showAlert) {
            const timer = setTimeout(() => {
                setShowAlert(false);
            }, 10000); // 10 Sekunden
            return () => clearTimeout(timer); // Aufräumen, falls Alert früher verschwindet
        }
    }, [showAlert]);

    const handleChange = (event: SelectChangeEvent) => {
        setCurrentGame(event.target.value as string);
    };

    const bluetoothDevices = useBluetoothContext();

    useEffect(() => {
        setPairedController(bluetoothDevices.reduce((previousValue, bluetoothController) => {
            return previousValue + bluetoothController.pairedMotionController.length
        }, 0))

        setConnectedController(bluetoothDevices.reduce((previousValue, bluetoothController) => {
            return previousValue + bluetoothController.pairedMotionController.filter(controller => controller.connected).length
        }, 0))

        setAdminCount(bluetoothDevices.reduce((previousValue, bluetoothController) => {
            return previousValue + bluetoothController.pairedMotionController.filter(controller => controller.isAdmin).length
        }, 0))
    }, [bluetoothDevices])

    const gameStats = useGameStatsContext();

    useEffect(() => {
        setGameState(gameStats.currentGameState);
        setActivePlayer(gameStats.activeController.length)
    }, [gameStats]);

    const isGameStateInLobby = gameState == "Lobby"

    function forceStartGame(gameState: string) {
        console.debug("Force start: ", gameState);

        const url = `http://${window.location.hostname}/api/game/force-start`;
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({}),
        })
            .then(async (response) => {
                if (!response.ok) {
                    const body = await response.text()
                    console.error(body);
                    setError(`Failed to force start the game. Reason: ${body}`);
                    setShowAlert(true);
                }
            })
            .catch((error) => {
                console.error(error)
                setError(error.message);
                setShowAlert(true);
            });
    }

    function forceStopGame() {
        console.debug("Force stop: ", gameState);

        const url = `http://${window.location.hostname}/api/game/force-stop`;
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({}),
        })
            .then(async (response) => {
                if (!response.ok) {
                    const body = await response.text();
                    console.error(body);
                    setError(`Failed to force stop the game. Reason: ${body}`);
                    setShowAlert(true);
                }
            })
            .catch((error) => {
                console.error(error);
                setError(error.message);
                setShowAlert(true);
            });
    }

    return (
        <Box className="rootPage p-4 scroll-auto mb-14">
            <Box sx={{minWidth: 120}}>
                <FormControl fullWidth>
                    <InputLabel id="game-mode-select-label">Selected Game Mode</InputLabel>
                    <Select
                        labelId="game-mode-select-label"
                        id="game-mode-select"
                        value={currentGame}
                        label="CurrentGame"
                        disabled={false}
                        onChange={handleChange}
                        defaultValue={"selectGame-ffa"}
                    >
                        {[...possibleGames.entries()].map(([key, value]) =>
                            (
                                <MenuItem key={"selectGame-" + key} value={key}>{value}</MenuItem>
                            )
                        )}
                    </Select>
                </FormControl>
            </Box>

            <List className={"w-full"} sx={{bgcolor: 'background.paper'}}>
                <ListItem>
                    <ListItemAvatar>
                        <Avatar>
                            {(isGameStateInLobby) ? (
                                <TransferWithinAStationIcon style={{color: "#000"}}/>
                            ) : (
                                <SportsHandballIcon style={{color: "#000"}}/>
                            )}
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Game State"/>
                    <Box
                        className="text-right font-bold">{isGameStateInLobby ? "Lobby" : "Game running"}</Box>
                </ListItem>
                <ListItem className="w-full">
                    <ListItemAvatar>
                        <Avatar>
                            <Diversity3Icon style={{color: "#000"}}/>
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Active Player"/>
                    <Box className="text-right font-bold">{activePlayer}</Box>
                </ListItem>
                <ListItem>
                    <ListItemAvatar>
                        <Avatar>
                            <PSMoveController width={32} height={32}
                                              style={{color: "#ffa500", transform: "rotate(20deg)"}}/>
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Connected Controller"
                                  secondary={`${adminCount} controller with admin rights`}/>
                    <Box className="text-right font-bold">{`${connectedController} / ${pairedController}`}</Box>
                </ListItem>
            </List>

            <Box className={"mt-8"}>
                {isGameStateInLobby ? (
                        <ConfirmGameStartButtonWithDialog gameNameToStart={possibleGames.get(currentGame)}
                                                          gameMode={currentGame}
                                                          onConfirm={forceStartGame}/>
                    ) :
                    (
                        <AbortGameButtonWithDialog onConfirm={forceStopGame}/>
                    )
                }

            </Box>

            {showAlert && (
                <Alert severity="error" className={"absolute bottom-16"}>{error}</Alert>
            )}
        </Box>)
}

export default Game