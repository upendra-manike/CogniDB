import os
import asyncio
import edge_tts

# Using Microsoft Studio Neural Voice: Christopher (Deep professional AI tech voice)
VOICE = "en-US-ChristopherNeural"

SEGMENTS = [
    {
        "tab": "Dashboard Overview",
        "audio": "tab_1_dashboard.mp3",
        "image": "frame_1_dashboard.png",
        "text": "Starting with Tab 1, the Dashboard Overview. Here, developers get real-time metrics on buffer pool hit ratios, vector search index latency, active memory usage, and unified node health."
    },
    {
        "tab": "SQL Query Console",
        "audio": "tab_2_query_console.mp3",
        "image": "frame_2_query_console.png",
        "text": "Moving to Tab 2, the SQL and AI Query Console. This interactive workspace allows you to run standard SQL queries alongside sub-millisecond vector similarity search using intuitive syntax like SIMILAR TO."
    },
    {
        "tab": "Vector Explorer",
        "audio": "tab_3_vector_explorer.mp3",
        "image": "frame_3_vector_explorer.png",
        "text": "Next, in Tab 3, the 2D Vector Explorer. You can visually explore high-dimensional vector embeddings, discover nearest semantic neighbors, and inspect vector clusters interactively in real time."
    },
    {
        "tab": "Metrics & Telemetry",
        "audio": "tab_4_metrics.mp3",
        "image": "frame_4_metrics.png",
        "text": "In Tab 4, the Metrics and Telemetry screen. This view provides live throughput charts, memory performance analysis, and JVM garbage collection health for enterprise monitoring."
    },
    {
        "tab": "Disaster Recovery Snapshots",
        "audio": "tab_5_snapshots.mp3",
        "image": "frame_5_snapshots.png",
        "text": "Finally, in Tab 5, the Disaster Recovery Snapshots tab. CogniDB features automated write-ahead log persistence, allowing one-click point-in-time snapshots and zero-downtime cluster backups."
    }
]

async def generate_audio():
    for seg in SEGMENTS:
        print(f"Generating studio neural voice for {seg['tab']} ({seg['audio']})...")
        communicate = edge_tts.Communicate(seg['text'], VOICE)
        await communicate.save(seg['audio'])

if __name__ == "__main__":
    asyncio.run(generate_audio())
    print("All tab-by-tab neural audio segments generated successfully!")
