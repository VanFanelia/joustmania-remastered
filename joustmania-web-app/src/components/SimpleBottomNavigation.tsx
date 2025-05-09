import * as React from 'react';
import BottomNavigation from '@mui/material/BottomNavigation';
import BottomNavigationAction from '@mui/material/BottomNavigationAction';
// @ts-ignore
import PSMoveIcon from '../assets/psmove.svg?react';
import {Paper} from "@mui/material";
import { Link } from 'react-router-dom';
import {DeveloperMode, Settings, SportsEsports} from '@mui/icons-material';

export default function SimpleBottomNavigation() {
    const [value, setValue] = React.useState(0);

    return (
        <Paper sx={{position: 'fixed', bottom: 0, left: 0, right: 0}} elevation={3}>
            <BottomNavigation
                showLabels
                value={value}
                onChange={(_, newValue) => {
                    setValue(newValue);
                }}
            >
                <BottomNavigationAction label="Game" icon={<SportsEsports/>} component={Link} to="/game"/>
                <BottomNavigationAction label="Settings" icon={<Settings/>} component={Link} to="/settings"/>
                <BottomNavigationAction label="Hardware" icon={<PSMoveIcon width={32} height={32} />} component={Link} to="/hardware"/>
                <BottomNavigationAction label="Debug" icon={<DeveloperMode/>} component={Link} to="/debug"/>
            </BottomNavigation>
        </Paper>
    );
}