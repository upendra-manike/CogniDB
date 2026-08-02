import requests
import json

COGNIDB_URL = "http://localhost:8080/api/sql"

def execute_query(sql_statement):
    headers = {"Content-Type": "application/json"}
    payload = {"sql": sql_statement}
    
    response = requests.post(COGNIDB_URL, json=payload, headers=headers)
    if response.status_code == 200:
        return response.json()
    else:
        raise Exception(f"Query execution failed: {response.status_code} - {response.text}")

def main():
    print("=================================================")
    print("🐍 CogniDB Python SDK & REST Integration Demo")
    print("=================================================")

    # 1. Create Table
    create_sql = """
    CREATE TABLE developers (
        id VARCHAR PRIMARY KEY,
        name VARCHAR,
        role VARCHAR,
        experience_years INT,
        bio VARCHAR,
        embedding FLOAT_VECTOR(128)
    );
    """
    try:
        execute_query(create_sql)
        print("✅ Created 'developers' table.")
    except Exception as e:
        print(f"ℹ️ Table creation info: {e}")

    # 2. Insert Records with Auto AI Embeddings
    insert_sql = """
    INSERT INTO developers VALUES (
        'dev_401',
        'Alice Johnson',
        'Senior AI Engineer',
        8,
        'Specializes in PyTorch, LLM fine-tuning, and sub-millisecond vector indexing.',
        AI_EMBED('Senior AI Engineer PyTorch LLM fine-tuning vector index')
    );
    """
    res = execute_query(insert_sql)
    print(f"✅ Inserted Developer Record: {res.get('status', 'OK')}")

    # 3. Hybrid SQL + Vector Search
    vector_sql = """
    SELECT id, name, role, experience_years, bio 
    FROM developers 
    WHERE experience_years >= 5 
      AND embedding SIMILAR TO 'LLM fine tuning vector index' 
    TOP 1;
    """
    search_res = execute_query(vector_sql)
    print("\n🔍 CogniDB Hybrid Vector Search Results:")
    print(json.dumps(search_res, indent=2))

    # 4. Native In-Engine RAG Query
    rag_sql = "SELECT AI_RAG('Who is our lead engineer for LLM fine-tuning?');"
    rag_res = execute_query(rag_sql)
    print("\n🤖 CogniDB Native AI RAG Response:")
    print(json.dumps(rag_res, indent=2))

    print("=================================================")

if __name__ == "__main__":
    main()
