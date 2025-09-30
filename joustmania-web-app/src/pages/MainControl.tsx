import Box from "@mui/material/Box";
import {
    Alert,
    Avatar,
    Button,
    Card,
    CardActions,
    CardContent,
    CardMedia,
    Slider,
    Stack,
    Typography
} from "@mui/material";
import freeForAllImage from '../assets/free-for-all.banner.full.png';
import sortingToddlerImage from '../assets/sorting-toddler.banner.full.png';
import werewolfImage from '../assets/werewolf.banner.full.png';
import zombieImage from '../assets/zombie.banner.full.png';
import protectTheKingImage from '../assets/protect-the-king.banner.full.png';
import ninjaBombImage from '../assets/ninja-bomb.banner.full.png';
import shakeNRunImage from '../assets/shake-n-run.banner.full.png';
import Diversity3Icon from "@mui/icons-material/Diversity3";
// @ts-expect-error the auto transform into a React component seems to be an error for the linter
import SkullsAndBonesIcon from '../assets/skulls-and-bones.svg?react';
// @ts-expect-error he auto transform into a React component seems to be an error for the linter
import PSMoveControllerIcon from '../assets/PSMoveController.svg?react';
import ArrowBackIosNewIcon from '@mui/icons-material/ArrowBackIosNew';
import {ArrowForwardIos, MusicNote, SurroundSound, VolumeDown, VolumeUp} from "@mui/icons-material";
import {useEffect, useState} from "react";
import {useGameStatsContext} from "../context/GameStatsProvider.tsx";
import {forceStartGame, forceStopGame} from "../api/game.api.client.ts";
import {ApiStatus} from "../api/api.definitions.tsx";
import AbortGameButtonWithDialog from "../components/AbortGameButtonWithDialog.tsx";
import {useBluetoothContext} from "../context/BluetoothProvider.tsx";
import {GameMode, getDisplayName} from "../util/gameConstants.tsx";
import {useSettingsContext} from "../context/SettingsProvider.tsx";
import {setGlobalVolume, setMusicVolume} from "../api/settings.api.client.ts";

function MainControl() {
    const SLIDER_MAX = 100;
    const SLIDER_MIN = 0;
    const sliderMarks = [
        {
            value: SLIDER_MIN,
            label: '',
        },
        {
            value: SLIDER_MAX,
            label: '',
        },
    ];

    const [activePlayer, setActivePlayer] = useState<number>(0);
    const [connectedController, setConnectedController] = useState<number>(0);

    const bluetoothDevices = useBluetoothContext();

    useEffect(() => {
        setConnectedController(bluetoothDevices.reduce((previousValue, bluetoothController) => {
            return previousValue + bluetoothController.pairedMotionController.filter(controller => controller.connected).length
        }, 0))

    }, [bluetoothDevices])

    const [currentGame, setCurrentGame] = useState<string>(GameMode.FREE_FOR_ALL);
    const [gameState, setGameState] = useState<string>("Lobby");

    const gameStats = useGameStatsContext();

    useEffect(() => {
        setGameState(gameStats.currentGameState);
        setActivePlayer(gameStats.activeController.length)
        setCurrentGame(gameStats.selectedGame)
    }, [gameStats]);

    const [error, setError] = useState<string | null>(null);
    const [showAlert, setShowAlert] = useState(false);

    useEffect(() => {
        if (showAlert) {
            const timer = setTimeout(() => {
                setShowAlert(false);
            }, 10000); // 10 Sekunden
            return () => clearTimeout(timer); // Aufräumen, falls Alert früher verschwindet
        }
    }, [showAlert]);

    function callForceStopGame() {
        forceStopGame().then((result) => {
            if (result.status == ApiStatus.ERROR) {
                setShowAlert(true)
                setError(result.reason)
            }
        })
    }

    const [currentMusicVolume, setCurrentMusicVolume] = useState<number>(50);
    const [currentGlobalVolume, setCurrentGlobalVolume] = useState<number>(50);

    const config = useSettingsContext();

    useEffect(() => {
        if (config !== null) {
            setCurrentMusicVolume(config.musicVolume)
            setCurrentGlobalVolume(config.globalVolume)
        }
    }, [config])

    const handleMusicVolumeChange = (_: Event, newValue: number) => {
        setCurrentMusicVolume(newValue);
        setMusicVolume(newValue).then((result) => {
            if (result.status == ApiStatus.ERROR) {
                setShowAlert(true)
                setError(result.reason)
            }
        })
    };

    const handleGlobalVolumeChange = (_: Event, newValue: number) => {
        setCurrentGlobalVolume(newValue);
        setGlobalVolume(newValue).then((result) => {
            if (result.status == ApiStatus.ERROR) {
                setShowAlert(true)
                setError(result.reason)
            }
        })
    };

    const isGameRunning = gameState != "Lobby";
    const headline =
        isGameRunning ? "Lobby: Waiting for game selection" : `Game running: ${currentGame}`;

    return (
        <Box className="rootPage p-4 scroll-auto mb-14">
            <Box className="flex flex-col justify-between"
                 sx={{minWidth: 768, height: 'calc(100vh - 56px)', overflow: 'hidden'}}>
                <div className="flex flex-row justify-between items-center">
                    <Typography gutterBottom variant="h5" component="span" sx={{minWidth: 200}}>
                        {headline}
                    </Typography>

                    <Typography gutterBottom variant="subtitle1" component="span" sx={{minWidth: 200}}>
                        <div className={"flex flex-row gap-2 items-center"}>
                            {connectedController} <Diversity3Icon/>
                            <span className="mr-8 ml-8">|</span>
                            {activePlayer} <PSMoveControllerIcon width={32} height={32} style={{
                            color: "#ffa500",
                            transform: "rotate(20deg)",
                            opacity: 1
                        }}/>
                            <span className="mr-8 ml-8">|</span>
                            <SkullsAndBonesIcon alt="Skulls and Bones" width={32} height={32}/>
                            {gameStats.playerLost.length}
                        </div>

                    </Typography>

                    <div style={{minWidth: 200}}>
                        <AbortGameButtonWithDialog disabled={!isGameRunning} onConfirm={callForceStopGame}/>
                    </div>
                </div>
                <div style={{flex: 1, minHeight: 0, display: 'flex'}}>
                    <SelectGameSlider onGameStartError={(reason: string) => {
                        setShowAlert(true)
                        setError(reason)
                    }}/>
                </div>
                <div className="flex flex-row justify-between p-2 m-2">
                    <div className="flex flex-row justify-between gap-12">
                        <div className="flex flex-row gap-2 justify-between self-center">
                            <Avatar>
                                <SurroundSound style={{color: "#000"}}/>
                            </Avatar>
                        </div>
                        <div className="flex flex-col gap-2">
                            <Typography gutterBottom variant="subtitle1" component="div" sx={{marginBottom: 0}}>
                                Global Volume
                            </Typography>
                            <Stack spacing={2} direction="row"
                                   sx={{alignItems: 'center', mb: 1, minWidth: 200, marginBottom: 0}}>
                                <VolumeDown style={{color: "#000"}}/>
                                <Slider aria-label="Global" marks={sliderMarks} step={5} value={currentGlobalVolume}
                                        onChange={handleGlobalVolumeChange}/>
                                <VolumeUp style={{color: "#000"}}/>
                            </Stack>
                        </div>
                    </div>

                    <div className="flex flex-row justify-between gap-12">
                        <div className="flex flex-col gap-2">
                            <Typography gutterBottom variant="subtitle1" component="div" sx={{marginBottom: 0}}>
                                Music Volume
                            </Typography>
                            <Stack spacing={2} direction="row"
                                   sx={{alignItems: 'center', mb: 1, minWidth: 200, marginBottom: 0}}>
                                <VolumeDown style={{color: "#000"}}/>
                                <Slider aria-label="Global" marks={sliderMarks} step={5} value={currentMusicVolume}
                                        onChange={handleMusicVolumeChange}/>
                                <VolumeUp style={{color: "#000"}}/>
                            </Stack>
                        </div>
                        <div className="flex flex-row gap-2 justify-between self-center">
                            <Avatar>
                                <MusicNote style={{color: "#000"}}/>
                            </Avatar>
                        </div>
                    </div>
                </div>
            </Box>
            {showAlert && (
                <Alert severity="error" className={"absolute bottom-16"}>{error}</Alert>
            )}
        </Box>
    )
        ;
}

interface SelectGameSliderProps {
    onGameStartError: (reason: string) => void
}

function SelectGameSlider({onGameStartError}: SelectGameSliderProps) {

    let intervalId: number | undefined = undefined;
    const stopScroll = () => {
        clearInterval(intervalId);
        window.removeEventListener("mouseup", stopScroll);
        window.removeEventListener("mouseleave", stopScroll);
    };


    const scrollLeft = () => {
        const container = document.querySelector("#sideScroller") as HTMLElement;
        if (container) {
            container.scrollLeft -= 200;
        }
    }

    const scrollRight = () => {
        const container = document.querySelector("#sideScroller") as HTMLElement;
        if (container) {
            container.scrollLeft += 200;
        }
    }

    const autoScroll = (distance: number) => {
        stopScroll();
        const container = document.querySelector("#sideScroller") as HTMLElement;
        if (!container) return;

        const scrollStep = () => {
            const newDistance = container.scrollLeft + distance;
            if (container.scrollLeft < 0) {
                container.scrollLeft = 0;
                stopScroll();
            } else if (container.scrollLeft > container.scrollWidth) {
                container.scrollLeft = container.scrollWidth;
                stopScroll();
            } else {
                container.scrollLeft = newDistance;
            }
        };
        intervalId = setInterval(scrollStep, 33);

        window.addEventListener("mouseup", stopScroll);
        window.addEventListener("mouseleave", stopScroll);
    }

    function callForceStartGame(gameMode: string, forceActivateAllController: boolean) {
        forceStartGame(gameMode, forceActivateAllController).then((result) => {
            if (result.status == ApiStatus.ERROR) {
                onGameStartError(result.reason ?? "Unknown Error")
            }
        })
    }

    return (
        <div className="flex flex-row justify-between p-2 m-2">
            <div className="flex-col content-center z-10 h-full w-8" onClick={scrollLeft}
                 onMouseOver={() => autoScroll(-5)}>
                <ArrowBackIosNewIcon height={48} width={48} className="self-center"/>
            </div>
            <div id="sideScroller" className="flex flex-row gap-4 overflow-x-auto max-w-screen w-full p-8 items-center"
                 style={{maxWidth: "calc(100vw - 128px)"}}>

                <GameCard image={freeForAllImage}
                          title={getDisplayName(GameMode.FREE_FOR_ALL)}
                          onStart={() => {
                              callForceStartGame(GameMode.FREE_FOR_ALL, false)
                          }}
                          onForceStart={() => {
                              callForceStartGame(GameMode.FREE_FOR_ALL, true)
                          }}/>

                <GameCard image={sortingToddlerImage}
                          title={getDisplayName(GameMode.SORTING_TODDLER)}
                          onStart={() => {
                              callForceStartGame(GameMode.SORTING_TODDLER, false)
                          }}
                          onForceStart={() => {
                              callForceStartGame(GameMode.SORTING_TODDLER, true)
                          }}/>

                <GameCard image={werewolfImage}
                          title={getDisplayName(GameMode.WEREWOLF)}
                          onStart={() => {
                              callForceStartGame(GameMode.WEREWOLF, false)
                          }}
                          onForceStart={() => {
                              callForceStartGame(GameMode.WEREWOLF, true)
                          }}/>

                <GameCard image={zombieImage}
                          title={getDisplayName(GameMode.ZOMBIE)}
                          onStart={() => {
                              callForceStartGame(GameMode.ZOMBIE, false)
                          }}
                          onForceStart={() => {
                              callForceStartGame(GameMode.ZOMBIE, true)
                          }}/>

                <GameCard isDeactivated image={ninjaBombImage} title={"Ninja Bomb"} onStart={() => {
                }} onForceStart={() => {
                }}/>

                <GameCard isDeactivated image={shakeNRunImage} title={"Shake n Run"} onStart={() => {
                }} onForceStart={() => {
                }}/>

                <GameCard isDeactivated image={protectTheKingImage} title={"Protect the King"} onStart={() => {
                }} onForceStart={() => {
                }}/>

            </div>
            <div className="flex-col content-center z-10 h-full w-8" onClick={scrollRight}
                 onMouseOver={() => autoScroll(5)}>
                <ArrowForwardIos height={48} width={48}/>
            </div>
        </div>
    );
}

interface GameCardProps {
    isDeactivated?: boolean
    image: string,
    title: string,
    onStart: () => void,
    onForceStart: () => void
}

function GameCard({isDeactivated = false, image, title, onStart, onForceStart}: GameCardProps) {
    return <Card sx={{minWidth: {xs: 200, md: 400, xl: 600}, height: "fit-content"}} className="pb-2">
        <CardMedia
            sx={{
                minHeight: {xs: 134, md: 268, xl: 402},
                filter: isDeactivated ? 'grayscale(100%)' : "none",
            }}
            image={image}
            title={title}
        />
        <CardContent>
            <Typography gutterBottom variant="h5" component="div"
                        color={isDeactivated ? "text.disabled" : "text.primary"}>
                {title}
            </Typography>
        </CardContent>
        <CardActions className="flex flex-row gap-4" sx={{justifyContent: 'space-between'}}>
            <Button className="flex-1" size="large" variant="outlined" onClick={onStart}
                    disabled={isDeactivated}>Start</Button>
            <Button className="flex-2" size="large" variant="contained" color="primary" onClick={onForceStart}
                    disabled={isDeactivated}>Force
                Start All</Button>
        </CardActions>
    </Card>
}

export default MainControl