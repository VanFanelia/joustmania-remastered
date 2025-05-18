import Box from '@mui/material/Box';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import {useState} from "react";
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import {Avatar, ListItemAvatar, ListItemText} from "@mui/material";
import TransferWithinAStationIcon from '@mui/icons-material/TransferWithinAStation';
import SportsHandballIcon from '@mui/icons-material/SportsHandball';
import Diversity3Icon from '@mui/icons-material/Diversity3';
// @ts-ignore
import PSMoveController from '../assets/PSMoveController.svg?react';

enum GameState {
    LOBBY,
    GAME_RUNNING
}

function Game() {
    const possibleGames: Map<string, string> = new Map<string, string>(
        [
            ['ffa', "FreeForAll"],
            //['toddler', 'Sorting Toddler']
        ]
    );

    const [currentGame, setCurrentGame] = useState<string>('ffa');
    // @ts-ignore
    const [gameState, setGameState] = useState<GameState>(GameState.LOBBY);
    // @ts-ignore
    const [activePlayer, setActivePlayer] = useState<number>(0);
    // @ts-ignore
    const [adminCount, setAdminCount] = useState<number>(0);
    // @ts-ignore
    const [connectedController, setConnectedController] = useState<number>(0);


    const handleChange = (event: SelectChangeEvent) => {
        setCurrentGame(event.target.value as string);
    };

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
                            {(gameState == GameState.LOBBY) ? (
                                <TransferWithinAStationIcon style={{color: "#000"}}/>
                            ) : (
                                <SportsHandballIcon style={{color: "#000"}}/>
                            )}
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Game State"/>
                    <Box
                        className="text-right font-bold">{gameState == GameState.LOBBY ? "Lobby" : "Game running"}</Box>
                </ListItem>
                <ListItem className="w-full">
                    <ListItemAvatar>
                        <Avatar>
                            <Diversity3Icon style={{color: "#000"}}/>
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Active Player"/>
                    <Box className="text-right font-bold">{"?" /* activePlayer */}</Box>
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
                    <Box className="text-right font-bold">{"?" /* connectedController */}</Box>
                </ListItem>
            </List>

        </Box>)
}

export default Game