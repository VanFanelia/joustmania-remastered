import BatteryUnknownIcon from '@mui/icons-material/BatteryUnknown';
import Battery0BarIcon from '@mui/icons-material/Battery0Bar';
import Battery20Icon from '@mui/icons-material/Battery20';
import Battery50Icon from '@mui/icons-material/Battery50';
import Battery80Icon from '@mui/icons-material/Battery80';
import BatteryFullIcon from '@mui/icons-material/BatteryFull';
import {AkkuState} from '../dto/HardwareDTOs.tsx';
import {BatteryCharging50, BatteryChargingFull} from '@mui/icons-material';

export function getMoveColor(isAdmin: boolean, isConnected: boolean): string {
    if (!isConnected) {
        return "#bdbdbd"
    } else if (isAdmin) {
        return "#7b00ff"
    } else {
        return "#ffa500"
    }
}

export function getAkkuIcon(akku: AkkuState) {
    switch (akku) {
        case AkkuState.UNKNOWN:
            return <BatteryUnknownIcon fontSize={"large"} color={"disabled"}/>;
        case AkkuState.LEVEL_0:
            return <Battery0BarIcon fontSize={"large"}/>;
        case AkkuState.LEVEL_1:
            return <Battery20Icon fontSize={"large"}/>;
        case AkkuState.LEVEL_2:
        case AkkuState.LEVEL_3:
            return <Battery50Icon fontSize={"large"}/>;
        case AkkuState.LEVEL_4:
            return <Battery80Icon fontSize={"large"}/>;
        case AkkuState.LEVEL_5:
            return <BatteryFullIcon fontSize={"large"}/>;
        case AkkuState.CHARGING:
            return <BatteryCharging50 fontSize={"large"}/>;
        case AkkuState.CHARGING_DONE:
            return <BatteryChargingFull fontSize={"large"}/>;
    }
}