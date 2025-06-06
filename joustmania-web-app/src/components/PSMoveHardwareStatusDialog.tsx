import * as React from 'react';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import {useBluetoothContext} from "../context/BluetoothProvider.tsx";
import {
    BlueToothControllerStats,
    MacAddress,
    MotionControllerStatsWithAdapterInfo,
    toAkkuState,
    toAkkuStateLabel
} from "../dto/HardwareDTOs.tsx";
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemAvatar from '@mui/material/ListItemAvatar';
import Avatar from '@mui/material/Avatar';
// @ts-ignore
import PSMoveControllerIcon from '../assets/PSMoveController.svg?react';
import {getAkkuIcon, getMoveColor} from './hardwareHelper.utils.tsx';
import ListItemText from '@mui/material/ListItemText';
import Box from '@mui/material/Box';
import {blinkMoveController, rumbleMoveController} from "../api/hardware.api.client.ts";
import {ApiStatus} from "../api/api.definitions.tsx";
import {Alert, Divider} from "@mui/material";
import {useState} from "react";

interface PSMoveHardwareStatusDialogProps {
    controller: MacAddress | null,
    isOpen: boolean,
    onClose: () => void
}

function getStatsOfController(blueToothStats: BlueToothControllerStats[], macAddress: MacAddress): MotionControllerStatsWithAdapterInfo | null {
    const stats = blueToothStats.map((blueToothControllerStat) =>
        blueToothControllerStat.pairedMotionController.map((moveStats) => new MotionControllerStatsWithAdapterInfo(
            moveStats, blueToothControllerStat.adapterId, blueToothControllerStat.macAddress, blueToothControllerStat.name
        ))
    ).flat()

    return stats.find((value) => value.motionController.macAddress == macAddress) ?? null
}


export function PSMoveHardwareStatusDialog({controller, isOpen, onClose}: PSMoveHardwareStatusDialogProps) {
    const [error, setError] = useState<string | null>(null);
    const [showAlert, setShowAlert] = useState(false);

    const bluetoothDevices = useBluetoothContext();

    if (controller == null) {
        return
    }

    const move = getStatsOfController(bluetoothDevices, controller)

    if (move == null) {
        return
    }

    const akkuState = toAkkuState(move.motionController.batteryLevel);
    const akkuStateLabel = toAkkuStateLabel(akkuState)

    function callBlinkMoveController(macAddress: MacAddress) {
        blinkMoveController(macAddress).then((result) => {
            if (result.status == ApiStatus.ERROR) {
                setShowAlert(true)
                setError(result.reason)
            }
        })
    }

    function callRumbleMoveController(macAddress: MacAddress) {
        rumbleMoveController(macAddress).then((result) => {
            if (result.status == ApiStatus.ERROR) {
                setShowAlert(true)
                setError(result.reason)
            }
        })
    }

    return (
        <React.Fragment>
            <Dialog
                open={isOpen}
                onClose={onClose}
                aria-labelledby="alert-dialog-title"
                aria-describedby="alert-dialog-description"
                fullWidth={true}
            >
                <DialogTitle id="alert-dialog-title">
                    <Box className={"flex flex-row justify-center align-middle"}>
                        <Avatar className={"mr-4"}>
                            <PSMoveControllerIcon width={32} height={32} style={{
                                color: getMoveColor(move.motionController.isAdmin ?? false, move.motionController.connected ?? false),
                                transform: "rotate(20deg)",
                                opacity: move.motionController.connected ? 1 : 0.333
                            }}/>
                        </Avatar>
                        <span style={{height: 30, alignSelf: "center"}}>
                            {move.motionController.macAddress}
                        </span>
                    </Box>
                </DialogTitle>
                <DialogContent>
                    <List className={"w-full"} sx={{bgcolor: 'background.paper'}}>
                        <ListItem className="w-full">
                            <ListItemAvatar>
                                <Avatar className={"text-black"}>
                                    {getAkkuIcon(akkuState)}
                                </Avatar>
                            </ListItemAvatar>
                            <ListItemText primary={akkuStateLabel}/>
                        </ListItem>


                        <ListItem className="w-full">
                            <ListItemAvatar>
                                <Avatar>
                                    <PSMoveControllerIcon width={32} height={32} style={{
                                        color: getMoveColor(move.motionController.isAdmin ?? false, move.motionController.connected ?? false),
                                        transform: "rotate(20deg)",
                                        opacity: move.motionController.connected ? 1 : 0.333
                                    }}/>
                                </Avatar>
                            </ListItemAvatar>
                            <ListItemText
                                primary={`Status: ${(move.motionController.connected ? (move.motionController.isAdmin ? "Admin" : "User") : "disconnected")}`}/>
                        </ListItem>
                        <Divider component="li" className={"pt-4"}/>
                        <ListItem className={"mt-6"}>
                            <Box className={"flex w-full"} sx={{justifyContent: "space-around"}}>

                                <Button variant={"contained"} color={"primary"} sx={{
                                    minWidth: "96px",
                                    background: "linear-gradient(90deg, red, orange, yellow, green, blue, indigo, violet)",
                                    transition: "background-position 0.5s ease",
                                    backgroundSize: "100%",
                                    textShadow: "2px 2px 4px rgba(0, 0, 0, 0.9);"

                                }}
                                        onClick={() => {
                                            callBlinkMoveController(move.motionController.macAddress)
                                        }}>Blink</Button>

                                <Button variant={"contained"} color={"primary"} sx={{minWidth: "96px"}}
                                        onClick={() => {
                                            callRumbleMoveController(move.motionController.macAddress)
                                        }}>Rumble</Button>
                            </Box>
                        </ListItem>
                    </List>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>OK</Button>
                </DialogActions>
            </Dialog>
            {showAlert && (
                <Alert severity="error" className={"absolute bottom-16"}>{error}</Alert>
            )}
        </React.Fragment>
    );
}