package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.LinuxStackApp
import com.example.core.BootstrapExtractor
import com.example.core.LinuxEnvManager
import com.example.data.repository.DatabaseRepository
import com.example.service.DatabaseStackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

data class PortInfo(
    val port: Int,
    val name: String,
    val isOpen: Boolean = false,
    val latencyMs: Long = -1L
)

data class TerminalLogItem(
    val id: Long,
    val timestamp: Long,
    val tag: String,
    val message: String,
    val isError: Boolean = false
)

enum class StudioTab(val label: String) {
    QUERY("Query Console"),
    BROWSER("Data Browser"),
    CODEGEN("Polyglot Hub"),
    SEEDER("1-Click Seeder")
}

enum class AppScreen(val title: String, val route: String) {
    SETUP("First-Launch Setup", "setup"),
    STUDIO("Dev Studio", "studio"),
    CODEMIRROR("Code Editor", "codemirror"),
    DAEMONS("Database Daemons", "daemons"),
    TERMINAL("Interactive Terminal", "terminal")
}

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val isExtracted: Boolean = false,
    val isDarkMode: Boolean = true,
    val currentScreen: AppScreen = AppScreen.STUDIO,
    val extractionProgress: BootstrapExtractor.ExtractionProgress = BootstrapExtractor.ExtractionProgress(),
    val ports: Map<Int, PortInfo> = mapOf(
        3306 to PortInfo(3306, "MariaDB"),
        6379 to PortInfo(6379, "Redis"),
        27017 to PortInfo(27017, "MongoDB")
    ),
    val logs: List<TerminalLogItem> = emptyList(),
    val isAutoScrollEnabled: Boolean = true,
    val prefixPath: String = "",
    val homePath: String = "",
    // Developer Code Playground State
    val playgroundCode: String = """
// Node.js HTTP Web Server & API Playground
const http = require('http');

const PORT = 3000;
const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
  res.end(JSON.stringify({
    status: 'online',
    runtime: 'Node.js on Android',
    loopback: '127.0.0.1',
    timestamp: new Date().toISOString(),
    databases: {
      mariadb: '127.0.0.1:3306',
      redis: '127.0.0.1:6379',
      mongodb: '127.0.0.1:27017'
    },
    message: 'Hello from your Linux Developer Code Playground!'
  }, null, 2));
});

server.listen(PORT, '127.0.0.1', () => {
  console.log('🚀 Node.js server running live at http://127.0.0.1:' + PORT);
});
""".trimIndent(),
    val playgroundLanguage: String = "javascript",
    val playgroundOutput: String = "",
    val playgroundIsRunning: Boolean = false,
    val playgroundServerPort: Int = 3000,
    val previewUrl: String = "http://127.0.0.1:3000",
    // Tunable database and telemetry sliders for runtime configuration
    val wiredTigerCacheSizeMb: Int = 256,
    val innodbBufferPoolMb: Int = 128,
    val maxLogBufferSize: Int = 300,
    val probeIntervalSec: Int = 5,
    // Interactive Terminal Shell Stream State
    val isInteractiveSessionActive: Boolean = false,
    val activeSessionTitle: String = "none",
    val activeSessionPid: Long = -1L,
    // Universal Studio & Explorer State
    val studioTab: StudioTab = StudioTab.QUERY,
    val studioEngine: String = "mariadb",
    val studioSqlQueryInput: String = "SELECT * FROM users LIMIT 10;",
    val studioRedisCommandInput: String = "KEYS *",
    val studioMongoQueryInput: String = "find audit_logs",
    val studioSqlResult: com.example.core.SqlResult = com.example.core.SqlResult(),
    val studioRedisResult: com.example.core.RedisResult = com.example.core.RedisResult(),
    val studioMongoResult: com.example.core.MongoResult = com.example.core.MongoResult(),
    val studioIsLoading: Boolean = false,
    val studioStatusMessage: String = "",
    val studioRedisKeys: List<com.example.core.RedisKeyInfo> = emptyList(),
    val studioSqlTables: List<String> = emptyList(),
    val studioMongoCollections: List<String> = emptyList(),
    val studioHealthMap: Map<String, com.example.core.DbHealth> = emptyMap(),
    val studioBenchmarkResult: com.example.core.BenchmarkResult? = null,
    val studioSelectedLanguage: String = "nodejs",
    val studioGeneratedSnippet: String = "",
    val studioSeedResult: com.example.core.FullStackSeedResult? = null
)

class MainViewModel(
    application: Application,
    private val repository: DatabaseRepository,
    private val linuxEnvManager: LinuxEnvManager,
    private val bootstrapExtractor: BootstrapExtractor,
    private val ubuntuRootfsManager: com.example.core.UbuntuRootfsManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        MainUiState(
            isExtracted = bootstrapExtractor.isExtracted(),
            prefixPath = linuxEnvManager.PREFIX,
            homePath = linuxEnvManager.HOME
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val studioManager = com.example.core.UniversalDatabaseStudioManager(application)

    private var portPollingJob: Job? = null
    private var interactiveProcess: Process? = null
    private var interactiveWriter: java.io.BufferedWriter? = null
    private var interactiveReaderJob: Job? = null

    init {
        val initialSnippet = studioManager.generatePolyglotSnippet("nodejs")
        _uiState.update { it.copy(studioGeneratedSnippet = initialSnippet) }
        
        // Ensure embedded database appliance is online immediately
        try {
            com.example.core.EmbeddedDatabaseStackServer.getInstance(application).startServers { tag, msg ->
                appendLog(tag, msg)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "Embedded server init: ${e.message}")
        }

        // Start continuous port probing & health polling immediately
        startPortPolling()
        testStudioHealth()

        // Collect service running state from Foreground Service
        viewModelScope.launch {
            DatabaseStackService.isRunning.collect { running ->
                _uiState.update { it.copy(isServiceRunning = running) }
            }
        }

        // Collect live log stream from Foreground Service
        viewModelScope.launch {
            DatabaseStackService.logStream.collect { msg ->
                val newItem = TerminalLogItem(
                    id = System.nanoTime(),
                    timestamp = msg.timestamp,
                    tag = msg.tag,
                    message = msg.text,
                    isError = msg.isError
                )
                _uiState.update { state ->
                    val updated = (state.logs + newItem).takeLast(400)
                    state.copy(logs = updated)
                }
            }
        }

        // Collect historical logs from Room Database
        viewModelScope.launch {
            repository.recentLogs.collect { dbLogs ->
                if (_uiState.value.logs.isEmpty() && dbLogs.isNotEmpty()) {
                    val mapped = dbLogs.reversed().map {
                        TerminalLogItem(
                            id = it.id,
                            timestamp = it.timestamp,
                            tag = it.tag,
                            message = it.message,
                            isError = it.level == "ERROR"
                        )
                    }
                    _uiState.update { it.copy(logs = mapped) }
                }
            }
        }

        // Initial check for extraction, auto-trigger extraction if not yet extracted
        val alreadyExtracted = bootstrapExtractor.isExtracted()
        _uiState.update { it.copy(isExtracted = alreadyExtracted) }
        if (!alreadyExtracted) {
            extractBootstrap(force = false)
        }

        // Auto-boot database stack service
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            try {
                DatabaseStackService.start(application)
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Service auto-start: ${e.message}")
            }
        }
    }

    fun checkExtractionState() {
        _uiState.update { it.copy(isExtracted = bootstrapExtractor.isExtracted()) }
    }

    fun toggleService() {
        val currentRunning = _uiState.value.isServiceRunning
        val context = getApplication<Application>()
        if (currentRunning) {
            DatabaseStackService.stop(context)
        } else {
            DatabaseStackService.start(context)
        }
        probePortsNow()
    }

    fun extractBootstrap(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    extractionProgress = BootstrapExtractor.ExtractionProgress(isExtracting = true)
                )
            }
            val result = bootstrapExtractor.extractBootstrap(force = force) { progress ->
                _uiState.update { it.copy(extractionProgress = progress) }
            }
            _uiState.update {
                it.copy(
                    isExtracted = bootstrapExtractor.isExtracted(),
                    extractionProgress = BootstrapExtractor.ExtractionProgress(
                        isExtracting = false,
                        isCompleted = result.isSuccess,
                        errorMessage = result.exceptionOrNull()?.message
                    )
                )
            }
            if (result.isSuccess) {
                appendLog("BOOTSTRAP", "Extracted ${result.getOrNull()} items successfully to ${linuxEnvManager.PREFIX}")
            } else {
                appendLog("BOOTSTRAP", "Extraction failed: ${result.exceptionOrNull()?.message}", isError = true)
            }
        }
    }

    private fun startPortPolling() {
        portPollingJob?.cancel()
        portPollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                probeAllPorts()
                delay(2000L)
            }
        }
    }

    private fun stopPortPolling() {
        portPollingJob?.cancel()
        portPollingJob = null
    }

    private fun resetPortStatus() {
        _uiState.update { state ->
            state.copy(
                ports = state.ports.mapValues { (_, info) ->
                    info.copy(isOpen = false, latencyMs = -1L)
                }
            )
        }
    }

    fun probePortsNow() {
        viewModelScope.launch(Dispatchers.IO) {
            appendLog("PROBE", "\u001B[36m[PROBE]\u001B[0m Scanning loopback sockets (127.0.0.1:3306, 6379, 27017)...")
            val (openCount, closedCount) = probeAllPorts(logDetails = true)
            
            val activePorts = getListeningTcpPorts()
            if (activePorts.isNotEmpty()) {
                appendLog("PROBE", "Discovered active TCP listener ports: ${activePorts.joinToString(", ") { ":$it" }}")
            }

            testStudioHealth()
            appendLog("PROBE", "\u001B[32mScan complete: $openCount online, $closedCount closed.\u001B[0m")
        }
    }

    /**
     * Probing ports 3306, 6379, and 27017 using loopback socket connection + embedded engine status.
     */
    private suspend fun probeAllPorts(logDetails: Boolean = false): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val portList = listOf(3306, 6379, 27017)
        val newMap = mutableMapOf<Int, PortInfo>()
        var openCount = 0
        var closedCount = 0

        for (port in portList) {
            val (isOpen, latency) = probeSocket(port)
            val name = when (port) {
                3306 -> "MariaDB (MySQL)"
                6379 -> "Redis"
                27017 -> "MongoDB"
                else -> "Port $port"
            }
            newMap[port] = PortInfo(
                port = port,
                name = name,
                isOpen = isOpen,
                latencyMs = latency
            )
            if (isOpen) {
                openCount++
                if (logDetails) {
                    appendLog("PROBE", "\u001B[32m✔ $name :$port -> OPEN (${latency}ms latency)\u001B[0m")
                }
            } else {
                closedCount++
                if (logDetails) {
                    appendLog("PROBE", "\u001B[90m✖ $name :$port -> CLOSED (Not listening on 127.0.0.1)\u001B[0m")
                }
            }
        }

        _uiState.update { it.copy(ports = newMap) }
        Pair(openCount, closedCount)
    }

    private fun getListeningTcpPorts(): List<Int> {
        val ports = mutableListOf<Int>()
        val files = listOf(File("/proc/net/tcp"), File("/proc/net/tcp6"))
        for (f in files) {
            if (f.exists() && f.canRead()) {
                try {
                    f.forEachLine { line ->
                        val tokens = line.trim().split(Regex("\\s+"))
                        if (tokens.size >= 4 && tokens[3].equals("0A", ignoreCase = true)) { // 0A = TCP_LISTEN
                            val localAddr = tokens[1]
                            val portHex = localAddr.substringAfterLast(":", "")
                            val portInt = portHex.toIntOrNull(16)
                            if (portInt != null && !ports.contains(portInt)) {
                                ports.add(portInt)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        return ports.sorted()
    }

    private fun probeSocket(port: Int): Pair<Boolean, Long> {
        val embeddedEngine = com.example.core.EmbeddedDatabaseStackServer.getInstance(getApplication())
        val isEmbeddedListening = embeddedEngine.isPortListening(port)

        val start = System.currentTimeMillis()
        for (host in listOf("127.0.0.1", "localhost")) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(host, port), 250)
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(0L)
                return Pair(true, latency)
            } catch (_: Throwable) {
                // Next target
            } finally {
                try {
                    socket?.close()
                } catch (_: Throwable) {}
            }
        }

        if (isEmbeddedListening) {
            return Pair(true, 1L)
        }

        return Pair(false, -1L)
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _uiState.update { it.copy(logs = emptyList()) }
        }
    }

    fun setScreen(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    fun setAutoScroll(enabled: Boolean) {
        _uiState.update { it.copy(isAutoScrollEnabled = enabled) }
    }

    fun setWiredTigerCacheSizeMb(sizeMb: Int) {
        _uiState.update { it.copy(wiredTigerCacheSizeMb = sizeMb) }
        appendLog("CONFIG", "Adjusted WiredTiger cache size slider: ${sizeMb} MB")
    }

    fun setInnodbBufferPoolMb(sizeMb: Int) {
        _uiState.update { it.copy(innodbBufferPoolMb = sizeMb) }
        appendLog("CONFIG", "Adjusted MariaDB InnoDB buffer pool slider: ${sizeMb} MB")
    }

    fun setProbeIntervalSec(sec: Int) {
        _uiState.update { it.copy(probeIntervalSec = sec) }
        if (_uiState.value.isServiceRunning) {
            startPortPolling()
        }
        appendLog("CONFIG", "Adjusted socket probe polling interval slider: ${sec}s")
    }

    fun setMaxLogBufferSize(limit: Int) {
        _uiState.update { state ->
            state.copy(
                maxLogBufferSize = limit,
                logs = state.logs.takeLast(limit)
            )
        }
        appendLog("CONFIG", "Adjusted terminal console buffer size slider: $limit lines")
    }

    private var activePlaygroundProcess: Process? = null

    fun setPlaygroundCode(code: String) {
        _uiState.update { it.copy(playgroundCode = code) }
    }

    fun setPlaygroundLanguage(language: String) {
        _uiState.update { it.copy(playgroundLanguage = language) }
    }

    fun setPreviewUrl(url: String) {
        _uiState.update { it.copy(previewUrl = url) }
    }

    fun runPlaygroundScript(code: String, language: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return

        stopPlaygroundScript()

        _uiState.update {
            it.copy(
                playgroundIsRunning = true,
                playgroundOutput = "⚡ Executing $language script on Linux userland...\n"
            )
        }

        appendLog("PLAYGROUND", "▶ Running $language script...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val homeDir = File(linuxEnvManager.HOME)
                homeDir.mkdirs()

                val (scriptFile, runCommand, detectedPort) = when (language.lowercase()) {
                    "javascript", "js", "node" -> {
                        val file = File(homeDir, "playground.js")
                        file.writeText(trimmed)
                        val port = Regex("""(?:PORT|listen\()\s*[:=]?\s*(\d{2,5})""").find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 3000
                        val nodeBin = if (File(linuxEnvManager.BINDIR, "node").exists()) "${linuxEnvManager.BINDIR}/node" else "node"
                        Triple(file, "$nodeBin ${file.absolutePath} || sh -c 'echo \"[Node.js Playground] Output:\"; node ${file.absolutePath} 2>&1 || nodejs ${file.absolutePath}'", port)
                    }
                    "python", "py" -> {
                        val file = File(homeDir, "playground.py")
                        file.writeText(trimmed)
                        val port = Regex("""(?:port|PORT)\s*[:=]\s*(\d{2,5})""").find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: 8000
                        val pyBin = if (File(linuxEnvManager.BINDIR, "python3").exists()) "${linuxEnvManager.BINDIR}/python3" else "python3"
                        Triple(file, "$pyBin ${file.absolutePath} 2>&1 || python ${file.absolutePath}", port)
                    }
                    "sql" -> {
                        val file = File(homeDir, "playground.sql")
                        file.writeText(trimmed)
                        Triple(file, "mysql -u root -e \"source ${file.absolutePath}\" 2>&1 || mysqladmin status", 3306)
                    }
                    "redis" -> {
                        val file = File(homeDir, "playground.redis")
                        file.writeText(trimmed)
                        Triple(file, "cat ${file.absolutePath} | redis-cli 2>&1 || redis-cli ping", 6379)
                    }
                    else -> {
                        val file = File(homeDir, "playground.sh")
                        file.writeText(trimmed)
                        file.setExecutable(true, false)
                        Triple(file, "sh ${file.absolutePath}", 3000)
                    }
                }

                _uiState.update {
                    it.copy(
                        playgroundServerPort = detectedPort,
                        previewUrl = "http://127.0.0.1:$detectedPort"
                    )
                }

                val env = linuxEnvManager.getLinuxEnvironment()
                val shellBinary = if (File(linuxEnvManager.BINDIR, "bash").exists() && File(linuxEnvManager.BINDIR, "bash").canExecute()) {
                    File(linuxEnvManager.BINDIR, "bash").absolutePath
                } else {
                    "/system/bin/sh"
                }

                val pb = ProcessBuilder(shellBinary, "-c", runCommand)
                pb.directory(homeDir)
                pb.environment().putAll(env)
                pb.redirectErrorStream(true)

                val process = pb.start()
                activePlaygroundProcess = process

                val reader = process.inputStream.bufferedReader()
                var line: String? = reader.readLine()
                while (line != null) {
                    val currentLine = line
                    _uiState.update { state ->
                        state.copy(playgroundOutput = state.playgroundOutput + currentLine + "\n")
                    }
                    appendLog("PLAYGROUND-OUT", currentLine)
                    line = reader.readLine()
                }

                val exitCode = process.waitFor()
                _uiState.update {
                    it.copy(
                        playgroundIsRunning = false,
                        playgroundOutput = it.playgroundOutput + "\n✨ Execution finished with exit code $exitCode\n"
                    )
                }
                appendLog("PLAYGROUND", "Script finished [exit $exitCode]")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        playgroundIsRunning = false,
                        playgroundOutput = it.playgroundOutput + "\n❌ Execution error: ${e.message}\n"
                    )
                }
                appendLog("PLAYGROUND-ERR", "Error executing script: ${e.message}", isError = true)
            } finally {
                activePlaygroundProcess = null
            }
        }
    }

    fun stopPlaygroundScript() {
        try {
            activePlaygroundProcess?.destroy()
            activePlaygroundProcess = null
            _uiState.update {
                it.copy(
                    playgroundIsRunning = false,
                    playgroundOutput = it.playgroundOutput + "\n⏹ Script process stopped by user.\n"
                )
            }
            appendLog("PLAYGROUND", "⏹ Playground script stopped.")
        } catch (_: Exception) {}
    }

    fun installPackage(pkgName: String) {
        val trimmed = pkgName.trim()
        if (trimmed.isEmpty()) return
        appendLog("PKG", "📦 Installing package '$trimmed'...")
        executeCommand("apk add $trimmed || pkg install $trimmed || npm install $trimmed")
    }

    fun executeCommand(commandStr: String) {
        val trimmed = commandStr.trim()
        if (trimmed.isEmpty()) return
        appendLog("CLI", "$ $trimmed")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val env = linuxEnvManager.getLinuxEnvironment()
                // Prefer binary bash if available in usr/bin, otherwise fallback to system shell
                val shellBinary = if (File(linuxEnvManager.BINDIR, "bash").exists() && File(linuxEnvManager.BINDIR, "bash").canExecute()) {
                    File(linuxEnvManager.BINDIR, "bash").absolutePath
                } else {
                    "/system/bin/sh"
                }

                val pb = ProcessBuilder(shellBinary, "-c", trimmed)
                pb.directory(File(linuxEnvManager.HOME))
                pb.environment().putAll(env)
                pb.redirectErrorStream(true)
                val process = pb.start()
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        appendLog("OUT", line)
                    }
                }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    appendLog("CLI", "Process exited with code $exitCode", isError = true)
                } else {
                    appendLog("CLI", "Process completed successfully [exit 0]")
                }
            } catch (e: Exception) {
                appendLog("CLI-ERR", "Execution failed: ${e.message}", isError = true)
            }
        }
    }

    fun startInteractiveShell(command: String = "") {
        killInteractiveSession()
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val env = linuxEnvManager.getLinuxEnvironment()
                
                // Set PROOT_LOADER paths in the interactive environment
                val context = getApplication<Application>().applicationContext
                val installer = com.example.core.ProotInstaller(context)
                env["PROOT_LOADER"] = installer.getLoaderPath()
                env["PROOT_LOADER_32"] = installer.getLoader32Path()
                env["PROOT_NO_SECCOMP"] = "1"
                
                val cmdList = if (ubuntuRootfsManager.isBootstrapReady()) {
                    val rootfsDir = ubuntuRootfsManager.rootfsDir
                    val dataDir = ubuntuRootfsManager.dataDir
                    val workspaceDir = File(context.filesDir, "workspace")
                    val proot = installer.getExecutableProot().absolutePath
                    val rootfs = rootfsDir.absolutePath
                    val data = dataDir.absolutePath
                    
                    if (command.isBlank()) {
                        listOf(
                            proot,
                            "-0",
                            "-r", rootfs,
                            "-b", "/dev",
                            "-b", "/proc",
                            "-b", "/sys",
                            "-b", "$data/mysql:/var/lib/mysql",
                            "-b", "$data/redis:/var/lib/redis",
                            "-b", "$data/mongodb:/var/lib/mongodb",
                            "-b", "$data/run:/var/run",
                            "-b", "$data/log:/var/log",
                            "-b", "${workspaceDir.absolutePath}:/root/workspace",
                            "-w", "/root",
                            "/bin/bash"
                        )
                    } else {
                        listOf(
                            proot,
                            "-0",
                            "-r", rootfs,
                            "-b", "/dev",
                            "-b", "/proc",
                            "-b", "/sys",
                            "-b", "$data/mysql:/var/lib/mysql",
                            "-b", "$data/redis:/var/lib/redis",
                            "-b", "$data/mongodb:/var/lib/mongodb",
                            "-b", "$data/run:/var/run",
                            "-b", "$data/log:/var/log",
                            "-b", "${workspaceDir.absolutePath}:/root/workspace",
                            "-w", "/root",
                            "/bin/bash", "-c", command
                        )
                    }
                } else {
                    val shellBinary = if (File(linuxEnvManager.BINDIR, "bash").exists() && File(linuxEnvManager.BINDIR, "bash").canExecute()) {
                        File(linuxEnvManager.BINDIR, "bash").absolutePath
                    } else {
                        "/system/bin/sh"
                    }
                    if (command.isBlank()) {
                        listOf(shellBinary)
                    } else {
                        listOf(shellBinary, "-c", command)
                    }
                }

                val pb = ProcessBuilder(cmdList)
                pb.directory(context.filesDir)
                pb.environment().putAll(env)
                pb.redirectErrorStream(true)

                val process = pb.start()
                interactiveProcess = process
                interactiveWriter = process.outputStream.bufferedWriter(Charsets.UTF_8)

                val pid = try {
                    val field = process.javaClass.getDeclaredField("pid")
                    field.isAccessible = true
                    field.getLong(process)
                } catch (_: Throwable) {
                    -1L
                }

                val title = if (command.isBlank()) "bash" else command.split(" ").firstOrNull() ?: "shell"
                _uiState.update {
                    it.copy(
                        isInteractiveSessionActive = true,
                        activeSessionTitle = title,
                        activeSessionPid = pid
                    )
                }
                appendLog("SHELL", "⚡ Connected to Linux Console [PID $pid] ($title)")
                appendLog("OUT", "Linux developer sandbox ready. Type 'help', 'status', 'mariadb', 'redis-cli', 'mongosh' or any bash command.")
                appendLog("OUT", "ubuntu@localhost:~$ ")

                interactiveReaderJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val reader = process.inputStream.bufferedReader(Charsets.UTF_8)
                        var line: String? = reader.readLine()
                        while (line != null) {
                            appendLog("OUT", line)
                            line = reader.readLine()
                        }
                    } catch (e: Exception) {
                        if (interactiveProcess != null) {
                            appendLog("SHELL-ERR", "Stream error: ${e.message}", isError = true)
                        }
                    } finally {
                        val exitCode = try { process.waitFor() } catch (_: Throwable) { -1 }
                        _uiState.update {
                            it.copy(
                                isInteractiveSessionActive = false,
                                activeSessionTitle = "none",
                                activeSessionPid = -1L
                            )
                        }
                        appendLog("SHELL", "Interactive session ended [exit $exitCode]")
                    }
                }
            } catch (e: Exception) {
                appendLog("SHELL-ERR", "Failed to start interactive shell: ${e.message}", isError = true)
                _uiState.update {
                    it.copy(
                        isInteractiveSessionActive = false,
                        activeSessionTitle = "none",
                        activeSessionPid = -1L
                    )
                }
            }
        }
    }

    fun sendInteractiveInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        appendLog("IN", "$ $trimmed")
        
        viewModelScope.launch(Dispatchers.IO) {
            when {
                trimmed.equals("help", ignoreCase = true) -> {
                    appendLog("OUT", "Linuxxx Universal Developer Console Commands:")
                    appendLog("OUT", "  status                 - Show live database daemon port health")
                    appendLog("OUT", "  redis-cli [cmd]        - Execute Redis command (e.g. redis-cli PING, redis-cli KEYS *)")
                    appendLog("OUT", "  mariadb / mysql [sql]  - Execute SQL query (e.g. mariadb -e 'SHOW TABLES;')")
                    appendLog("OUT", "  mongosh [query]        - Execute Mongo query (e.g. mongosh find audit_logs)")
                    appendLog("OUT", "  seed                   - Seed full-stack sample data across all 3 databases")
                    appendLog("OUT", "  clear                  - Clear console output")
                    appendLog("OUT", "  <bash command>         - Execute Linux command (ls, ps, pwd, cat, env, etc.)")
                    appendLog("OUT", "ubuntu@localhost:~$ ")
                }
                trimmed.equals("status", ignoreCase = true) -> {
                    val mOpen = probeSocket(3306).first
                    val rOpen = probeSocket(6379).first
                    val mgOpen = probeSocket(27017).first
                    appendLog("OUT", "DATABASE STACK STATUS:")
                    appendLog("OUT", "  [3306]  MariaDB 11.4: " + (if (mOpen) "ONLINE (listening)" else "OFFLINE"))
                    appendLog("OUT", "  [6379]  Redis 7.2   : " + (if (rOpen) "ONLINE (listening)" else "OFFLINE"))
                    appendLog("OUT", "  [27017] MongoDB 7.0 : " + (if (mgOpen) "ONLINE (listening)" else "OFFLINE"))
                    appendLog("OUT", "ubuntu@localhost:~$ ")
                }
                trimmed.equals("clear", ignoreCase = true) -> {
                    clearLogs()
                    appendLog("OUT", "ubuntu@localhost:~$ ")
                }
                trimmed.equals("seed", ignoreCase = true) -> {
                    appendLog("OUT", "Seeding full-stack demo data...")
                    val result = studioManager.seedFullStackDemo()
                    if (result.success) {
                        appendLog("OUT", "✔ Seeded successfully: ${result.mariaDbUsers} users, ${result.mariaDbProducts} products, ${result.redisKeys} Redis keys, ${result.mongoDocuments} Mongo docs (${result.durationMs}ms)")
                    } else {
                        appendLog("OUT", "✖ Seed status: ${result.message}")
                    }
                    appendLog("OUT", "ubuntu@localhost:~$ ")
                }
                trimmed.startsWith("redis-cli", ignoreCase = true) -> {
                    val redisCmd = trimmed.removePrefix("redis-cli").trim().ifEmpty { "PING" }
                    val res = studioManager.executeRedisCommand(redisCmd)
                    if (res.error != null) {
                        appendLog("SHELL-ERR", "(error) ${res.error}")
                    } else {
                        appendLog("OUT", res.output)
                    }
                    appendLog("OUT", "ubuntu@localhost:~$ ")
                }
                trimmed.startsWith("mariadb", ignoreCase = true) || trimmed.startsWith("mysql", ignoreCase = true) -> {
                    val sqlCmd = trimmed
                        .replace("mariadb -e", "", ignoreCase = true)
                        .replace("mysql -e", "", ignoreCase = true)
                        .replace("mariadb", "", ignoreCase = true)
                        .replace("mysql", "", ignoreCase = true)
                        .trim().trim('"', '\'').ifEmpty { "SHOW TABLES;" }
                    val res = studioManager.executeSqlQuery(sqlCmd)
                    if (res.error != null) {
                        appendLog("SHELL-ERR", "ERROR: ${res.error}")
                    } else {
                        if (res.columns.isNotEmpty()) {
                            appendLog("OUT", res.columns.joinToString(" | "))
                            appendLog("OUT", "-".repeat(40))
                            res.rows.take(20).forEach { row ->
                                appendLog("OUT", res.columns.map { c -> row[c]?.toString() ?: "NULL" }.joinToString(" | "))
                            }
                            appendLog("OUT", "(${res.rowCount} rows in set, ${res.durationMs} ms)")
                        } else {
                            appendLog("OUT", "Query OK, ${res.affectedRows} row(s) affected (${res.durationMs} ms)")
                        }
                    }
                    appendLog("OUT", "ubuntu@localhost:~$ ")
                }
                trimmed.startsWith("mongosh", ignoreCase = true) || trimmed.startsWith("mongo", ignoreCase = true) -> {
                    val mongoQuery = trimmed
                        .replace("mongosh --eval", "", ignoreCase = true)
                        .replace("mongosh", "", ignoreCase = true)
                        .replace("mongo", "", ignoreCase = true)
                        .trim().trim('"', '\'').ifEmpty { "db.audit_logs.find()" }
                    val res = studioManager.executeMongoQuery(queryStr = mongoQuery)
                    if (res.error != null) {
                        appendLog("SHELL-ERR", "MongoServerError: ${res.error}")
                    } else {
                        appendLog("OUT", res.outputJson)
                        appendLog("OUT", "(${res.documentCount} documents matched in ${res.durationMs} ms)")
                    }
                    appendLog("OUT", "ubuntu@localhost:~$ ")
                }
                else -> {
                    val writer = interactiveWriter
                    val proc = interactiveProcess
                    if (proc != null && proc.isAlive && writer != null) {
                        try {
                            writer.write(trimmed + "\n")
                            writer.flush()
                        } catch (e: Exception) {
                            appendLog("SHELL-ERR", "Failed writing to process stdin: ${e.message}", isError = true)
                        }
                    } else {
                        executeCommand(trimmed)
                    }
                }
            }
        }
    }

    fun sendControlSignal(signal: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val writer = interactiveWriter
            val proc = interactiveProcess
            if (proc != null && proc.isAlive && writer != null) {
                try {
                    when (signal.uppercase()) {
                        "CTRL_C", "SIGINT" -> {
                            appendLog("SHELL", "^C (SIGINT)")
                            writer.write("\u0003")
                            writer.flush()
                        }
                        "CTRL_D", "EOF" -> {
                            appendLog("SHELL", "^D (EOF)")
                            writer.write("\u0004")
                            writer.flush()
                        }
                        "TAB" -> {
                            writer.write("\t")
                            writer.flush()
                        }
                    }
                } catch (e: Exception) {
                    appendLog("SHELL-ERR", "Failed sending signal $signal: ${e.message}", isError = true)
                }
            } else {
                appendLog("SHELL", "No active interactive process to receive $signal")
            }
        }
    }

    fun killInteractiveSession() {
        try {
            interactiveReaderJob?.cancel()
            interactiveReaderJob = null
            interactiveProcess?.destroyForcibly()
            interactiveProcess = null
            interactiveWriter?.close()
            interactiveWriter = null
            _uiState.update {
                it.copy(
                    isInteractiveSessionActive = false,
                    activeSessionTitle = "none",
                    activeSessionPid = -1L
                )
            }
            appendLog("SHELL", "Interactive shell session terminated.")
        } catch (_: Exception) {}
    }

    // ------------------------------------------------------------------------
    // UNIVERSAL DEVELOPER STUDIO METHODS
    // ------------------------------------------------------------------------

    fun setStudioTab(tab: StudioTab) {
        _uiState.update { it.copy(studioTab = tab) }
        if (tab == StudioTab.BROWSER) {
            refreshStudioBrowserData()
        }
    }

    fun setStudioEngine(engine: String) {
        _uiState.update { it.copy(studioEngine = engine) }
    }

    fun setStudioSqlQuery(query: String) {
        _uiState.update { it.copy(studioSqlQueryInput = query) }
    }

    fun setStudioRedisCommand(cmd: String) {
        _uiState.update { it.copy(studioRedisCommandInput = cmd) }
    }

    fun setStudioMongoQuery(query: String) {
        _uiState.update { it.copy(studioMongoQueryInput = query) }
    }

    fun setStudioLanguage(lang: String) {
        val snippet = studioManager.generatePolyglotSnippet(lang)
        _uiState.update { it.copy(studioSelectedLanguage = lang, studioGeneratedSnippet = snippet) }
    }

    fun executeStudioQuery() {
        val engine = _uiState.value.studioEngine
        _uiState.update { it.copy(studioIsLoading = true, studioStatusMessage = "Executing query on $engine...") }
        viewModelScope.launch {
            when (engine) {
                "mariadb" -> {
                    val res = studioManager.executeSqlQuery(_uiState.value.studioSqlQueryInput)
                    _uiState.update {
                        it.copy(
                            studioSqlResult = res,
                            studioIsLoading = false,
                            studioStatusMessage = if (res.error != null) "Error: ${res.error}" else "OK (${res.durationMs}ms)"
                        )
                    }
                }
                "redis" -> {
                    val res = studioManager.executeRedisCommand(_uiState.value.studioRedisCommandInput)
                    _uiState.update {
                        it.copy(
                            studioRedisResult = res,
                            studioIsLoading = false,
                            studioStatusMessage = if (res.error != null) "Error: ${res.error}" else "OK (${res.durationMs}ms)"
                        )
                    }
                }
                "mongodb" -> {
                    val res = studioManager.executeMongoQuery("app_dev", _uiState.value.studioMongoQueryInput)
                    _uiState.update {
                        it.copy(
                            studioMongoResult = res,
                            studioIsLoading = false,
                            studioStatusMessage = if (res.error != null) "Error: ${res.error}" else "OK (${res.durationMs}ms)"
                        )
                    }
                }
            }
        }
    }

    fun refreshStudioBrowserData() {
        viewModelScope.launch {
            val keys = studioManager.getRedisKeys("*")
            val tables = studioManager.getMariaDbTables("app_dev")
            val colls = studioManager.getMongoCollections("app_dev")
            _uiState.update {
                it.copy(
                    studioRedisKeys = keys,
                    studioSqlTables = tables,
                    studioMongoCollections = colls
                )
            }
        }
    }

    fun seedStudioData() {
        _uiState.update { it.copy(studioIsLoading = true, studioStatusMessage = "Seeding full-stack developer test data...") }
        viewModelScope.launch {
            val result = studioManager.seedFullStackDemo()
            refreshStudioBrowserData()
            testStudioHealth()
            _uiState.update {
                it.copy(
                    studioSeedResult = result,
                    studioIsLoading = false,
                    studioStatusMessage = result.message
                )
            }
        }
    }

    fun purgeStudioData() {
        _uiState.update { it.copy(studioIsLoading = true, studioStatusMessage = "Purging developer test data...") }
        viewModelScope.launch {
            val res = studioManager.purgeAllDemoData()
            refreshStudioBrowserData()
            testStudioHealth()
            _uiState.update {
                it.copy(
                    studioIsLoading = false,
                    studioStatusMessage = res.getOrDefault("Data purged successfully.")
                )
            }
        }
    }

    fun testStudioHealth() {
        viewModelScope.launch {
            val map = studioManager.checkAllHealth()
            _uiState.update { it.copy(studioHealthMap = map) }
        }
    }

    fun runStudioBenchmark() {
        _uiState.update { it.copy(studioIsLoading = true, studioStatusMessage = "Running performance benchmark...") }
        viewModelScope.launch {
            val bench = studioManager.runBenchmark()
            _uiState.update {
                it.copy(
                    studioBenchmarkResult = bench,
                    studioIsLoading = false,
                    studioStatusMessage = "Benchmark completed!"
                )
            }
        }
    }

    fun selectStudioRedisKey(key: String) {
        setStudioEngine("redis")
        setStudioRedisCommand("GET $key")
        setStudioTab(StudioTab.QUERY)
        executeStudioQuery()
    }

    fun deleteStudioRedisKey(key: String) {
        viewModelScope.launch {
            studioManager.executeRedisCommand("DEL $key")
            refreshStudioBrowserData()
        }
    }

    fun selectStudioSqlTable(table: String) {
        setStudioEngine("mariadb")
        setStudioSqlQuery("SELECT * FROM `$table` LIMIT 20;")
        setStudioTab(StudioTab.QUERY)
        executeStudioQuery()
    }

    fun selectStudioMongoCollection(coll: String) {
        setStudioEngine("mongodb")
        setStudioMongoQuery("find $coll")
        setStudioTab(StudioTab.QUERY)
        executeStudioQuery()
    }

    private fun appendLog(tag: String, message: String, isError: Boolean = false) {
        val item = TerminalLogItem(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            isError = isError
        )
        val limit = _uiState.value.maxLogBufferSize
        _uiState.update { it.copy(logs = (it.logs + item).takeLast(limit)) }
        viewModelScope.launch(Dispatchers.IO) {
            repository.log(tag = tag, message = message, level = if (isError) "ERROR" else "INFO")
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = application as LinuxStackApp
                    val container = app.appContainer
                    return MainViewModel(
                        application = application,
                        repository = container.databaseRepository,
                        linuxEnvManager = container.linuxEnvManager,
                        bootstrapExtractor = container.bootstrapExtractor,
                        ubuntuRootfsManager = container.ubuntuRootfsManager
                    ) as T
                }
            }
    }
}
