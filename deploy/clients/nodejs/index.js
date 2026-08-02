/**
 * Official Node.js Client for CogniDB AI-Native Unified Database Engine.
 */
class CogniDBClient {
    constructor(options = {}) {
        this.host = (options.host || 'http://localhost:8080').replace(/\/$/, '');
        this.endpoint = `${this.host}/api/sql`;
        this.apiKey = options.apiKey || null;
    }

    async query(sql) {
        const headers = { 'Content-Type': 'application/json' };
        if (this.apiKey) {
            headers['Authorization'] = `Bearer ${this.apiKey}`;
        }

        const response = await fetch(this.endpoint, {
            method: 'POST',
            headers,
            body: JSON.stringify({ sql })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`CogniDB Error (${response.status}): ${errorText}`);
        }

        return await response.json();
    }

    async vectorSearch(table, queryText, topK = 5, whereClause = '') {
        const whereStmt = whereClause ? `WHERE ${whereClause} AND ` : 'WHERE ';
        const sql = `SELECT * FROM ${table} ${whereStmt}embedding SIMILAR TO '${queryText}' TOP ${topK};`;
        const res = await this.query(sql);
        return res.data || [];
    }
}

module.exports = { CogniDBClient };
