import Box from '@mui/material/Box';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import {Avatar, ListItemAvatar, ListItemText, Select, Switch} from "@mui/material";
import LocalPoliceIcon from '@mui/icons-material/LocalPolice';
import {ChangeEvent, useEffect, useState} from "react";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import RecordVoiceOverIcon from '@mui/icons-material/RecordVoiceOver';

import {SelectChangeEvent} from "@mui/material/Select";
import {SensibilityLevel, useSettingsContext} from "../context/SettingsProvider.tsx";

const supportedLanguages: Map<string, string> = new Map<string, string>(
    [['en', "Englisch"], ['de', 'German']]
);

function Settings() {
    const [everyoneCanBecomeAdmin, setEveryoneCanBecomeAdmin] = useState(true);
    const [language, setLanguage] = useState<string>("en");
    const [sensibility, setSensibility] = useState<SensibilityLevel>(SensibilityLevel.MEDIUM);

    const changeEveryoneCanBecomeAdmin = (event: ChangeEvent<HTMLInputElement>) => {
        setEveryoneCanBecomeAdmin(event.target.checked);
    };

    const handleLanguageChange = (event: SelectChangeEvent) => {
        const languageKey = event.target.value as string;
        setLanguage(languageKey);

        const url = `http://${window.location.hostname}/api/settings/language`;
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({language: languageKey}),
        })
            .then((response) => {
                if (!response.ok) throw new Error('Failed to set sensitivity');
            })
            .then((result) => {
                console.log('Success:', result);
            })
            .catch((error) => {
                console.error('Error:', error);
            });
    };

    const handleSensibilityChange = (event: SelectChangeEvent) => {
        const sensitivity = event.target.value as SensibilityLevel;
        setSensibility(sensitivity)

        const url = `http://${window.location.hostname}/api/settings/sensitivity`;
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({sensitivity: sensitivity.toString()}),
        })
            .then((response) => {
                if (!response.ok) throw new Error('Failed to set sensitivity');
            })
            .then((result) => {
                console.log('Success:', result);
            })
            .catch((error) => {
                console.error('Error:', error);
            });
    }

    const config = useSettingsContext();

    useEffect(() => {
        if (config !== null) {
            setSensibility(config.sensibility)
            setLanguage(config.language.toLowerCase())
        }
    }, [config])

    return (

        <Box className="rootPage p-4 scroll-auto mb-14 flex items-start">

            <h2 className="mb-2 text-2xl">Settings</h2>
            <List className={"w-full"} sx={{bgcolor: 'background.paper'}}>
                <ListItem className="w-full">
                    <ListItemAvatar>
                        <Avatar>
                            <LocalPoliceIcon style={{color: "#000"}}/>
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="All controllers can become admin"/>
                    <Switch
                        disabled={true}
                        checked={everyoneCanBecomeAdmin}
                        onChange={changeEveryoneCanBecomeAdmin}
                    />
                </ListItem>

                <ListItem className="w-full flex justify-between">
                    <ListItemAvatar>
                        <Avatar>
                            <RecordVoiceOverIcon style={{color: "#000"}}/>
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Voice Language"/>
                    <FormControl sx={{m: 1, minWidth: 120}}>
                        <InputLabel id="settings-language-select-label">Language</InputLabel>
                        <Select
                            className="text-right"
                            labelId="settings-language-select-label"
                            id="settings-language-select"
                            value={language}
                            label="Language"
                            onChange={handleLanguageChange}
                            disabled={false}
                        >
                            {[...supportedLanguages.entries()].map(([key, value]) => (
                                <MenuItem key={"selectedLanguage-" + key} value={key}>{value}</MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </ListItem>
                <ListItem className="w-full flex justify-between">
                    <ListItemAvatar>
                        <Avatar>
                            <RecordVoiceOverIcon style={{color: "#000"}}/>
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Sensibility"/>
                    <FormControl sx={{m: 1, minWidth: 120}}>
                        <InputLabel id="settings-sensibility-select-label">Sensibility</InputLabel>
                        <Select
                            className="text-right"
                            labelId="settings-sensibility-select-label"
                            id="settings-sensibility-select"
                            value={sensibility.valueOf()}
                            label="Sensibility"
                            onChange={handleSensibilityChange}
                        >
                            <MenuItem key={"sensitivity-VERY_LOW"} value={SensibilityLevel.VERY_LOW.valueOf()}>Very
                                Low</MenuItem>
                            <MenuItem key={"sensitivity-LOW"} value={SensibilityLevel.LOW.valueOf()}>Low</MenuItem>
                            <MenuItem key={"sensitivity-MEDIUM"}
                                      value={SensibilityLevel.MEDIUM.valueOf()}>Medium</MenuItem>
                            <MenuItem key={"sensitivity-HIGH"} value={SensibilityLevel.HIGH.valueOf()}>High</MenuItem>
                            <MenuItem key={"sensitivity-VERY_HIGH"} value={SensibilityLevel.VERY_HIGH.valueOf()}>Very
                                High</MenuItem>
                        </Select>
                    </FormControl>
                </ListItem>
            </List>
        </Box>
    )
}

export default Settings