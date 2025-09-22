import Box from "@mui/material/Box";
import {Avatar, Button, Card, CardActions, CardContent, CardMedia, Slider, Stack, Typography} from "@mui/material";
import freeForAllImage from '../assets/free-for-all.banner.full.png';
import sortingToddlerImage from '../assets/sorting-toddler.banner.full.png';
import werewolfImage from '../assets/werewolf.banner.full.png';
import zombieImage from '../assets/zombie.banner.full.png';
import protectTheKingImage from '../assets/protect-the-king.banner.full.png';
import ninjaBombImage from '../assets/ninja-bomb.banner.full.png';
import Diversity3Icon from "@mui/icons-material/Diversity3";
// @ts-ignore
import SkullsAndBonesIcon from '../assets/skulls-and-bones.svg?react';
// @ts-ignore
import PSMoveControllerIcon from '../assets/PSMoveController.svg?react';
import ArrowBackIosNewIcon from '@mui/icons-material/ArrowBackIosNew';
import {ArrowForwardIos, MusicNote, SurroundSound, VolumeDown, VolumeUp} from "@mui/icons-material";

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

    return (
        <Box className="rootPage p-4 scroll-auto mb-14">
            <Box className="flex flex-col justify-between" sx={{minWidth: 768, height: '100vh', overflow: 'hidden'}}>
                <div className="flex flex-row justify-between items-center">
                    <Typography gutterBottom variant="h5" component="span" sx={{minWidth: 200}}>
                        Aktuelles Spiel: FFA
                    </Typography>

                    <Typography gutterBottom variant="subtitle1" component="span" sx={{minWidth: 200}}>
                        <div className={"flex flex-row gap-2 items-center"}>
                            11 <Diversity3Icon/>
                            <span className="mr-8 ml-8">|</span>
                            3 <PSMoveControllerIcon width={32} height={32} style={{
                            color: "#ffa500",
                            transform: "rotate(20deg)",
                            opacity: 1
                        }}/>
                            <span className="mr-8 ml-8">|</span>
                            <SkullsAndBonesIcon alt="Skulls and Bones" width={32} height={32}/>
                            3
                        </div>

                    </Typography>

                    <div style={{minWidth: 200}}>
                        <Button size="large" variant="contained" color="error">Stop Game</Button>
                    </div>
                </div>
                <SelectGameSlider/>
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
                                <Slider aria-label="Global" marks={sliderMarks} step={5} value={30}
                                        onChange={() => {
                                            console.log("huhu")
                                        }}/>
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
                                <Slider aria-label="Global" marks={sliderMarks} step={5} value={30}
                                        onChange={() => {
                                            console.log("huhu")
                                        }}/>
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
        </Box>
    )
        ;
}

function SelectGameSlider() {

    let intervalId: any;
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

    return (
        <div className="flex flex-row justify-between p-2 m-2">
            <div className="flex-col content-center z-10 h-full w-8" onClick={scrollLeft}
                 onMouseOver={() => autoScroll(-5)}>
                <ArrowBackIosNewIcon height={48} width={48} className="self-center"/>
            </div>
            <div id="sideScroller" className="flex flex-row gap-4 overflow-x-auto max-w-screen w-full p-8">

                <GameCard image={freeForAllImage} title={"Free For all"} onStart={() => {
                }} onForceStart={() => {
                }}/>

                <GameCard image={sortingToddlerImage} title={"Sorting Toddler"} onStart={() => {
                }} onForceStart={() => {
                }}/>

                <GameCard image={werewolfImage} title={"Werewolf"} onStart={() => {
                }} onForceStart={() => {
                }}/>

                <GameCard image={zombieImage} title={"Zombie"} onStart={() => {
                }} onForceStart={() => {
                }}/>

                <GameCard image={ninjaBombImage} title={"Ninja Bomb"} onStart={() => {
                }} onForceStart={() => {
                }}/>

                <GameCard image={protectTheKingImage} title={"Protect the King"} onStart={() => {
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
    image: string,
    title: string,

    onStart: () => void,

    onForceStart: () => void
}

function GameCard({image, title, onStart, onForceStart}: GameCardProps) {
    return <Card sx={{minWidth: 400}}>
        <CardMedia sx={{height: 267}}
                   image={image}
                   title={title}>
        </CardMedia>
        <CardContent>
            <Typography gutterBottom variant="h5" component="div">
                {title}
            </Typography>
        </CardContent>
        <CardActions className="flex flex-row gap-4" sx={{justifyContent: 'space-between'}}>
            <Button className="flex-1" size="large" variant="outlined" onClick={onStart}>Start</Button>
            <Button className="flex-2" size="large" variant="contained" color="primary" onClick={onForceStart}>Force
                Start All</Button>
        </CardActions>
    </Card>
}

export default MainControl