import requests
import json
from typing import Dict, Any, List, Optional

class SyntricDBClient:
    """
    Official Python Client SDK for SyntricDB AI-Native Unified Database Engine.
    """
    def __init__(self, host: str = "http://localhost:8080", api_key: Optional[str] = None):
        self.host = host.rstrip("/")
        self.sql_endpoint = f"{self.host}/api/sql"
        self.vector_endpoint = f"{self.host}/api/vector/search"
        self.rag_endpoint = f"{self.host}/api/ai/rag"
        self.cluster_endpoint = f"{self.host}/api/cluster"
        self.headers = {"Content-Type": "application/json"}
        if api_key:
            self.headers["Authorization"] = f"Bearer {api_key}"

    def query(self, sql: str) -> Dict[str, Any]:
        """
        Executes a SQL query against SyntricDB.
        """
        payload = {"sql": sql}
        response = requests.post(self.sql_endpoint, json=payload, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"SyntricDB Error ({response.status_code}): {response.text}")

    def execute_sql(self, sql: str) -> Dict[str, Any]:
        """Alias for query(sql)"""
        return self.query(sql)

    def vector_search(self, table: str, column: str = "embedding", query: str = "", limit: int = 5) -> Dict[str, Any]:
        """
        Performs sub-millisecond HNSW vector similarity search.
        """
        payload = {
            "table": table,
            "column": column,
            "query": query,
            "limit": limit
        }
        response = requests.post(self.vector_endpoint, json=payload, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"SyntricDB Vector Error ({response.status_code}): {response.text}")

    def ask_rag(self, prompt: str, table: str = "users", column: str = "embedding", limit: int = 3) -> Dict[str, Any]:
        """
        Executes Retrieval-Augmented Generation (RAG) context search.
        """
        payload = {
            "prompt": prompt,
            "table": table,
            "column": column,
            "limit": limit
        }
        response = requests.post(self.rag_endpoint, json=payload, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"SyntricDB RAG Error ({response.status_code}): {response.text}")

    def get_cluster_status(self) -> Dict[str, Any]:
        """
        Retrieves cluster topology, Raft consensus status, and node health.
        """
        response = requests.get(self.cluster_endpoint, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"SyntricDB Cluster Error ({response.status_code}): {response.text}")

    def test_connection(self) -> tuple[bool, Dict[str, Any]]:
        """
        Tests connectivity to the SyntricDB server.
        """
        try:
            status = self.get_cluster_status()
            return True, status
        except Exception as e:
            return False, {"error": str(e)}
