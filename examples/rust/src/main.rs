use serde::Serialize;
use std::error::Error;

const SYNTRICDB_URL: &str = "http://localhost:8080/api/sql";

#[derive(Serialize)]
struct QueryRequest<'a> {
    sql: &'a str,
}

async fn execute_query(sql: &str) -> Result<String, Box<dyn Error>> {
    let client = reqwest::Client::new();
    let body = QueryRequest { sql };
    let resp = client
        .post(SYNTRICDB_URL)
        .json(&body)
        .send()
        .await?
        .text()
        .await?;
    Ok(resp)
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    println!("=================================================");
    println!("🦀 SyntricDB Rust Integration Demo");
    println!("=================================================");

    // 1. Create Table
    let create_sql = "CREATE TABLE rust_sensors (id VARCHAR PRIMARY KEY, temp FLOAT, embedding FLOAT_VECTOR(128));";
    match execute_query(create_sql).await {
        Ok(res) => println!("✅ Create Table Response: {}", res),
        Err(e) => println!("ℹ️ Info: {}", e),
    }

    // 2. Insert Record
    let insert_sql = "INSERT INTO rust_sensors VALUES ('sen_801', 42.5, AI_EMBED('overheating temperature sensor warning'));";
    let res2 = execute_query(insert_sql).await?;
    println!("✅ Insert Record Response: {}", res2);

    // 3. Vector Similarity Search Query
    let search_sql = "SELECT id, temp FROM rust_sensors WHERE embedding SIMILAR TO 'temperature warning' TOP 1;";
    let res3 = execute_query(search_sql).await?;
    println!("\n🔍 Vector Search Results:\n{}", res3);

    println!("=================================================");
    Ok(())
}
