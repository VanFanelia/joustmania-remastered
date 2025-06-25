import Box from '@mui/material/Box';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import {Alert, Avatar, FormHelperText, ListItemAvatar, ListItemText, Select, Switch, TextField} from "@mui/material";
import LocalPoliceIcon from '@mui/icons-material/LocalPolice';
import * as React from "react";
import {ChangeEvent, useEffect, useState} from "react";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import RecordVoiceOverIcon from '@mui/icons-material/RecordVoiceOver';

import {SelectChangeEvent} from "@mui/material/Select";
import {SensibilityLevel, useSettingsContext} from "../context/SettingsProvider.tsx";
import {
    setGameOptionSortToddlerAmountOfRounds,
    setGameOptionSortToddlerRoundDuration,
    setLanguage,
    setSensitivity
} from "../api/settings.api.client.ts";
import {ApiStatus} from "../api/api.definitions.tsx";

const supportedLanguages: Map<string, string> = new Map<string, string>(
    [['en', "Englisch"], ['de', 'German']]
);

function Settings() {
    const [everyoneCanBecomeAdmin, setEveryoneCanBecomeAdmin] = useState(true);
    const [currentLanguage, setCurrentLanguage] = useState<string>("en");
    const [currentSensibility, setCurrentSensibility] = useState<SensibilityLevel>(SensibilityLevel.MEDIUM);
    const [sortToddlerRoundDuration, setSortToddlerRoundDuration] = useState(30);
    const [sortToddlerAmountOfRounds, setSortToddlerAmountOfRounds] = useState(10);

    const [error, setError] = useState<string | null>(null);
    const [showAlert, setShowAlert] = useState(false);

    const changeEveryoneCanBecomeAdmin = (event: ChangeEvent<HTMLInputElement>) => {
        setEveryoneCanBecomeAdmin(event.target.checked);
    };

    const handleLanguageChange = (event: SelectChangeEvent) => {
        const languageKey = event.target.value as string;
        setCurrentLanguage(languageKey);
        setLanguage(languageKey).then((result) => {
            if (result.status == ApiStatus.ERROR) {
                setShowAlert(true)
                setError(result.reason)
            }
        })

    };

    const handleSensibilityChange = (event: SelectChangeEvent) => {
        const sensibility = event.target.value as SensibilityLevel;
        setCurrentSensibility(sensibility)
        setSensitivity(sensibility.toString()).then((result) => {
            if (result.status == ApiStatus.ERROR) {
                setShowAlert(true)
                setError(result.reason)
            }
        })
    }

    const config = useSettingsContext();

    useEffect(() => {
        if (config !== null) {
            setCurrentSensibility(config.sensibility)
            setCurrentLanguage(config.language.toLowerCase())
        }
    }, [config])

    const hasRoundDurationError = (roundDuration: any) => !roundDuration || (roundDuration < 10 || roundDuration > 180)
    const hasAmountOfRoundsError = (amountOfRounds: any) => !amountOfRounds || (amountOfRounds < 1 || amountOfRounds > 20)

    const handleSortToddlerRoundDurationChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const roundDuration = parseInt(event.target.value as string)
        setSortToddlerRoundDuration(roundDuration)
        if (!hasRoundDurationError(roundDuration)) {
            setGameOptionSortToddlerRoundDuration(roundDuration).then((result) => {
                if (result.status == ApiStatus.ERROR) {
                    setShowAlert(true)
                    setError(result.reason)
                }
            })
        }
    }

    const handleSortToddlerAmountOfRounds = (event: React.ChangeEvent<HTMLInputElement>) => {
        const amountOfRounds = parseInt(event.target.value as string)
        setSortToddlerAmountOfRounds(amountOfRounds)
        if (!hasAmountOfRoundsError(amountOfRounds)) {
            setGameOptionSortToddlerAmountOfRounds(amountOfRounds).then((result) => {
                if (result.status == ApiStatus.ERROR) {
                    setShowAlert(true)
                    setError(result.reason)
                }
            })
        }
    }

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
                            value={currentLanguage}
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
                            value={currentSensibility.valueOf()}
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
                <ListItem>
                    <h2 className="mb-2 text-2xl">Sort Toddler Game Settings</h2>
                </ListItem>
                <ListItem className="w-full flex justify-between">
                    <ListItemAvatar>
                        <Avatar>
                            <RecordVoiceOverIcon style={{color: "#000"}}/>
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Round Duration in S"/>
                    <FormControl sx={{m: 1, maxWidth: 120}}>
                        <TextField
                            error={hasRoundDurationError(sortToddlerRoundDuration)}
                            variant="outlined"
                            className="text-right"
                            id="settings-sensibility-select"
                            value={sortToddlerRoundDuration}
                            onChange={handleSortToddlerRoundDurationChange}
                            type="number"
                            label="Duration"
                            aria-describedby="component-error-text"
                        />
                        {
                            hasRoundDurationError(sortToddlerRoundDuration) && (
                                <FormHelperText id="component-error-text" sx={{"color": "var(--color-red-500)"}}>10s -
                                    180s</FormHelperText>
                            )
                        }
                    </FormControl>
                </ListItem>
                <ListItem className="w-full flex justify-between">
                    <ListItemAvatar>
                        <Avatar>
                            <RecordVoiceOverIcon style={{color: "#000"}}/>
                        </Avatar>
                    </ListItemAvatar>
                    <ListItemText primary="Number of Rounds"/>
                    <FormControl sx={{m: 1, maxWidth: 120}}>
                        <TextField
                            error={hasAmountOfRoundsError(sortToddlerAmountOfRounds)}
                            variant="outlined"
                            className="text-right"
                            id="settings-sensibility-select"
                            value={sortToddlerAmountOfRounds}
                            onChange={handleSortToddlerAmountOfRounds}
                            type="number"
                            label="Rounds"
                        />
                        {
                            hasAmountOfRoundsError(sortToddlerAmountOfRounds) && (
                                <FormHelperText id="component-error-text" sx={{"color": "var(--color-red-500)"}}>1 -
                                    20</FormHelperText>
                            )
                        }
                    </FormControl>
                </ListItem>
            </List>

            {showAlert && (
                <Alert severity="error" className={"absolute bottom-16"}>{error}</Alert>
            )}
        </Box>
    )
}

export default Settings