import Box from '@mui/material/Box';
import {useState} from "react";
import {
    Accordion,
    AccordionDetails,
    AccordionSummary,
    Avatar,
    IconButton,
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
import {useBluetoothContext} from "../context/BluetoothProvider.tsx";
import {toAkkuState} from '../dto/HardwareDTOs.tsx';
import { MoreVert} from '@mui/icons-material';
import {PSMoveHardwareStatusDialog} from "../components/PSMoveHardwareStatusDialog.tsx";
import {getAkkuIcon, getMoveColor} from "../components/hardwareHelper.utils.tsx";

function Hardware() {
    const bluetoothDevices = useBluetoothContext();
    const [openControllerDialog, setOpenControllerDialog] = useState<boolean>(false);
    const [lastSelectedMacAddress, setLastSelectedMacAddress] = useState<string | null>(null);

    function showControllerDialog(macAddress: string) {
        setOpenControllerDialog(true)
        setLastSelectedMacAddress(macAddress)
    }

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
                                        className="text-right font-bold pr-8">{getAkkuIcon(toAkkuState(controller.batteryLevel))}
                                    </Box>
                                    <Box className="text-right font-bold">
                                        <IconButton color="primary" aria-label="more settings"
                                                    onClick={() => {
                                                        showControllerDialog(controller.macAddress)
                                                    }}>
                                            <MoreVert fontSize="large"/>
                                        </IconButton>
                                    </Box>
                                </ListItem>
                            ))}
                        </List>

                    </AccordionDetails>
                </Accordion>
            ))}

            <PSMoveHardwareStatusDialog isOpen={openControllerDialog}
                                        controller={lastSelectedMacAddress}
                                        onClose={() => {
                                            setOpenControllerDialog(false)
                                        }}
            />
        </Box>)
}

export default Hardware