import {useEffect, useState} from 'react'
import './App.css'
import {Chart as ChartJS, registerables} from 'chart.js';
import {Line} from 'react-chartjs-2'
import 'chartjs-adapter-date-fns';

ChartJS.register(...registerables);

function App() {
    const [rawHttpJSON, setRawHttpJSON] = useState<any[]>([])
    const [chartData, setChartData] = useState<{ labels: any, datasets: any } | null>(null)

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
        const newColor = colors[macToColor.size % colors.length ]
        macToColor.set(mac, newColor)
        return newColor
    }

    useEffect(() => {
        const interval = setInterval(() => {
            fetchJSON().then((result) => setRawHttpJSON(result)).catch(console.error)
        }, 2000);

        return () => clearInterval(interval);
    }, []);

    async function fetchJSON() {
        const response = await fetch("http://localhost:8080/api/accelerations")
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


    const options = {
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
                suggestedMin: 0.7,
                suggestedMax: 1.3,
                title: {
                    display: true,
                    text: 'Acceleration',
                },
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

    return (
        <>
            <h1 className="mb-8">Debug Move controller</h1>

            {(chartData != null && chartData.datasets.length > 0) &&
                <div className={"min-w-full"}>
                    <Line
                        key="AccelerationGraph"
                        data={chartData}
                        options={options}
                    />
                </div>
            }
        </>
    )
}

export default App
