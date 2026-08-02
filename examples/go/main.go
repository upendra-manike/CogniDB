package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
)

const CogniDBURL = "http://localhost:8080/api/sql"

type QueryPayload struct {
	SQL string `json:"sql"`
}

func executeQuery(sql string) (string, error) {
	payload := QueryPayload{SQL: sql}
	body, err := json.Marshal(payload)
	if err != nil {
		return "", err
	}

	resp, err := http.Post(CogniDBURL, "application/json", bytes.NewBuffer(body))
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	return string(respBody), nil
}

func main() {
	fmt.Println("=================================================")
	fmt.Println("🔷 CogniDB Go Integration Demo")
	fmt.Println("=================================================")

	// 1. Create Table
	createSQL := `
	CREATE TABLE go_metrics (
		id VARCHAR PRIMARY KEY,
		metric_name VARCHAR,
		value FLOAT,
		embedding FLOAT_VECTOR(128)
	);`
	res, err := executeQuery(createSQL)
	if err != nil {
		fmt.Printf("Error creating table: %v\n", err)
	} else {
		fmt.Printf("✅ Create Table Response: %s\n", res)
	}

	// 2. Insert Record with Vector Embedding
	insertSQL := `
	INSERT INTO go_metrics VALUES (
		'met_601',
		'cpu_utilization',
		88.5,
		AI_EMBED('high cpu utilization server load alert')
	);`
	res, err = executeQuery(insertSQL)
	if err != nil {
		fmt.Printf("Error inserting record: %v\n", err)
	} else {
		fmt.Printf("✅ Insert Record Response: %s\n", res)
	}

	// 3. Vector Similarity Search Query
	searchSQL := `
	SELECT id, metric_name, value 
	FROM go_metrics 
	WHERE embedding SIMILAR TO 'server cpu load alert' 
	TOP 1;`
	res, err = executeQuery(searchSQL)
	if err != nil {
		fmt.Printf("Error searching: %v\n", err)
	} else {
		fmt.Printf("\n🔍 Vector Search Results:\n%s\n", res)
	}

	fmt.Println("=================================================")
}
