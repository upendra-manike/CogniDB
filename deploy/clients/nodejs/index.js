/**
 * Official Node.js Client SDK for SyntricDB AI-Native Unified Database Engine.
 */
class SyntricDBClient {
    constructor(options = {}) {
        if (typeof options === 'string') {
            this.host = options.replace(/\/$/, '');
            this.apiKey = null;
        } else {
            this.host = (options.host || 'http://localhost:8080').replace(/\/$/, '');
            this.apiKey = options.apiKey || null;
        }
        this.sqlEndpoint = `${this.host}/api/sql`;
        this.vectorEndpoint = `${this.host}/api/vector/search`;
        this.ragEndpoint = `${this.host}/api/ai/rag`;
        this.clusterEndpoint = `${this.host}/api/cluster`;
    }

    _getHeaders() {
        const headers = { 'Content-Type': 'application/json' };
        if (this.apiKey) {
            headers['Authorization'] = `Bearer ${this.apiKey}`;
        }
        return headers;
    }

    /**
     * Executes a SQL query against SyntricDB.
     * @param {string} sql 
     */
    async query(sql) {
        const response = await fetch(this.sqlEndpoint, {
            method: 'POST',
            headers: this._getHeaders(),
            body: JSON.stringify({ sql })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`SyntricDB Error (${response.status}): ${errorText}`);
        }

        return await response.json();
    }

    /**
     * Alias for query(sql)
     */
    async executeSql(sql) {
        return this.query(sql);
    }

    /**
     * Performs HNSW vector similarity search.
     * @param {string} table 
     * @param {string} column 
     * @param {string} queryText 
     * @param {number} limit 
     */
    async vectorSearch(table, column = 'embedding', queryText = '', limit = 5) {
        const response = await fetch(this.vectorEndpoint, {
            method: 'POST',
            headers: this._getHeaders(),
            body: JSON.stringify({ table, column, query: queryText, limit })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`SyntricDB Vector Error (${response.status}): ${errorText}`);
        }

        return await response.json();
    }

    /**
     * Executes Retrieval-Augmented Generation (RAG) context search.
     * @param {string} prompt 
     * @param {string} table 
     * @param {string} column 
     * @param {number} limit 
     */
    async askRag(prompt, table = 'users', column = 'embedding', limit = 3) {
        const response = await fetch(this.ragEndpoint, {
            method: 'POST',
            headers: this._getHeaders(),
            body: JSON.stringify({ prompt, table, column, limit })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`SyntricDB RAG Error (${response.status}): ${errorText}`);
        }

        return await response.json();
    }

    /**
     * Retrieves cluster topology, status, and node health.
     */
    async getClusterStatus() {
        const response = await fetch(this.clusterEndpoint, {
            method: 'GET',
            headers: this._getHeaders()
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`SyntricDB Cluster Error (${response.status}): ${errorText}`);
        }

        return await response.json();
    }

    /**
     * Tests connectivity to the SyntricDB server.
     */
    async testConnection() {
        try {
            const status = await this.getClusterStatus();
            return { connected: true, info: status };
        } catch (err) {
            return { connected: false, error: err.message };
        }
    }
}

module.exports = { SyntricDBClient };
