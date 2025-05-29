import Box from '@mui/material/Box';
import {SyntheticEvent, useState} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Avatar,
    List,
    ListItem,
    ListItemAvatar,
    ListItemText,
    Typography
} from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import BluetoothSearchingIcon from '@mui/icons-material/BluetoothSearching';
// @ts-ignore
import PSMoveControllerIcon from '../assets/PSMoveController.svg?react';
import BatteryUnknownIcon from '@mui/icons-material/BatteryUnknown';
import Battery0BarIcon from '@mui/icons-material/Battery0Bar';
import Battery20Icon from '@mui/icons-material/Battery20';
import Battery50Icon from '@mui/icons-material/Battery50';
import Battery80Icon from '@mui/icons-material/Battery80';
import BatteryFullIcon from '@mui/icons-material/BatteryFull';
import {useBluetoothContext} from "../context/BluetoothProvider.tsx";
import {AkkuState, toAkkuState} from '../dto/HardwareDTOs.tsx';
import {BatteryCharging50, BatteryChargingFull} from '@mui/icons-material';

function Hardware() {

    function getMoveColor(isAdmin: boolean, isConnected: boolean): string {
        if (!isConnected) {
            return "#bdbdbd"
        } else if (isAdmin) {
            return "#7b00ff"
        } else {
            return "#ffa500"
        }
    }

    function getAkkuIcon(akku: AkkuState) {
        switch (akku) {
            case AkkuState.UNKNOWN:
                return <BatteryUnknownIcon color={"disabled"}/>;
            case AkkuState.LEVEL_0:
                return <Battery0BarIcon/>;
            case AkkuState.LEVEL_1:
                return <Battery20Icon/>;
            case AkkuState.LEVEL_2:
            case AkkuState.LEVEL_3:
                return <Battery50Icon/>;
            case AkkuState.LEVEL_4:
                return <Battery80Icon/>;
            case AkkuState.LEVEL_5:
                return <BatteryFullIcon/>;
            case AkkuState.CHARGING:
                return <BatteryCharging50/>;
            case AkkuState.CHARGING_DONE:
                return <BatteryChargingFull/>;
        }
    }

    // @ts-ignore
    const [showOnlyConnectedController, setShowOnlyConnectedController] = useState<boolean>(false);

    // @ts-ignore
    const [blueToothAdapterAccordionExpanded, setBlueToothAdapterAccordionExpanded] = useState<string | false>(false);

    // @ts-ignore
    const handleAccordionBlueToothAdapterChange =
        (panel: string) => (_event: SyntheticEvent, isExpanded: boolean) => {
            setBlueToothAdapterAccordionExpanded(isExpanded ? panel : false);
        };

    const bluetoothDevices = useBluetoothContext();

    return (
        <Box className="rootPage p-4 scroll-auto mb-14">
            {bluetoothDevices.map((adapter, index) => (
                <Accordion key={"BluetoothAdapterAccordionKey" + index}>
                    <AccordionSummary
                        className="flex items-center align-middle"
                        expandIcon={<ExpandMoreIcon/>}
                        aria-controls="panel1bh-content"
                        id="panel1bh-header"
                    >
                        <Avatar className="mr-4">
                            <BluetoothSearchingIcon style={{color: "#000"}}/>
                        </Avatar>
                        <Typography component="span" className="self-center"
                                    style={{fontSize: "1.1rem", fontWeight: "bold"}}>
                            {`${adapter.name} (${adapter.adapterId} - ${adapter.macAddress})`}
                        </Typography>
                    </AccordionSummary>
                    <AccordionDetails>

                        <List className={"w-full"} sx={{bgcolor: 'background.paper'}}>
                            {adapter.pairedMotionController.map((controller, index) => (
                                <ListItem key={"BluetoothAdapterControllerKey" + index} className="w-full">
                                    <ListItemAvatar>
                                        <Avatar>
                                            <PSMoveControllerIcon width={32} height={32} style={{
                                                color: getMoveColor(controller.isAdmin ?? false, controller.connected ?? false),
                                                transform: "rotate(20deg)",
                                                opacity: controller.connected ? 1 : 0.333
                                            }}/>
                                        </Avatar>
                                    </ListItemAvatar>
                                    <ListItemText primary={controller.macAddress}
                                                  secondary={controller.connected ? (controller.isAdmin ? "Admin" : "User") : "disconnected"}/>
                                    <Box
                                        className="text-right font-bold">{getAkkuIcon(toAkkuState(controller.batteryLevel))}</Box>
                                </ListItem>
                            ))}
                        </List>

                    </AccordionDetails>
                </Accordion>
            ))}
        </Box>)
}

export default Hardware