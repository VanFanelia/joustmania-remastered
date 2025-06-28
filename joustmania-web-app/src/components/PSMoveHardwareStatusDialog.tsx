import * as React from 'react';
import {useState} from 'react';
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
import ListItem from '@mui/material/ListItem';
import Avatar from '@mui/material/Avatar';
// @ts-ignore
import PSMoveControllerIcon from '../assets/PSMoveController.svg?react';
import {getAkkuIcon, getMoveColor} from './hardwareHelper.utils.tsx';
import Box from '@mui/material/Box';
import {blinkMoveController, rumbleMoveController} from "../api/hardware.api.client.ts";
import {ApiStatus} from "../api/api.definitions.tsx";
import {Alert, Divider, List} from "@mui/material";
import TimerIcon from '@mui/icons-material/Timer';
import BarChartIcon from '@mui/icons-material/BarChart';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import RunningWithErrorsIcon from '@mui/icons-material/RunningWithErrors';
import SettingsEthernetIcon from '@mui/icons-material/SettingsEthernet';
import {usePSMoveStubStatisticsContext} from "../context/PSMoveStubStatisticsProvider.tsx";
import {Duration} from "luxon";

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
    const moveStatistics = usePSMoveStubStatisticsContext()

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

    function formatDuration(ms: number): string {
        const duration = Duration.fromMillis(ms).shiftTo("hours", "minutes");

        const hours = Math.floor(duration.hours);
        const minutes = Math.floor(duration.minutes);

        if (hours >= 1) {
            return `${hours} hour ${minutes} min`;
        } else {
            return `${minutes} min`;
        }
    }

    const statistic = moveStatistics[move.motionController.macAddress] ?? null

    const uptime = statistic == null ? "?" : formatDuration(Date.now() - statistic.firstPoll)
    const pollCount = statistic == null ? "?" : statistic.pollCount
    const averagePollTime = statistic == null ? -1 : statistic.averagePollTime
    const longPollingDistanceCounter = statistic == null ? "?" : statistic.longPollingDistanceCounter
    const longestPollGap = statistic == null ? "?" : statistic.longestPollingGap

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
                    <Box className="w-full flex flex-row mb-4">
                        <Box className="w-1/2">
                            <div className={"flex flex-row justify-start align-middle"}>
                                <Avatar>
                                    <PSMoveControllerIcon width={32} height={32} style={{
                                        color: getMoveColor(move.motionController.isAdmin ?? false, move.motionController.connected ?? false),
                                        transform: "rotate(20deg)",
                                        opacity: move.motionController.connected ? 1 : 0.333
                                    }}/>
                                </Avatar>
                                <p className="h-fit self-center ml-4">{`Status: ${(move.motionController.connected ? (move.motionController.isAdmin ? "Admin" : "User") : "disconnected")}`} </p>
                            </div>
                        </Box>

                        <Box className="w-1/2">
                            <div className={"flex flex-row justify-start align-middle"}>
                                <Avatar className={"text-black"}>
                                    {getAkkuIcon(akkuState)}
                                </Avatar>
                                <p className="h-fit self-center ml-4">{akkuStateLabel}</p>
                            </div>
                        </Box>
                    </Box>

                    <Box className="w-full flex flex-col">
                        <Box className="mb-4">
                            <div className={"flex flex-row justify-start align-middle"}>
                                <Avatar className={"text-black"}>
                                    <AccessTimeIcon width={32} height={32} style={{color: "#000000"}}/>
                                </Avatar>
                                <p className="h-fit w-full self-center ml-4 flex justify-between"><span>Controller Uptime</span><span>{uptime}</span>
                                </p>
                            </div>
                        </Box>

                        <Box className="mb-4">
                            <div className={"flex flex-row justify-start align-middle"}>
                                <Avatar className={"text-black"}>
                                    <BarChartIcon width={32} height={32} style={{color: "#000000"}}/>
                                </Avatar>
                                <p className="h-fit w-full self-center ml-4 flex justify-between">
                                    <span>Poll Count</span><span>{pollCount}</span></p>
                            </div>
                        </Box>

                        <Box className="mb-4">
                            <div className={"flex flex-row justify-start align-middle"}>
                                <Avatar className={"text-black"}>
                                    <TimerIcon width={32} height={32} style={{color: "#000000"}}/>
                                </Avatar>
                                <p className="h-fit w-full self-center ml-4 flex justify-between"><span>Average Poll time</span><span>{Math.round(averagePollTime)} ms</span>
                                </p>
                            </div>
                        </Box>

                        <Box className="mb-4">
                            <div className={"flex flex-row justify-start align-middle"}>
                                <Avatar className={"text-black"}>
                                    <RunningWithErrorsIcon width={32} height={32} style={{color: "#000000"}}/>
                                </Avatar>
                                <p className="h-fit w-full self-center ml-4 flex justify-between"><span>Amount of High Delay</span><span>{longPollingDistanceCounter}</span>
                                </p>
                            </div>
                        </Box>

                        <Box className="mb-4">
                            <div className={"flex flex-row justify-start align-middle"}>
                                <Avatar className={"text-black"}>
                                    <SettingsEthernetIcon width={32} height={32} style={{color: "#000000"}}/>
                                </Avatar>
                                <p className="h-fit w-full self-center ml-4 flex justify-between">
                                    <span>Highest Delay</span><span>{longestPollGap}</span></p>
                            </div>
                        </Box>
                    </Box>

                    <List className={"w-full"} sx={{bgcolor: 'background.paper'}}>
                        <ListItem className="w-full">
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
            {
                showAlert && (
                    <Alert severity="error" className={"absolute bottom-16"}>{error}</Alert>
                )
            }
        </React.Fragment>
    )
        ;
}