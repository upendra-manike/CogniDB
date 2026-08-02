import os
import subprocess
import json

TABS = [
    {"img": "frame_1_dashboard.png", "audio": "tab_1_dashboard.mp3", "out": "clip_1.mp4"},
    {"img": "frame_2_query_console.png", "audio": "tab_2_query_console.mp3", "out": "clip_2.mp4"},
    {"img": "frame_3_vector_explorer.png", "audio": "tab_3_vector_explorer.mp3", "out": "clip_3.mp4"},
    {"img": "frame_4_metrics.png", "audio": "tab_4_metrics.mp3", "out": "clip_4.mp4"},
    {"img": "frame_5_snapshots.png", "audio": "tab_5_snapshots.mp3", "out": "clip_5.mp4"}
]

def get_audio_duration(audio_file):
    cmd = [
        "ffprobe", "-v", "quiet", "-print_format", "json",
        "-show_format", "-show_streams", audio_file
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    data = json.loads(res.stdout)
    return float(data["format"]["duration"])

def render_clips():
    clip_files = []
    for tab in TABS:
        dur = get_audio_duration(tab["audio"])
        print(f"Tab clip {tab['out']} duration: {dur:.2f} seconds")
        
        # Render individual clip synced to audio
        cmd = [
            "ffmpeg", "-y", "-loop", "1", "-i", tab["img"], "-i", tab["audio"],
            "-t", str(dur),
            "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "192k",
            tab["out"]
        ]
        subprocess.run(cmd, check=True)
        clip_files.append(tab["out"])
    
    # Create list file for ffmpeg concat
    with open("concat_list.txt", "w") as f:
        for clip in clip_files:
            f.write(f"file '{clip}'\n")
            
    print("Concatenating clips into cognidb_studio_tab_by_tab_demo.mp4...")
    concat_cmd = [
        "ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", "concat_list.txt",
        "-c", "copy", "cognidb_studio_tab_by_tab_demo.mp4"
    ]
    subprocess.run(concat_cmd, check=True)
    print("FINISHED! cognidb_studio_tab_by_tab_demo.mp4 is ready!")

if __name__ == "__main__":
    render_clips()
