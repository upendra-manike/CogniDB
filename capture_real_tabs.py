import time
from playwright.sync_api import sync_playwright

TABS = [
    {"selector": 'button[data-tab="dashboard"]', "img": "frame_1_dashboard.png", "title": "Overview & Cluster"},
    {"selector": 'button[data-tab="sql-studio"]', "img": "frame_2_query_console.png", "title": "SQL & AI Query Studio"},
    {"selector": 'button[data-tab="vector-explorer"]', "img": "frame_3_vector_explorer.png", "title": "Vector 2D Explorer"},
    {"selector": 'button[data-tab="rag-assistant"]', "img": "frame_4_rag_assistant.png", "title": "AI RAG Search Engine"},
    {"selector": 'button[data-tab="benchmark"]', "img": "frame_5_benchmark.png", "title": "IOPS Benchmark Suite"}
]

def capture_tabs():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={'width': 1920, 'height': 1080})
        page = context.new_page()
        
        print("Navigating to CogniDB Web Studio...")
        page.goto("http://localhost:8080/")
        page.wait_for_load_state("networkidle")
        time.sleep(1.5)
        
        for tab in TABS:
            print(f"Clicking Tab: {tab['title']} ({tab['selector']})...")
            btn = page.query_selector(tab["selector"])
            if btn:
                btn.click()
                time.sleep(1.5)
            else:
                print(f"ERROR: Selector {tab['selector']} not found!")
                
            page.screenshot(path=tab["img"])
            print(f"Successfully saved {tab['img']} for {tab['title']}")
            
        browser.close()

if __name__ == "__main__":
    capture_tabs()
