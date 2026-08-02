#!/usr/bin/env bash

COGNIDB_URL="http://localhost:8080/api/sql"

echo "================================================="
echo "💻 CogniDB cURL & REST API Executable Demo"
echo "================================================="

echo "1. Creating Table 'curl_logs'..."
curl -s -X POST "$COGNIDB_URL" \
  -H "Content-Type: application/json" \
  -d '{"sql": "CREATE TABLE curl_logs (id VARCHAR PRIMARY KEY, log_level VARCHAR, message VARCHAR, embedding FLOAT_VECTOR(128));"}'
echo -e "\n"

echo "2. Inserting Record with Vector Embedding..."
curl -s -X POST "$COGNIDB_URL" \
  -H "Content-Type: application/json" \
  -d '{"sql": "INSERT INTO curl_logs VALUES (\"log_901\", \"ERROR\", \"Memory allocation failure in worker thread\", AI_EMBED(\"memory allocation error failure thread\"));"}'
echo -e "\n"

echo "3. Querying Hybrid SQL + Vector Similarity..."
curl -s -X POST "$COGNIDB_URL" \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT id, log_level, message FROM curl_logs WHERE log_level=\"ERROR\" AND embedding SIMILAR TO \"memory allocation failure\" TOP 1;"}'
echo -e "\n"

echo "4. Executing In-Engine AI RAG Query..."
curl -s -X POST "$COGNIDB_URL" \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT AI_RAG(\"Summarize recent error log events.\");"}'
echo -e "\n"

echo "================================================="
