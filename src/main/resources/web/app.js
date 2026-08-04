// SyntricDB Web Studio JavaScript Client Engine

let authToken = localStorage.getItem('syntricdb_auth_token') || '';
let currentUser = localStorage.getItem('syntricdb_user') || '';
let activeDb = 'default';

document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initAuth();

    // Auto-refresh stats every 10 seconds if logged in
    setInterval(() => {
        if (authToken) {
            refreshClusterStats();
        }
    }, 10000);
});

async function initAuth() {
    const passwordInput = document.getElementById('login-password');
    if (passwordInput) {
        passwordInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                submitStudioLogin();
            }
        });
    }

    if (authToken) {
        try {
            const res = await fetchWithAuth('/api/auth/verify');
            const data = await res.json();
            if (data.success) {
                currentUser = data.username || currentUser;
                showAuthenticatedState();
                await loadDatabases();
                refreshClusterStats();
                loadTables();
                renderVectorCanvas();
                return;
            }
        } catch (e) {
            console.warn('Auth verify error:', e);
        }
    }
    showLoginModal();
}

function getAuthHeaders(extraHeaders = {}) {
    const headers = { 'Content-Type': 'application/json', ...extraHeaders };
    if (authToken) {
        headers['Authorization'] = authToken;
    }
    return headers;
}

async function fetchWithAuth(url, options = {}) {
    options.headers = getAuthHeaders(options.headers || {});
    const response = await fetch(url, options);
    if (response.status === 401 && url !== '/api/auth/login') {
        showLoginModal('Session expired or invalid credentials. Please log in.');
    }
    return response;
}

async function submitStudioLogin() {
    const usernameInput = document.getElementById('login-username');
    const passwordInput = document.getElementById('login-password');
    const errorBanner = document.getElementById('login-error-banner');
    const submitBtn = document.getElementById('login-submit-btn');

    const username = usernameInput ? usernameInput.value.trim() : 'admin';
    const password = passwordInput ? passwordInput.value : '';

    if (!username || !password) {
        if (errorBanner) {
            errorBanner.style.display = 'block';
            errorBanner.textContent = 'Please enter both username and password.';
        }
        return;
    }

    if (submitBtn) submitBtn.disabled = true;

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();

        if (data.success) {
            authToken = data.token;
            currentUser = data.username;
            localStorage.setItem('syntricdb_auth_token', authToken);
            localStorage.setItem('syntricdb_user', currentUser);

            if (errorBanner) errorBanner.style.display = 'none';
            showAuthenticatedState();
            await loadDatabases();
            refreshClusterStats();
            loadTables();
            renderVectorCanvas();
        } else {
            if (errorBanner) {
                errorBanner.style.display = 'block';
                errorBanner.textContent = data.error || 'Invalid credentials. Please try again.';
            }
        }
    } catch (e) {
        console.error('Login error:', e);
        if (errorBanner) {
            errorBanner.style.display = 'block';
            errorBanner.textContent = 'Unable to connect to SyntricDB server. Is the engine running?';
        }
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
}

function showLoginModal(errorMsg = '') {
    const modal = document.getElementById('login-modal');
    const errorBanner = document.getElementById('login-error-banner');
    if (modal) modal.style.display = 'flex';
    if (errorBanner) {
        if (errorMsg) {
            errorBanner.style.display = 'block';
            errorBanner.textContent = errorMsg;
        } else {
            errorBanner.style.display = 'none';
        }
    }
}

function showAuthenticatedState() {
    const modal = document.getElementById('login-modal');
    const userBadge = document.getElementById('user-session-badge');
    const userNameElem = document.getElementById('user-display-name');

    if (modal) modal.style.display = 'none';
    if (userBadge) userBadge.style.display = 'flex';
    if (userNameElem) userNameElem.textContent = currentUser || 'admin';
}

function logoutStudio() {
    authToken = '';
    currentUser = '';
    localStorage.removeItem('syntricdb_auth_token');
    localStorage.removeItem('syntricdb_user');

    const userBadge = document.getElementById('user-session-badge');
    if (userBadge) userBadge.style.display = 'none';

    showLoginModal('Logged out successfully.');
}

async function loadDatabases() {
    try {
        const res = await fetchWithAuth('/api/databases');
        const data = await res.json();
        if (data.success && data.databases) {
            const selector = document.getElementById('db-context-selector');
            if (selector) {
                selector.innerHTML = '';
                data.databases.forEach(db => {
                    const opt = document.createElement('option');
                    opt.value = db;
                    opt.textContent = db;
                    if (db === activeDb || db === data.activeDatabase) {
                        opt.selected = true;
                    }
                    selector.appendChild(opt);
                });
                if (data.activeDatabase) activeDb = data.activeDatabase;
            }
        }
    } catch (e) {
        console.error('Error loading databases:', e);
    }
}

function onDatabaseContextChange() {
    const selector = document.getElementById('db-context-selector');
    if (selector) {
        activeDb = selector.value;
    }
    refreshClusterStats();
    loadTables();
    renderVectorCanvas();
}

async function promptCreateDatabase() {
    const dbName = prompt('Enter new database name (e.g. production, analytics, test_db):');
    if (!dbName || !dbName.trim()) return;

    try {
        const res = await fetchWithAuth('/api/databases', {
            method: 'POST',
            body: JSON.stringify({ name: dbName.trim() })
        });
        const data = await res.json();

        if (data.success) {
            alert(data.message);
            activeDb = dbName.trim().toLowerCase();
            await loadDatabases();
            refreshClusterStats();
            loadTables();
        } else {
            alert('Error creating database: ' + (data.error || data.message));
        }
    } catch (e) {
        console.error('Error creating database:', e);
    }
}

function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });

    const sqlInput = document.getElementById('sql-input');
    if (sqlInput) {
        sqlInput.addEventListener('keydown', (e) => {
            if (e.shiftKey && e.key === 'Enter') {
                e.preventDefault();
                runSqlQuery();
            }
        });
    }
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-item').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-page').forEach(page => page.classList.remove('active'));

    const btn = document.querySelector(`.nav-item[data-tab="${tabId}"]`);
    const page = document.getElementById(`tab-${tabId}`);

    if (btn) btn.classList.add('active');
    if (page) page.classList.add('active');

    const titleElem = document.getElementById('page-title');
    const descElem = document.getElementById('page-desc');

    switch (tabId) {
        case 'dashboard':
            titleElem.textContent = 'Cluster & Multi-Database Overview';
            descElem.textContent = 'Real-time status of unified LSM storage, HNSW vector indexes, Raft cluster & database isolation.';
            refreshClusterStats();
            break;
        case 'sql-studio':
            titleElem.textContent = 'SQL & AI Query Studio';
            descElem.textContent = 'Unified query runner supporting SQL, Vector SIMILAR TO, AI_SUMMARIZE, and Full-Text BM25.';
            break;
        case 'vector-explorer':
            titleElem.textContent = 'Vector 2D Explorer';
            descElem.textContent = 'Interactive 2D projection map of vector embeddings and HNSW nearest neighbors search.';
            renderVectorCanvas();
            break;
        case 'rag-assistant':
            titleElem.textContent = 'Native AI RAG Engine';
            descElem.textContent = 'Perform zero-latency vector similarity retrieval to construct augmented contexts for LLM prompts.';
            break;
        case 'tables':
            titleElem.textContent = 'Database Schema & Tables';
            descElem.textContent = 'Explore database schemas, column definitions, and raw record contents per database.';
            loadTables();
            break;
        case 'benchmark':
            titleElem.textContent = 'IOPS Benchmark Suite';
            descElem.textContent = 'Execute high throughput write & vector search performance benchmarks directly on the server.';
            break;
    }
}

async function refreshClusterStats() {
    try {
        const res = await fetchWithAuth('/api/cluster');
        const data = await res.json();

        if (data.success) {
            document.getElementById('metric-writes').textContent = data.writeOps.toLocaleString();
            document.getElementById('metric-reads').textContent = data.readOps.toLocaleString();
            document.getElementById('metric-cache').textContent = data.cacheHitRate.toFixed(1) + '%';
            document.getElementById('metric-nodes').textContent = data.nodes.length + ' Active';

            // Render Nodes Topology
            const container = document.getElementById('cluster-nodes-container');
            container.innerHTML = '';
            data.nodes.forEach(node => {
                const card = document.createElement('div');
                card.className = `node-card ${node.role === 'LEADER' ? 'leader' : ''}`;
                card.innerHTML = `
                    <div class="node-header">
                        <span class="node-title">${node.nodeId}</span>
                        <span class="badge ${node.role === 'LEADER' ? 'cyan' : 'purple'}">${node.role}</span>
                    </div>
                    <div class="node-body">
                        <p>Term: <strong>${node.term}</strong></p>
                        <p>Raft Log Entries: <strong>${node.logCount}</strong></p>
                        <p>Status: <span style="color:var(--accent-green)">HEALTHY</span></p>
                    </div>
                `;
                container.appendChild(card);
            });
        }
    } catch (err) {
        console.error('Error fetching cluster stats:', err);
    }
}

function loadPresetQuery() {
    const selector = document.getElementById('preset-selector');
    const input = document.getElementById('sql-input');

    switch (selector.value) {
        case 'sim':
            input.value = `SELECT id, name, city, age, AI_SUMMARIZE(bio) FROM users WHERE embedding SIMILAR TO 'Java Engineer' AND city='Hyderabad' AND age>25 TOP 5`;
            break;
        case 'ai':
            input.value = `SELECT id, name, AI_SUMMARIZE(bio) AS summary, AI_CLASSIFY(bio, 'Engineer', 'Researcher', 'Manager') AS category FROM users`;
            break;
        case 'match':
            input.value = `SELECT id, name, bio FROM users WHERE MATCH(bio, 'vector search raft')`;
            break;
        case 'scalar':
            input.value = `SELECT * FROM users WHERE city='Hyderabad' ORDER BY age DESC`;
            break;
        case 'insert':
            input.value = `INSERT INTO users VALUES ('usr_200', 'New AI Specialist', 'Bengaluru', 30, 'Principal Scientist', 'Expert in deep learning, vector databases, and high concurrency Java.', AI_EMBED('AI Specialist'))`;
            break;
    }
}

async function runSqlQuery() {
    const sql = document.getElementById('sql-input').value.trim();
    if (!sql) return;

    const planBadge = document.getElementById('plan-badge');
    const timeBadge = document.getElementById('time-badge');
    const planBanner = document.getElementById('plan-banner');
    const planStrategy = document.getElementById('plan-strategy');
    const planDesc = document.getElementById('plan-desc');

    planBadge.textContent = 'Executing...';
    timeBadge.textContent = 'Time: ...';

    try {
        const res = await fetchWithAuth('/api/sql', {
            method: 'POST',
            body: JSON.stringify({ sql, database: activeDb })
        });
        const result = await res.json();

        if (result.success) {
            timeBadge.textContent = `Time: ${result.executionTimeMs.toFixed(2)} ms`;
            planBadge.textContent = result.planStrategy ? `Plan: ${result.planStrategy}` : 'Success';

            if (result.activeDatabase) {
                activeDb = result.activeDatabase;
                const selector = document.getElementById('db-context-selector');
                if (selector) selector.value = activeDb;
            }

            if (result.planStrategy) {
                planBanner.style.display = 'flex';
                planStrategy.textContent = result.planStrategy;
                planDesc.textContent = result.planDescription + ` (Cost Est: ${result.estimatedCost})`;
            } else {
                planBanner.style.display = 'none';
            }

            renderQueryResultTable(result.data, result.message);
            refreshClusterStats();
            loadTables();
        } else {
            planBadge.textContent = 'Error';
            planBanner.style.display = 'none';
            renderErrorTable(result.error);
        }
    } catch (err) {
        console.error('SQL Execution Error:', err);
        planBadge.textContent = 'Network Error';
    }
}

function renderQueryResultTable(rows, message) {
    const headersTr = document.getElementById('result-headers');
    const bodyTbody = document.getElementById('result-body');

    headersTr.innerHTML = '';
    bodyTbody.innerHTML = '';

    if (!rows || rows.length === 0) {
        headersTr.innerHTML = `<th>Status</th>`;
        bodyTbody.innerHTML = `<tr><td>${message || 'No rows returned.'}</td></tr>`;
        return;
    }

    const columns = Object.keys(rows[0]);
    columns.forEach(col => {
        const th = document.createElement('th');
        th.textContent = col;
        headersTr.appendChild(th);
    });

    rows.forEach(row => {
        const tr = document.createElement('tr');
        columns.forEach(col => {
            const td = document.createElement('td');
            let val = row[col];
            if (typeof val === 'object' && val !== null) {
                val = JSON.stringify(val);
            }
            td.textContent = val;
            tr.appendChild(td);
        });
        bodyTbody.appendChild(tr);
    });
}

function renderErrorTable(errMsg) {
    const headersTr = document.getElementById('result-headers');
    const bodyTbody = document.getElementById('result-body');

    headersTr.innerHTML = `<th style="color:red">SQL Error</th>`;
    bodyTbody.innerHTML = `<tr><td style="color:#ef4444; font-weight:bold">${errMsg}</td></tr>`;
}

async function loadTables() {
    try {
        const res = await fetchWithAuth(`/api/tables?db=${encodeURIComponent(activeDb)}`);
        const data = await res.json();
        const container = document.getElementById('tables-container');
        if (!container) return;

        container.innerHTML = '';
        if (data.tables && data.tables.length > 0) {
            data.tables.forEach(t => {
                const box = document.createElement('div');
                box.className = 'panel margin-top';
                box.innerHTML = `
                    <div class="panel-header">
                        <h3>Database: <code>${t.database}</code> | Table: <code>${t.tableName}</code> (${t.rowCount} rows)</h3>
                        <span class="badge cyan">Primary Key: ${t.primaryKey || 'None'}</span>
                    </div>
                    <p style="font-size:13px; color:var(--text-secondary); margin-bottom:12px;">Vector Column: <strong>${t.vectorColumn || 'None'}</strong></p>
                    <table class="data-table">
                        <thead>
                            <tr><th>Column Name</th><th>Data Type</th><th>Vector Dim</th><th>Indexed</th><th>Primary Key</th></tr>
                        </thead>
                        <tbody>
                            ${t.columns.map(c => `
                                <tr>
                                    <td><strong>${c.name}</strong></td>
                                    <td>${c.type}</td>
                                    <td>${c.vectorDimension || '-'}</td>
                                    <td>${c.indexed ? 'Yes' : 'No'}</td>
                                    <td>${c.primaryKey ? 'Yes' : 'No'}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                `;
                container.appendChild(box);
            });
        } else {
            container.innerHTML = `<div class="panel margin-top"><p style="color:var(--text-secondary)">No tables found in database <code>${activeDb}</code>. Run <code>CREATE TABLE ${activeDb}.sample_table (...)</code> in SQL Studio to create one!</p></div>`;
        }
    } catch (e) {
        console.error('Error loading tables:', e);
    }
}

// Interactive Vector Canvas 2D PCA Map Simulation
let vectorPoints = [];

async function renderVectorCanvas() {
    const canvas = document.getElementById('vectorCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    try {
        const res = await fetchWithAuth('/api/sql', {
            method: 'POST',
            body: JSON.stringify({ sql: "SELECT * FROM users", database: activeDb })
        });
        const result = await res.json();
        if (result.success && result.data) {
            vectorPoints = result.data.map((row, idx) => {
                const idVal = row.id ? row.id.toString() : 'row_' + idx;
                const seed = idVal.split('').reduce((a, b) => a + b.charCodeAt(0), 0);
                const x = 150 + (seed * 17) % 600;
                const y = 80 + (seed * 23) % 300;
                return { id: idVal, name: row.name || row.title || idVal, x, y, isSearchTarget: false };
            });
        }
    } catch (e) {
        console.error(e);
    }

    drawCanvas(ctx, canvas);
}

function drawCanvas(ctx, canvas) {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = '#1e293b';
    ctx.lineWidth = 1;
    for (let x = 0; x < canvas.width; x += 50) {
        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
    }
    for (let y = 0; y < canvas.height; y += 50) {
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
    }

    vectorPoints.forEach(p => {
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.isSearchTarget ? 10 : 7, 0, 2 * Math.PI);
        ctx.fillStyle = p.isSearchTarget ? '#06b6d4' : '#a855f7';
        ctx.fill();

        if (p.isSearchTarget) {
            ctx.strokeStyle = '#06b6d4';
            ctx.lineWidth = 2;
            ctx.stroke();
        }

        ctx.fillStyle = '#f9fafb';
        ctx.font = '12px Inter';
        ctx.fillText(p.name, p.x + 12, p.y + 4);
    });
}

async function searchVectorCanvas() {
    const query = document.getElementById('vector-search-input').value.trim();
    if (!query) return;

    try {
        const res = await fetchWithAuth('/api/vector/search', {
            method: 'POST',
            body: JSON.stringify({ database: activeDb, table: 'users', column: 'embedding', query, limit: 3 })
        });
        const data = await res.json();

        if (data.success) {
            const topIds = data.results.map(r => r.id);
            vectorPoints.forEach(p => {
                p.isSearchTarget = topIds.includes(p.id);
            });

            const canvas = document.getElementById('vectorCanvas');
            drawCanvas(canvas.getContext('2d'), canvas);

            const container = document.getElementById('vector-knn-results');
            container.innerHTML = `<h3>HNSW Nearest Neighbor Results in database <code>${activeDb}</code> (Latency: ${data.executionTimeMs.toFixed(2)} ms):</h3>`;
            data.results.forEach(r => {
                const box = document.createElement('div');
                box.className = 'panel margin-top';
                box.innerHTML = `
                    <div style="display:flex; justify-content:space-between;">
                        <strong>${r.id} - ${r.record ? (r.record.name || r.record.title) : ''}</strong>
                        <span class="badge cyan">Similarity: ${(r.similarity * 100).toFixed(1)}%</span>
                    </div>
                    <p style="font-size:13px; color:var(--text-secondary); margin-top:4px;">${r.record ? (r.record.bio || r.record.category) : ''}</p>
                `;
                container.appendChild(box);
            });
        }
    } catch (e) {
        console.error(e);
    }
}

async function runBenchmarkTest() {
    const count = document.getElementById('bench-count').value;
    const btn = document.getElementById('bench-run-btn');
    btn.disabled = true;
    btn.textContent = '🔥 Benchmarking in progress...';

    try {
        const res = await fetchWithAuth('/api/benchmark', {
            method: 'POST',
            body: JSON.stringify({ database: activeDb, count: parseInt(count) })
        });
        const data = await res.json();

        if (data.success) {
            document.getElementById('bench-results').style.display = 'grid';
            document.getElementById('bench-write-ops').textContent = data.writesPerSec.toLocaleString();
            document.getElementById('bench-write-time').textContent = `Total Time for ${data.insertedCount} writes into '${activeDb}': ${data.writeTimeMs} ms`;

            document.getElementById('bench-search-ops').textContent = data.searchesPerSec.toLocaleString();
            document.getElementById('bench-search-time').textContent = `Total Time for ${data.searchCount} vector searches in '${activeDb}': ${data.searchTimeMs} ms`;
        }
    } catch (e) {
        console.error('Benchmark Error', e);
    } finally {
        btn.disabled = false;
        btn.textContent = '🔥 Start Performance Test';
        refreshClusterStats();
    }
}

async function runRagQuery() {
    const prompt = document.getElementById('rag-prompt-input').value.trim();
    if (!prompt) return;

    try {
        const res = await fetchWithAuth('/api/ai/rag', {
            method: 'POST',
            body: JSON.stringify({ database: activeDb, table: 'users', column: 'embedding', prompt, limit: 3 })
        });
        const data = await res.json();

        if (data.success) {
            document.getElementById('rag-output-container').style.display = 'block';
            document.getElementById('rag-answer-text').textContent = data.generatedAnswer;
            document.getElementById('rag-augmented-prompt').textContent = data.augmentedPrompt;
        }
    } catch (e) {
        console.error('RAG Query Error:', e);
    }
}
