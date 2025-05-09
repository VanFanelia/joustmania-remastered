import Box from '@mui/material/Box';
import {SyntheticEvent, useState} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary, Avatar,
    List,
    ListItem, ListItemAvatar, ListItemText,
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

enum AkkuState {
    UNKNOWN = "unknown",
    EMPTY = "empty",
    LOW = "low",
    MEDIUM = "medium",
    HIGH = "high",
    FULL = "full"
}

class BluetoothAdapter {
    name: string;
    controller: PSMoveController[];

    constructor(name: string, controller: PSMoveController[]) {
        this.name = name;
        this.controller = controller;
    }
}

class PSMoveController {
    macAddress: string;
    akku: AkkuState;
    isAdmin: boolean = false;
    isConnected: boolean = false;

    constructor(macAddress: string, akku: AkkuState, isAdmin: boolean = false, isConnected: boolean = false) {
        this.macAddress = macAddress;
        this.akku = akku;
        this.isAdmin = isAdmin;
        this.isConnected = isConnected;
    }
}

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
        switch(akku) {
            case AkkuState.UNKNOWN:
                return <BatteryUnknownIcon color={"disabled"}/>;
            case AkkuState.EMPTY:
                return <Battery0BarIcon />;
            case AkkuState.LOW:
                return <Battery20Icon />;
            case AkkuState.MEDIUM:
                return <Battery50Icon />;
            case AkkuState.HIGH:
                return <Battery80Icon />;
            case AkkuState.FULL:
                return <BatteryFullIcon />;
        }
    }

    const demoHardwareSetup: BluetoothAdapter[] = [
        new BluetoothAdapter("Internal Bluetooth", [
            new PSMoveController("00:11:22:33:44:55", AkkuState.FULL, true, true),
            new PSMoveController("66:77:88:99:AA:BB", AkkuState.MEDIUM, false, true),
            new PSMoveController("CC:DD:EE:FF:00:11", AkkuState.LOW, false, true),
            new PSMoveController("CC:DD:EE:FF:22:33", AkkuState.HIGH, false, true),
            new PSMoveController("22:33:44:55:66:77", AkkuState.UNKNOWN, false, false)
        ]),
        new BluetoothAdapter("Bluetooth Dongle", [
            new PSMoveController("11:22:33:44:55:66", AkkuState.MEDIUM, false, true),
            new PSMoveController("77:88:99:AA:BB:CC", AkkuState.EMPTY, true, false)
        ]),
    ]
    const [showOnlyConnectedController, setShowOnlyConnectedController] = useState<boolean>(false);

    const [blueToothAdapterAccordionExpanded, setBlueToothAdapterAccordionExpanded] = useState<string | false>(false);

    const handleAccordionBlueToothAdapterChange =
        (panel: string) => (event: SyntheticEvent, isExpanded: boolean) => {
            setBlueToothAdapterAccordionExpanded(isExpanded ? panel : false);
        };

    return (
        <Box className="rootPage p-4 scroll-auto mb-14">
            {demoHardwareSetup.map((adapter, index) => (
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
                            {adapter.name}
                        </Typography>
                    </AccordionSummary>
                    <AccordionDetails>

                        <List className={"w-full"} sx={{bgcolor: 'background.paper'}}>
                            {adapter.controller.map((controller, index) => (
                                <ListItem key={"BluetoothAdapterControllerKey" + index} className="w-full">
                                    <ListItemAvatar>
                                        <Avatar>
                                            <PSMoveControllerIcon width={32} height={32} style={{
                                                color: getMoveColor(controller.isAdmin, controller.isConnected),
                                                transform: "rotate(20deg)",
                                                opacity: controller.isConnected ? 1 : 0.333
                                            }}/>
                                        </Avatar>
                                    </ListItemAvatar>
                                    <ListItemText primary={controller.macAddress}
                                                  secondary={controller.isConnected ? (controller.isAdmin ? "Admin" : "User") : "disconnected"}/>
                                    <Box className="text-right font-bold">{getAkkuIcon(controller.akku)}</Box>
                                </ListItem>
                            ))}
                        </List>

                    </AccordionDetails>
                </Accordion>
            ))}
        </Box>)
}

export default Hardware