import {Route, Routes} from 'react-router-dom'
import Game from './pages/Game'
import Hardware from './pages/Hardware'
import Debug from './pages/Debug'
import './App.css'
import SimpleBottomNavigation from "./components/SimpleBottomNavigation.tsx";
import Settings from './pages/Settings.tsx'

function App() {
    return (
        <>
            <Routes>
                <Route path="/game" element={<Game/>}/>
                <Route path="/settings" element={<Settings/>}/>
                <Route path="/hardware" element={<Hardware/>}/>
                <Route path="/debug" element={<Debug/>}/>
            </Routes>
            <SimpleBottomNavigation/>
        </>
    )
}

export default App
