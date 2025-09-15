import * as React from 'react'
import {useEffect, useState} from 'react'
import {Chart as ChartJS, registerables} from 'chart.js';
import {Line} from 'react-chartjs-2'
import 'chartjs-adapter-date-fns';
import PauseIcon from '@mui/icons-material/Pause';
import PlayIcon from '@mui/icons-material/PlayCircle';
import Box from "@mui/material/Box";
import {Button, CircularProgress, Paper, Tab, TableContainer, Tabs, Typography} from "@mui/material";

import {useThreadContext} from "../context/DebugThreadProvider.tsx";
import {ThreadTreeNode} from "../components/ThreadTree.tsx";

ChartJS.register(...registerables);

function Debug() {

    const [rawHttpJSON, setRawHttpJSON] = useState<any[]>([])
    const [chartData, setChartData] = useState<{ labels: any, datasets: any } | null>(null)
    const [pauseChart, setPauseChart] = useState<boolean>(false)
    const [activeTab, setActiveTab] = useState<number>(0)

    const colors: string[] = [
        "#0066CC",
        "#DCA614",
        "#5E40BE",
        "#63993D",
        "#C7C7C7",
        "#CA6C0F",
        "#37A3A3",
        "#F0561D",

        "#92C5F9",
        "#FFE072",
        "#B6A6E9",
        "#AFDC8F",
        "#F2F2F2",
        "#F8AE54",
        "#9AD8D8",
        "#F89B78",

        "#003366",
        "#96640F",
        "#21134D",
        "#204D00",
        "#707070",
        "#732E00",
        "#004D4D",
        "#731F00",
    ]

    const macToColor: Map<string, string> = new Map();

    function getColorForMac(mac: string): string {
        if (macToColor.has(mac)) {
            return macToColor.get(mac)!
        }
        const newColor = colors[macToColor.size % colors.length]
        macToColor.set(mac, newColor)
        return newColor
    }

    useEffect(() => {
        const interval = setInterval(() => {
            if (!pauseChart) {
                fetchJSON().then((result) => setRawHttpJSON(result)).catch(console.error)
            }
        }, 2000);

        return () => clearInterval(interval);
    }, [pauseChart]);

    async function fetchJSON() {
        const response = await fetch(`http://${window.location.hostname}/api/accelerations`)
        const jsonData = await response.json()
        return jsonData
    }

    useEffect(() => {
        const datasets: any[] = []
        let minTimeStamp = 9999999999999
        let maxTimeStamp = 0
        rawHttpJSON.forEach((entry) => {
            if (Boolean(entry) && Boolean(entry.accelerations) && entry.accelerations.length > 0) {
                const label = (Boolean(entry.colorName)) ? `${entry.colorName} (${entry.mac})` : entry.mac

                datasets.push({
                    label: label,
                    data: entry.accelerations.map((value: { t: number; a: number }) => {
                        if (value.t > maxTimeStamp) {
                            maxTimeStamp = value.t
                        }
                        if (value.t < minTimeStamp) {
                            minTimeStamp = value.t
                        }
                        return {
                            x: new Date(value.t),
                            y: value.a,
                        }
                    }),
                    fill: false,
                    borderColor: getColorForMac(entry.mac),
                    backgroundColor: `${getColorForMac(entry.mac)}80`,
                    tension: 0.1,
                })
            }
        })
        setChartData({
            labels: [minTimeStamp, maxTimeStamp],
            datasets: datasets,
        })
    }, [rawHttpJSON]);

    function stopChartUpdates() {
        setPauseChart(true)
    }

    function startChartUpdates() {
        setPauseChart(false)
    }

    const options = {
        responsive: true,
        maintainAspectRatio: false,
        animation: {
            duration: 250,
            easing: 'linear' as const,
        },
        plugins: {
            legend: {
                display: true,
            },
        },
        scales: {
            x: {
                type: 'time' as const,
                time: {
                    unit: 'second' as const,
                    displayFormats: {
                        second: 'HH:mm:ss',
                    },
                    tooltipFormat: 'HH:mm:ss',
                },
                title: {
                    display: true,
                    text: 'Zeit',
                },
                ticks: {
                    maxTicksLimit: 10,
                },
            },
            y: {
                beginAtZero: false,
                suggestedMin: 0.7,
                suggestedMax: 1.3,
                title: {
                    display: true,
                    text: 'Acceleration',
                },
            },
        },
        elements: {
            point: {
                radius: 2,
            },
        },
        transitions: {
            active: {
                animation: {
                    duration: 250,
                    easing: 'linear' as const,
                    onProgress: (animation: any) => {
                        const chart = animation.chart;
                        chart.scales.x.ticks.forEach((tick: any) => {
                            tick.x -= 1;
                        });
                    },
                },
            },
        },
    };

    const threadStats = useThreadContext();
    console.log(threadStats)

    const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
        setActiveTab(newValue)
    }



    return (
        <Box className="rootPage p-4 scroll-auto mb-14">
            <Box sx={{width: '100%'}}>
                <Box sx={{borderBottom: 1, borderColor: 'divider'}}>
                    <Tabs value={activeTab} onChange={handleTabChange} aria-label="debug tabs">
                        <Tab label="Accelerations Chart"/>
                        <Tab label="Threads"/>
                    </Tabs>
                </Box>

                {/* Tab Panel 0 - Acceleration Chart */}
                {activeTab === 0 && (
                    <>
                        <div className="card mb-4 flex flex-row justify-center">
                            {pauseChart ? (
                                <Button variant="contained"
                                        className={"flex flex-row justify-start justify-items-center items-center min-w-24"}
                                        style={{textTransform: "none"}}
                                        onClick={startChartUpdates}
                                        startIcon={<PlayIcon/>}
                                >
                                    <span className={"text-lg"}>Play</span>
                                </Button>) : (
                                <Button variant="contained"
                                        className={"flex flex-row justify-start justify-items-center items-center min-w-24"}
                                        style={{textTransform: "none"}}
                                        onClick={stopChartUpdates}
                                        startIcon={<PauseIcon/>}
                                >
                                    <span className={"text-lg"}>Pause</span>
                                </Button>)}
                        </div>


                        <div className={"min-w-full"} style={{height: "calc(100vh - 160px)"}}>
                            {(chartData != null && chartData.datasets.length > 0) ?
                                (<Line
                                    key="AccelerationGraph"
                                    data={chartData}
                                    options={options}
                                />)
                                : (
                                    <div className="flex flex-col justify-center items-center h-full w-full">
                                        <CircularProgress size="80px"/>
                                    </div>
                                )
                            }
                        </div>
                    </>
                )}
                {/* Tab Panel 1 - thread stats */}
                {activeTab === 1 && (
                    <Box sx={{p: 3}}>
                        {threadStats ? (
                            <>
                                <Typography variant="h6" gutterBottom>
                                    Thread Hierarchie (Gesamt: {threadStats.totalThreadCount} Threads)
                                </Typography>
                                <TableContainer component={Paper} sx={{minWidth: 650}}>
                                    <ThreadTreeNode
                                        group={threadStats.rootGroup}
                                        level={0}
                                    />
                                </TableContainer>
                            </>
                        ) : (
                            <Box sx={{display: 'flex', justifyContent: 'center', p: 4}}>
                                <CircularProgress/>
                            </Box>
                        )}
                    </Box>
                )}

            </Box>
        </Box>
    )
}

export default Debug