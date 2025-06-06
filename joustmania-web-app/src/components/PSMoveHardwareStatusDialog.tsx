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
import {ListItemButton} from "@mui/material";
import {blinkMoveController} from "../api/hardware.api.client.ts";
import {ApiStatus} from "../api/api.definitions.tsx";

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
                //setShowAlert(true)
                //setError(result.reason)
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

                        <ListItem>
                            <ListItemButton className={"flex w-full"} sx={{justifyContent: "center"}}>
                                <Button variant={"contained"} color={"primary"}
                                        onClick={() => {
                                            callBlinkMoveController(move.motionController.macAddress)
                                        }}>Blink
                                    Rainbow</Button>
                            </ListItemButton>
                        </ListItem>
                    </List>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>OK</Button>
                </DialogActions>
            </Dialog>
        </React.Fragment>
    );
}