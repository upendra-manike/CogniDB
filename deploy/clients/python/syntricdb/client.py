import requests
import json
from typing import Dict, Any, List, Optional

class SyntricDBClient:
    """
    Official Python Client for SyntricDB AI-Native Unified Database Engine.
    """
    def __init__(self, host: str = "http://localhost:8080", api_key: Optional[str] = None):
        self.host = host.rstrip("/")
        self.endpoint = f"{self.host}/api/sql"
        self.headers = {"Content-Type": "application/json"}
        if api_key:
            self.headers["Authorization"] = f"Bearer {api_key}"

    def query(self, sql: str) -> Dict[str, Any]:
        """
        Executes a SQL query, vector similarity search, or AI prompt against SyntricDB.
        """
        payload = {"sql": sql}
        response = requests.post(self.endpoint, json=payload, headers=self.headers)
        if response.status_code == 200:
            return response.json()
        else:
            raise RuntimeError(f"SyntricDB Error ({response.status_code}): {response.text}")

    def insert_vector(self, table: str, record_id: str, text: str, extra_fields: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Helper to insert a record with automatic AI text vector embedding.
        """
        fields = ["id", "embedding"]
        values = [f"'{record_id}'", f"AI_EMBED('{text}')"]
        
        if extra_fields:
            for key, val in extra_fields.items():
                fields.append(key)
                if isinstance(val, str):
                    values.append(f"'{val}'")
                else:
                    values.append(str(val))

        sql = f"INSERT INTO {table} ({', '.join(fields)}) VALUES ({', '.join(values)});"
        return self.query(sql)

    def vector_search(self, table: str, query_text: str, top_k: int = 5, where_clause: str = "") -> List[Dict[str, Any]]:
        """
        Performs sub-millisecond HNSW vector similarity search.
        """
        where_stmt = f"WHERE {where_clause} AND " if where_clause else "WHERE "
        sql = f"SELECT * FROM {table} {where_stmt}embedding SIMILAR TO '{query_text}' TOP {top_k};"
        res = self.query(sql)
        return res.get("data", [])
