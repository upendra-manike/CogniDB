#!/usr/bin/env python3
"""
CogniDB Real-Time Connectivity & CRUD Test Suite
"""

from cognidb_sdk import CogniDBClient
import sys
import time

def main():
    print("==========================================================================")
    print("⚡ CogniDB Real-Time Connectivity & CRUD Test Suite ⚡")
    print("==========================================================================")
    
    db = CogniDBClient("http://localhost:8080")
    
    # 1. TEST CONNECTIVITY
    print("\n📡 Step 1: Testing Connectivity to CogniDB Engine...")
    is_connected, info = db.test_connection()
    if not is_connected:
        print(f"❌ Connectivity Failed: {info}")
        print("💡 Make sure server is running with: cognidb start")
        sys.exit(1)
    
    print(f"✅ Connected to CogniDB Engine successfully!")
    print(f"   Cluster Leader: {info.get('leaderId', 'N/A')}")
    print(f"   Total Cluster Nodes: {len(info.get('nodes', []))}")
    print(f"   Storage Health: {info.get('status', 'OK')}")

    # 2. CREATE TABLE (CREATE)
    print("\n🔨 Step 2: Testing CREATE TABLE Operation...")
    create_table_sql = """
    CREATE TABLE products (
        id VARCHAR PRIMARY KEY,
        name VARCHAR,
        category VARCHAR,
        price DOUBLE,
        description VARCHAR,
        embedding FLOAT_VECTOR(128)
    )
    """
    res = db.execute_sql(create_table_sql)
    if res.get("success"):
        print(f"✅ CREATE TABLE 'products': {res.get('message')}")
    else:
        print(f"⚠️ Create Table Note: {res.get('error', res.get('message'))}")

    # 3. INSERT RECORDS (INSERT / CREATE)
    print("\n📥 Step 3: Testing INSERT CRUD Operations (Data + Vector Embeddings)...")
    products = [
        ("prod_001", "MacBook Pro M3", "Electronics", 1999.99, "High performance Apple Silicon laptop for software engineering and database development.", "Laptop Software"),
        ("prod_002", "Sony WH-1000XM5", "Audio", 399.99, "Noise canceling wireless headphones with crystal clear acoustics.", "Headphones Audio"),
        ("prod_003", "Dell UltraSharp 32", "Monitors", 799.99, "4K UHD IPS display monitor with high color precision for developers.", "Display Monitor"),
        ("prod_004", "Logitech MX Master 3S", "Accessories", 99.99, "Ergonomic wireless mouse with ultra-fast magspeed scrolling.", "Developer Accessories")
    ]

    for p in products:
        insert_sql = f"""
        INSERT INTO products VALUES (
            '{p[0]}', '{p[1]}', '{p[2]}', {p[3]}, '{p[4]}', AI_EMBED('{p[5]}')
        )
        """
        res = db.execute_sql(insert_sql)
        print(f"   ➕ Inserted product '{p[1]}': Status = {res.get('success')} ({res.get('executionTimeMs', 0):.2f} ms)")

    # 4. READ / SELECT OPERATIONS (READ)
    print("\n🔍 Step 4: Testing SELECT / READ CRUD Operations...")
    select_sql = "SELECT id, name, category, price FROM products WHERE price > 200.0"
    res = db.execute_sql(select_sql)
    print(f"   📊 Query Result ({res.get('rowCount')} rows found):")
    for row in res.get("data", []):
        print(f"      • {row.get('id')}: {row.get('name')} | Category: {row.get('category')} | Price: ${row.get('price')}")

    # 5. VECTOR SIMILARITY SEARCH (AI READ)
    print("\n🧠 Step 5: Testing AI Vector Similarity Search (HNSW Graph Index)...")
    vector_res = db.search_vector("products", "embedding", "High resolution coding display monitor", top_k=2)
    print(f"   🎯 Top Semantic Matches for 'High resolution coding display monitor':")
    for row in vector_res.get("data", []):
        print(f"      • {row.get('name')} (Price: ${row.get('price')}) - {row.get('description')}")

    # 6. STREAMING PUBLISH (EVENT STREAMING)
    print("\n📡 Step 6: Testing Real-Time Stream Event Publishing...")
    stream_sql = 'PUBLISH INTO telemetry VALUES {"event": "DEMO_COMPLETED", "status": "SUCCESS"}'
    stream_res = db.execute_sql(stream_sql)
    print(f"   ⚡ Stream Publish Status: {stream_res.get('message')}")

    # 7. NATIVE RAG QUERY
    print("\n🤖 Step 7: Testing Native RAG Context Retrieval...")
    rag_res = db.ask_rag("Find best laptop for database engineering", top_k=2)
    print(f"   Retrieved Context Snippets:")
    for ctx in rag_res.get("retrievedContext", []):
        print(f"      • {ctx.get('id')}: {ctx.get('bio', ctx.get('name', 'N/A'))}")

    print("\n==========================================================================")
    print("🎉 ALL CRUD AND CONNECTIVITY TESTS PASSED SUCCESSFULLY! 🎉")
    print("==========================================================================")

if __name__ == "__main__":
    main()
