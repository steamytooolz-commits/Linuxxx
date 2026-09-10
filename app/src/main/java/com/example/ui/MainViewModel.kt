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

enum class AppScreen(val title: String, val route: String) {
    SETUP("First-Launch Setup", "setup"),
    CODEMIRROR("CodeMirror 6 Editor", "codemirror"),
    DAEMONS("Database Daemons", "daemons"),
    TERMINAL("Interactive Terminal", "terminal")
}

data class MainUiState(
    val isServiceRunning: Boolean = false,
    val isExtracted: Boolean = false,
    val isDarkMode: Boolean = true,
    val currentScreen: AppScreen = AppScreen.SETUP,
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
    val activeSessionPid: Long = -1L
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

    private var portPollingJob: Job? = null
    private var interactiveProcess: Process? = null
    private var interactiveWriter: java.io.BufferedWriter? = null
    private var interactiveReaderJob: Job? = null

    init {
        // Collect service running state from Foreground Service
        viewModelScope.launch {
            DatabaseStackService.isRunning.collect { running ->
                _uiState.update { it.copy(isServiceRunning = running) }
                if (running) {
                    startPortPolling()
                } else {
                    stopPortPolling()
                    resetPortStatus()
                }
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
                val intervalMs = (_uiState.value.probeIntervalSec.coerceIn(3, 30)) * 1000L
                delay(intervalMs)
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
            } else {
                appendLog("PROBE", "No active TCP listeners detected in local namespace.")
            }

            if (!_uiState.value.isServiceRunning) {
                appendLog("PROBE", "\u001B[33mNote: Database stack foreground service is STOPPED. Tap 'START STACK' to launch daemons.\u001B[0m")
            } else {
                appendLog("PROBE", "\u001B[32mScan complete: $openCount online, $closedCount closed.\u001B[0m")
            }
        }
    }

    /**
     * Probing ports 3306, 6379, and 27017 using loopback socket connection.
     * Uses gentle timeouts (400ms) and avoids excessive rapid connection attempts.
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
        val start = System.currentTimeMillis()
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", port), 350)
            val latency = (System.currentTimeMillis() - start).coerceAtLeast(0L)
            Pair(true, latency)
        } catch (_: Throwable) {
            Pair(false, -1L)
        } finally {
            try {
                socket?.close()
            } catch (_: Throwable) {
                // Ignore close cleanup exceptions
            }
        }
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
                        listOf(shellBinary, "-i")
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
                appendLog("SHELL", "⚡ Started interactive Linux stream session [$title]")

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
        
        viewModelScope.launch(Dispatchers.IO) {
            val writer = interactiveWriter
            val proc = interactiveProcess
            if (proc != null && proc.isAlive && writer != null) {
                try {
                    appendLog("IN", "$ $trimmed")
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
