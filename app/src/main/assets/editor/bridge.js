// Bridge between CodeMirror 6 and AndroidFileBridge (FileBridge.kt)

let currentFilePath = "init.sql";
let editorInstance = null;
let activeDatabase = "mariadb";

const DEFAULT_FILES = {
    "init.sql": "-- MariaDB 11.x Initialization Script\nCREATE DATABASE IF NOT EXISTS app_dev;\nUSE app_dev;\n\nCREATE TABLE IF NOT EXISTS users (\n    id INT AUTO_INCREMENT PRIMARY KEY,\n    username VARCHAR(50) NOT NULL,\n    email VARCHAR(100) NOT NULL,\n    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n);\n\nINSERT INTO users (username, email) VALUES\n('alice', 'alice@linuxxx.local'),\n('bob', 'bob@linuxxx.local');\n\nSELECT * FROM users;\n",
    "seed_redis.py": "# Redis 7.x Data Seeder Script\nimport redis\n\nr = redis.Redis(host='127.0.0.1', port=6379, db=0)\nr.set('app:version', '1.0.0-linuxxx')\nr.set('app:status', 'running')\nr.hset('user:1001', mapping={'name': 'Dev User', 'role': 'Admin'})\n\nprint('Keys in Redis:', r.keys('*'))\nprint('User 1001:', r.hgetall('user:1001'))\n",
    "mongo_indexes.js": "// MongoDB 7.x Schema & Indexes\nconst db = connect('mongodb://127.0.0.1:27017/analytics');\n\ndb.telemetry.createIndex({ timestamp: -1 });\ndb.telemetry.createIndex({ eventType: 1 });\n\ndb.telemetry.insertOne({\n    eventType: 'BOOT_COMPLETE',\n    timestamp: new Date(),\n    metadata: { memoryMb: 64, mode: 'rootless-proot' }\n});\n\nprint('Total logs:', db.telemetry.countDocuments());\n"
};

function hasNativeBridge() {
    return typeof window.AndroidFileBridge !== "undefined";
}

function initApplication() {
    // Check low-end device heuristic
    const isLowEnd = (navigator.deviceMemory && navigator.deviceMemory < 3);

    // Initialize CodeMirror 6
    editorInstance = window.CodeMirrorBundle.initEditor({
        parent: document.getElementById("editor-host"),
        doc: DEFAULT_FILES["init.sql"],
        language: "sql",
        isLowEnd: isLowEnd,
        onChange: (content) => {
            updateStatus("Unsaved changes...");
        }
    });

    // Populate initial files if workspace is empty
    ensureDefaultFiles();
    refreshFileList();
    switchTab("editor");
    selectFile("init.sql");
}

function ensureDefaultFiles() {
    if (!hasNativeBridge()) return;
    try {
        const listJson = window.AndroidFileBridge.listDir("");
        const files = JSON.parse(listJson || "[]");
        if (files.length === 0) {
            for (const [name, content] of Object.entries(DEFAULT_FILES)) {
                window.AndroidFileBridge.writeFile(name, content);
            }
        }
    } catch (e) {
        console.error("Error ensuring default files:", e);
    }
}

function refreshFileList() {
    const listEl = document.getElementById("file-list");
    listEl.innerHTML = "";

    let files = [];
    if (hasNativeBridge()) {
        try {
            const listJson = window.AndroidFileBridge.listDir("");
            files = JSON.parse(listJson || "[]");
        } catch (e) {
            console.error("Failed to list files:", e);
        }
    }

    if (files.length === 0) {
        // Fallback to local default files list
        files = Object.keys(DEFAULT_FILES).map(name => ({ name: name, isDir: false, size: DEFAULT_FILES[name].length }));
    }

    files.forEach(f => {
        const li = document.createElement("li");
        li.className = "file-item" + (f.name === currentFilePath ? " selected" : "");
        const icon = f.isDir ? "📁" : getFileIcon(f.name);
        li.innerHTML = `<span>${icon}</span><span>${f.name}</span>`;
        li.onclick = () => selectFile(f.name);
        listEl.appendChild(li);
    });
}

function getFileIcon(name) {
    if (name.endsWith(".sql")) return "🗄️";
    if (name.endsWith(".py")) return "🐍";
    if (name.endsWith(".js") || name.endsWith(".json")) return "📜";
    if (name.endsWith(".sh")) return "⚡";
    if (name.endsWith(".cnf") || name.endsWith(".conf")) return "⚙️";
    return "📄";
}

function detectLanguage(filename) {
    if (filename.endsWith(".sql")) return "sql";
    if (filename.endsWith(".py")) return "python";
    if (filename.endsWith(".js") || filename.endsWith(".json")) return "javascript";
    return "javascript";
}

function selectFile(filename) {
    currentFilePath = filename;
    document.getElementById("active-file-title").innerText = filename;

    let content = "";
    if (hasNativeBridge()) {
        try {
            content = window.AndroidFileBridge.readFile(filename);
        } catch (e) {
            console.warn("Could not read from bridge, using fallback:", e);
            content = DEFAULT_FILES[filename] || "";
        }
    } else {
        content = DEFAULT_FILES[filename] || "";
    }

    if (editorInstance) {
        editorInstance.setValue(content);
        editorInstance.setLanguage(detectLanguage(filename));
    }

    refreshFileList();
    updateStatus(`Loaded ${filename}`);
}

function saveCurrentFile() {
    if (!editorInstance) return;
    const content = editorInstance.getValue();

    if (hasNativeBridge()) {
        try {
            window.AndroidFileBridge.writeFile(currentFilePath, content);
            updateStatus(`Saved ${currentFilePath} (${content.length} bytes)`);
        } catch (e) {
            updateStatus(`Save error: ${e.message}`);
        }
    } else {
        DEFAULT_FILES[currentFilePath] = content;
        updateStatus(`Saved ${currentFilePath} locally`);
    }
}

function createNewFile() {
    const name = prompt("Enter file name (e.g. script.sql, task.py):");
    if (!name || name.trim() === "") return;
    const cleanName = name.trim();

    if (hasNativeBridge()) {
        window.AndroidFileBridge.writeFile(cleanName, "-- New File\n");
    } else {
        DEFAULT_FILES[cleanName] = "-- New File\n";
    }

    selectFile(cleanName);
}

function switchTab(tabId) {
    document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".view-panel").forEach(p => p.classList.remove("active"));

    const tabBtn = document.getElementById(`tab-${tabId}`);
    const panel = document.getElementById(`panel-${tabId}`);
    if (tabBtn) tabBtn.classList.add("active");
    if (panel) panel.classList.add("active");

    if (tabId === "editor" && editorInstance) {
        // Refresh editor layout
        setTimeout(() => editorInstance.view.requestMeasure(), 50);
    }
}

function setDbEngine(engine) {
    activeDatabase = engine;
    const chips = {
        mariadb: document.getElementById("chip-mariadb"),
        redis: document.getElementById("chip-redis"),
        mongodb: document.getElementById("chip-mongodb")
    };

    chips.mariadb.className = "db-chip" + (engine === "mariadb" ? " active-mariadb" : "");
    chips.redis.className = "db-chip" + (engine === "redis" ? " active-redis" : "");
    chips.mongodb.className = "db-chip" + (engine === "mongodb" ? " active-mongodb" : "");

    const input = document.getElementById("query-input");
    if (engine === "mariadb") {
        input.value = "SELECT VERSION(), CURRENT_TIMESTAMP, USER();";
    } else if (engine === "redis") {
        input.value = "INFO server";
    } else if (engine === "mongodb") {
        input.value = "db.runCommand({ ping: 1 })";
    }
}

function executeQuery() {
    const query = document.getElementById("query-input").value.trim();
    const resultBox = document.getElementById("query-results");
    if (!query) return;

    resultBox.innerText = `Connecting to 127.0.0.1 [${activeDatabase}] and executing...\n`;

    if (hasNativeBridge() && typeof window.AndroidFileBridge.runQuery === "function") {
        try {
            const rawJson = window.AndroidFileBridge.runQuery(activeDatabase, query);
            let formatted = rawJson;
            try {
                const parsed = JSON.parse(rawJson);
                formatted = JSON.stringify(parsed, null, 2);
            } catch (_) {}
            resultBox.innerText = formatted;
            updateStatus(`Query executed successfully on ${activeDatabase}`);
        } catch (e) {
            resultBox.innerText = `Execution error:\n${e.message || e}`;
            updateStatus(`Query failed: ${e.message}`);
        }
    } else {
        resultBox.innerText = `Native AndroidFileBridge.runQuery not attached in this preview.\nExecuted simulated query: "${query}" on ${activeDatabase}.\nStatus: Ready.`;
    }
}

function updateStatus(text) {
    const el = document.getElementById("status-text");
    if (el) el.innerText = text;
}

window.addEventListener("DOMContentLoaded", initApplication);
