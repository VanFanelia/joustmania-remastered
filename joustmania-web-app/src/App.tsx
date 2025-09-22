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
import {ThreadProvider} from "./context/DebugThreadProvider.tsx";
import MainControl from "./pages/MainControl.tsx";

function App() {
    return (
        <>
            <ThreadProvider>
                <SettingsProvider>
                    <BluetoothProvider>
                        <GameStatsProvider>
                            <PSMoveStubStatisticsProvider>
                                <Routes>
                                    <Route path="/" element={<Navigate to="/main"/>}/>
                                    <Route path="/main" element={<MainControl/>}/>
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
            </ThreadProvider>
        </>
    )
}

export default App
