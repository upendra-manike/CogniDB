const fetch = require('node-fetch');

const COGNIDB_URL = 'http://localhost:8080/api/sql';

async function executeQuery(sql) {
    const response = await fetch(COGNIDB_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sql })
    });
    
    if (!response.ok) {
        throw new Error(`HTTP Error ${response.status}: ${await response.text()}`);
    }
    return await response.json();
}

async function main() {
    console.log('=================================================');
    console.log('💚 CogniDB Node.js Integration Demo');
    console.log('=================================================');

    try {
        // 1. Create Table
        await executeQuery(`
            CREATE TABLE node_services (
                id VARCHAR PRIMARY KEY,
                name VARCHAR,
                region VARCHAR,
                latency_ms FLOAT,
                embedding FLOAT_VECTOR(128)
            );
        `);
        console.log('✅ Created "node_services" table.');
    } catch (e) {
        console.log('ℹ️ Table info:', e.message);
    }

    // 2. Insert Record
    const insertRes = await executeQuery(`
        INSERT INTO node_services VALUES (
            'srv_501',
            'Authentication Microservice',
            'us-east-1',
            1.2,
            AI_EMBED('Authentication JWT OAuth2 security microservice')
        );
    `);
    console.log('✅ Inserted Service Record:', insertRes.status || 'OK');

    // 3. Vector Similarity Query
    const searchRes = await executeQuery(`
        SELECT id, name, region, latency_ms 
        FROM node_services 
        WHERE region = 'us-east-1' 
          AND embedding SIMILAR TO 'security authentication microservice' 
        TOP 1;
    `);
    console.log('\n🔍 Vector Search Results:');
    console.log(JSON.stringify(searchRes, null, 2));

    console.log('=================================================');
}

main().catch(console.error);
