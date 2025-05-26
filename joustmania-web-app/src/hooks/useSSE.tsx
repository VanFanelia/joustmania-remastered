import { useEffect, useState } from 'react';

export function useSSE<T>(url: string): T | null {
    const [data, setData] = useState<T | null>(null);

    useEffect(() => {
        const eventSource = new EventSource(url);

        eventSource.onmessage = (event) => {
            try {
                const parsed: T = JSON.parse(event.data);
                setData(parsed);
            } catch (error) {
                console.error("Failed to parse SSE data:", error);
            }
        };

        eventSource.onerror = (err) => {
            console.error("SSE error:", err);
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, [url]);

    return data;
}