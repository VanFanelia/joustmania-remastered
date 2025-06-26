import {Navigate, Route, Routes} from 'react-router-dom'
import Game from './pages/Game'
import Hardware from './pages/Hardware'
import Debug from './pages/Debug'
import './App.css'
import SimpleBottomNavigation from "./components/SimpleBottomNavigation.tsx";
import Settings from './pages/Settings.tsx'
import {SettingsProvider} from "./context/SettingsProvider.tsx";
import {BluetoothProvider} from "./context/BluetoothProvider.tsx";
import {GameStatsProvider} from "./context/GameStatsProvider.tsx";
import {PSMoveStubStatisticsProvider} from "./context/PSMoveStubStatisticsProvider.tsx";

function App() {
    return (
        <>
            <SettingsProvider>
                <BluetoothProvider>
                    <GameStatsProvider>
                        <PSMoveStubStatisticsProvider>
                            <Routes>
                                <Route path="/" element={<Navigate to="/game"/>}/>
                                <Route path="/game" element={<Game/>}/>
                                <Route path="/settings" element={<Settings/>}/>
                                <Route path="/hardware" element={<Hardware/>}/>
                                <Route path="/debug" element={<Debug/>}/>
                            </Routes>
                            <SimpleBottomNavigation/>
                        </PSMoveStubStatisticsProvider>
                    </GameStatsProvider>
                </BluetoothProvider>
            </SettingsProvider>
        </>
    )
}

export default App
