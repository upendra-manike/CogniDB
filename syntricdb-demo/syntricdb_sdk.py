import json
import urllib.request
import urllib.error

class SyntricDBClient:
    """Official SyntricDB Client SDK for Python"""

    def __init__(self, host="http://localhost:8080"):
        self.host = host.rstrip('/')
        self.sql_endpoint = f"{self.host}/api/sql"
        self.vector_endpoint = f"{self.host}/api/vector/search"
        self.rag_endpoint = f"{self.host}/api/ai/rag"
        self.health_endpoint = f"{self.host}/api/cluster"

    def test_connection(self):
        """Test live database connectivity"""
        try:
            req = urllib.request.Request(self.health_endpoint, headers={"User-Agent": "SyntricDB-Python-Client/1.0"})
            with urllib.request.urlopen(req, timeout=5) as response:
                if response.status == 200:
                    data = json.loads(response.read().decode('utf-8'))
                    return True, data
                return False, f"HTTP Status {response.status}"
        except Exception as e:
            return False, str(e)

    def execute_sql(self, sql_query):
        """Execute any SQL / AI / Vector query against SyntricDB"""
        payload = json.dumps({"sql": sql_query}).encode('utf-8')
        req = urllib.request.Request(
            self.sql_endpoint,
            data=payload,
            headers={"Content-Type": "application/json", "User-Agent": "SyntricDB-Python-Client/1.0"}
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as response:
                result = json.loads(response.read().decode('utf-8'))
                return result
        except urllib.error.HTTPError as e:
            err_body = e.read().decode('utf-8')
            return {"success": False, "error": f"HTTP {e.code}: {err_body}"}
        except Exception as e:
            return {"success": False, "error": str(e)}

    def search_vector(self, table_name, vector_column, query_text, top_k=5):
        """Perform semantic vector similarity search"""
        sql = f"SELECT * FROM {table_name} WHERE {vector_column} SIMILAR TO '{query_text}' TOP {top_k}"
        return self.execute_sql(sql)

    def ask_rag(self, prompt, top_k=3):
        """Perform sub-millisecond Retrieval Augmented Generation (RAG)"""
        payload = json.dumps({"prompt": prompt, "topK": top_k}).encode('utf-8')
        req = urllib.request.Request(
            self.rag_endpoint,
            data=payload,
            headers={"Content-Type": "application/json", "User-Agent": "SyntricDB-Python-Client/1.0"}
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as response:
                return json.loads(response.read().decode('utf-8'))
        except Exception as e:
            return {"success": False, "error": str(e)}
