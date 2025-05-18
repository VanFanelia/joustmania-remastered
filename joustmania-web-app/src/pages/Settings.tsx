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

const supportedLanguages: Map<string, string> = new Map<string, string>(
    [['en', "Englisch"], ['de', 'German']]
);

enum Sensitivity {
    "VERY_LOW" = "VERY_LOW",
    "LOW" = "LOW",
    "MEDIUM" = "MEDIUM",
    "HIGH" = "HIGH",
    "VERY_HIGH" = "VERY_HIGH",
};

function parseSensitivity(sensitivity: String): Sensitivity {
    switch (sensitivity) {
        case "VERY_LOW":
            return Sensitivity.VERY_LOW
        case "LOW":
            return Sensitivity.LOW
        case "MEDIUM":
            return Sensitivity.MEDIUM
        case "HIGH":
            return Sensitivity.HIGH
        case "VERY_HIGH":
            return Sensitivity.VERY_HIGH
        default:
            return Sensitivity.MEDIUM
    }
}

function Settings() {
    const [everyoneCanBecomeAdmin, setEveryoneCanBecomeAdmin] = useState(true);
    const [language, setLanguage] = useState<string>("en");
    const [sensibility, setSensibility] = useState<Sensitivity>(Sensitivity.MEDIUM);

    const changeEveryoneCanBecomeAdmin = (event: ChangeEvent<HTMLInputElement>) => {
        setEveryoneCanBecomeAdmin(event.target.checked);
    };

    const handleLanguageChange = (event: SelectChangeEvent) => {
        setLanguage(event.target.value as string);
    };

    const handleSensibilityChange = (event: SelectChangeEvent) => {
        const sensitivity = event.target.value as Sensitivity;
        console.log("sensitivity: ", sensitivity);
        setSensibility(sensitivity)

        const url = `http://${window.location.hostname}:8080/api/settings/sensitivity`;
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({sensitivity: sensitivity.toString()}),
        })
            .then((response) => {
                console.log(response);
                if (!response.ok) throw new Error('Failed to set sensitivity');
            })
            .then((result) => {
                console.log('Success:', result);
            })
            .catch((error) => {
                console.error('Error:', error);
            });

    }

    async function fetchSensibility(): Promise<Sensitivity> {
        const response = await fetch(`http://${window.location.hostname}:8080/api/settings/sensitivity`)
        const jsonData: { sensitivity: string } = await response.json()
        return parseSensitivity(jsonData.sensitivity)
    }

    useEffect(() => {
        fetchSensibility().then(result => setSensibility(result)).catch(console.error)
    }, []);

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
                    <FormControl sx={{m: 1, minWidth: 80}}>
                        <InputLabel id="settings-language-select-label">Language</InputLabel>
                        <Select
                            className="text-right"
                            labelId="settings-language-select-label"
                            id="settings-language-select"
                            value={language}
                            label="Language"
                            onChange={handleLanguageChange}
                            disabled={true}
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
                    <FormControl sx={{m: 1, minWidth: 80}}>
                        <InputLabel id="settings-sensibility-select-label">Sensibility</InputLabel>
                        <Select
                            className="text-right"
                            labelId="settings-sensibility-select-label"
                            id="settings-sensibility-select"
                            value={sensibility.valueOf()}
                            label="Sensibility"
                            onChange={handleSensibilityChange}
                        >
                            <MenuItem key={"sensitivity-VERY_LOW"} value={Sensitivity.VERY_LOW.valueOf()}>Very
                                Low</MenuItem>
                            <MenuItem key={"sensitivity-LOW"} value={Sensitivity.LOW.valueOf()}>Low</MenuItem>
                            <MenuItem key={"sensitivity-MEDIUM"} value={Sensitivity.MEDIUM.valueOf()}>Medium</MenuItem>
                            <MenuItem key={"sensitivity-HIGH"} value={Sensitivity.HIGH.valueOf()}>High</MenuItem>
                            <MenuItem key={"sensitivity-VERY_HIGH"} value={Sensitivity.VERY_HIGH.valueOf()}>Very
                                High</MenuItem>
                        </Select>
                    </FormControl>
                </ListItem>

            </List>

        </Box>)
}

export default Settings