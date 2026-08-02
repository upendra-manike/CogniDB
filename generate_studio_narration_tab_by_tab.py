import os
import asyncio
import edge_tts

# Using Microsoft Studio Neural Voice: Christopher (Deep professional AI tech voice)
VOICE = "en-US-ChristopherNeural"

SEGMENTS = [
    {
        "tab": "Overview & Cluster",
        "audio": "tab_1_dashboard.mp3",
        "image": "frame_1_dashboard.png",
        "text": "Welcome to CogniDB Studio. Starting with Tab 1, the Overview and Cluster dashboard. Here, developers get real-time telemetry on buffer pool hit ratios, memory usage, query latency, and unified node health."
    },
    {
        "tab": "SQL & AI Query Studio",
        "audio": "tab_2_query_console.mp3",
        "image": "frame_2_query_console.png",
        "text": "Switching to Tab 2, the SQL and AI Query Studio. In this interactive console, you can run standard relational SQL alongside sub-millisecond vector similarity search using intuitive extensions like SIMILAR TO."
    },
    {
        "tab": "Vector 2D Explorer",
        "audio": "tab_3_vector_explorer.mp3",
        "image": "frame_3_vector_explorer.png",
        "text": "Moving to Tab 3, the Vector 2D Explorer. Here, you can visually map high-dimensional vector embeddings, locate nearest semantic neighbors in real-time, and interactively explore semantic clusters."
    },
    {
        "tab": "AI RAG Search Engine",
        "audio": "tab_4_rag_assistant.mp3",
        "image": "frame_4_rag_assistant.png",
        "text": "Now in Tab 4, the AI RAG Search Engine. CogniDB brings retrieval augmented generation directly into the database engine, synthesizing context-aware answers natively inside your SQL queries."
    },
    {
        "tab": "IOPS Benchmark Suite",
        "audio": "tab_5_benchmark.mp3",
        "image": "frame_5_benchmark.png",
        "text": "Finally, in Tab 5, the IOPS Benchmark Suite. You can stress test CogniDB with over 100K write operations per second and sub-millisecond HNSW vector search latencies. Star CogniDB on GitHub today!"
    }
]

async def generate_audio():
    for seg in SEGMENTS:
        print(f"Generating studio neural voice for {seg['tab']} ({seg['audio']})...")
        communicate = edge_tts.Communicate(seg['text'], VOICE)
        await communicate.save(seg['audio'])

if __name__ == "__main__":
    asyncio.run(generate_audio())
    print("All tab-by-tab studio neural audio segments generated successfully!")
