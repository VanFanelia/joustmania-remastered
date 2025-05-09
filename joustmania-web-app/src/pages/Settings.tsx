import Box from '@mui/material/Box';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import {Avatar, ListItemAvatar, ListItemText, Select, Switch} from "@mui/material";
import LocalPoliceIcon from '@mui/icons-material/LocalPolice';
// @ts-ignore
import PSMoveIcon from '../assets/psmove2.svg?react';
import {ChangeEvent, useState} from "react";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import RecordVoiceOverIcon from '@mui/icons-material/RecordVoiceOver';

import {SelectChangeEvent} from "@mui/material/Select";

const supportedLanguages: Map<string, string> = new Map<string, string>(
    [['en', "Englisch"], ['de', 'German']]
);

function Settings() {
    const [everyoneCanBecomeAdmin, setEveryoneCanBecomeAdmin] = useState(true);
    const [language, setLanguage] = useState<string>("de");

    const changeEveryoneCanBecomeAdmin = (event: ChangeEvent<HTMLInputElement>) => {
        setEveryoneCanBecomeAdmin(event.target.checked);
    };

    const handleLanguageChange = (event: SelectChangeEvent) => {
        setLanguage(event.target.value as string);
    };

    return (
        <Box className="rootPage p-4" >

            <h2 className="mb-2 text-2xl">Settings</h2>
            <List className={"w-full"} sx={{bgcolor: 'background.paper' }}>
                <ListItem className="w-full">
                    <ListItemAvatar>
                        <Avatar>
                            <LocalPoliceIcon style={{color: "#000"}} />
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="All controllers can become admin"/>
                    <Switch
                        checked={everyoneCanBecomeAdmin}
                        onChange={changeEveryoneCanBecomeAdmin}
                    />
                </ListItem>

                <ListItem className="w-full flex justify-between">
                    <ListItemAvatar>
                        <Avatar>
                            <RecordVoiceOverIcon style={{color: "#000"}} />
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Voice Language"/>
                    <FormControl sx={{ m: 1, minWidth: 80 }}>
                        <InputLabel id="settings-language-select-label">Language</InputLabel>
                        <Select
                            className="text-right"
                            labelId="settings-language-select-label"
                            id="settings-language-select"
                            value={language}
                            label="Language"
                            onChange={handleLanguageChange}
                        >
                            {[...supportedLanguages.entries()].map(([key, value]) => (
                                <MenuItem key={"selectedLanguage" + key} value={key}>{value}</MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </ListItem>

            </List>

        </Box>)
}

export default Settings