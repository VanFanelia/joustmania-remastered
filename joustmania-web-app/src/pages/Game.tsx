import Box from '@mui/material/Box';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select, {SelectChangeEvent} from '@mui/material/Select';
import {useState} from "react";
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import {Avatar, ListItemAvatar, ListItemText} from "@mui/material";
import ImageIcon from '@mui/icons-material/Image';
import SportsHandballIcon from '@mui/icons-material/SportsHandball';
// @ts-ignore
import PSMoveIcon from '../assets/psmove2.svg?react';

function Game() {
    const possibleGames: Map<string, string> = new Map<string, string>(
        [['ffa', "FreeForAll"], ['toddler', 'Sorting Toddler']]
    );

    const [currentGame, setCurrentGame] = useState<string>('');
    const [gameState, setGameState] = useState<string>('Lobby');
    const [activePlayer, setActivePlayer] = useState<number>(0);
    const [connectedController, setConnectedController] = useState<number>(0);


    const handleChange = (event: SelectChangeEvent) => {
        setCurrentGame(event.target.value as string);
    };

    return (
        <Box className="rootPage p-4" >
            <Box sx={{minWidth: 120}}>
                <FormControl fullWidth>
                    <InputLabel id="demo-simple-select-label">CurrentGame</InputLabel>
                    <Select
                        labelId="demo-simple-select-label"
                        id="demo-simple-select"
                        value={currentGame}
                        label="CurrentGame"
                        onChange={handleChange}
                    >
                        {[...possibleGames.entries()].map(([key, value]) => (
                            <MenuItem key={"selectGame" + key} value={key}>{value}</MenuItem>
                        ))}
                    </Select>
                </FormControl>
            </Box>

            <List className={"w-full"} sx={{bgcolor: 'background.paper' }}>
                <ListItem>
                    <ListItemAvatar>
                        <Avatar>
                            <ImageIcon />
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Game State"/>
                    <Box className="text-right font-bold">{gameState}</Box>
                </ListItem>
                <ListItem className="w-full">
                    <ListItemAvatar>
                        <Avatar>
                            <SportsHandballIcon style={{color: "#000"}} />
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Active Player"/>
                    <Box className="text-right font-bold">{activePlayer}</Box>
                </ListItem>
                <ListItem>
                    <ListItemAvatar>
                        <Avatar>
                            <PSMoveIcon width={32} height={32} style={{color: "#000"}}/>
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Connected Controller" />
                    <Box className="text-right font-bold">{connectedController}</Box>
                </ListItem>
            </List>

        </Box>)
}

export default Game