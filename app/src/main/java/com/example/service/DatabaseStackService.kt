package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.LinuxStackApp
import com.example.MainActivity
import com.example.core.DatabaseSecurityManager
import com.example.core.UbuntuRootfsManager
import com.example.data.repository.DatabaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.min

/**
 * Android Foreground Service hosting the Linux database stack (MariaDB, Redis, MongoDB)
 * inside a rootless ARM64 PRoot container.
 * Supervised with an active watchdog coroutine providing automatic restarts and backoff.
 */
class DatabaseStackService : Service() {

    companion object {
        private const val TAG = "DatabaseStackService"
        private const val CHANNEL_ID = "linuxxx_db_channel"
        private const val NOTIFICATION_ID = 9001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _logStream = MutableSharedFlow<LogMessage>(extraBufferCapacity = 500)
        val logStream: SharedFlow<LogMessage> = _logStream.asSharedFlow()

        private val _portStatus = MutableStateFlow(mapOf(3306 to false, 6379 to false, 27017 to false))
        val portStatus: StateFlow<Map<Int, Boolean>> = _portStatus.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, DatabaseStackService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DatabaseStackService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    data class LogMessage(
        val timestamp: Long = System.currentTimeMillis(),
        val tag: String,
        val text: String,
        val isError: Boolean = false
    )

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var wakeLock: PowerManager.WakeLock? = null
    private var prootProcess: Process? = null
    private var prootSupervisorJob: Job? = null
    private var portMonitorJob: Job? = null

    private lateinit var dbRepository: DatabaseRepository
    private lateinit var rootfsManager: UbuntuRootfsManager
    private lateinit var dbSecurity: DatabaseSecurityManager

    override fun onCreate() {
        super.onCreate()
        val app = application as LinuxStackApp
        dbRepository = app.appContainer.databaseRepository
        rootfsManager = UbuntuRootfsManager(this)
        dbSecurity = DatabaseSecurityManager.getInstance(this)

        // Acquire PARTIAL_WAKE_LOCK to prevent CPU sleep during long-running background service
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Linuxxx:DatabaseStackWakeLock").apply {
                acquire(24 * 60 * 60 * 1000L)
            }
            Log.d(TAG, "Acquired PARTIAL_WAKE_LOCK")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}")
        }

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Linuxxx Database Appliance")
            .setContentText("MariaDB :3306 | Redis :6379 | MongoDB :27017")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isRunning.value = true

        serviceScope.launch {
            emitLog("SERVICE", "Starting Linuxxx Database Appliance foreground stack...", false)

            if (rootfsManager.isEnvironmentReady()) {
                startSupervisedProotWatchdog()
            } else {
                emitLog("PROOT", "Rootfs environment is not fully ready. Complete first-launch setup.", true)
            }

            startPortMonitoring()
        }

        return START_STICKY
    }

    /**
     * Starts the PRoot process watchdog. If the process dies unexpectedly,
     * logs the exit status and restarts with exponential backoff.
     */
    private fun startSupervisedProotWatchdog() {
        prootSupervisorJob?.cancel()
        prootSupervisorJob = serviceScope.launch {
            var restartDelay = 1000L
            val maxRestartDelay = 30000L

            while (isActive) {
                try {
                    rootfsManager.workspaceDir.mkdirs()
                    val cmd = rootfsManager.buildProotCommand()
                    val password = dbSecurity.getOrCreateMariaDbPassword()
                    emitLog("PROOT", "Launching proot supervisor: ${cmd.joinToString(" ")}", false)

                    val pb = ProcessBuilder(cmd)
                    pb.directory(filesDir)
                    pb.environment()["HOME"] = "/root"
                    pb.environment()["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                    pb.environment()["MARIADB_ROOT_PASSWORD"] = password
                    pb.redirectErrorStream(true)

                    val proc = pb.start()
                    prootProcess = proc

                    // Stream stdout / stderr output asynchronously
                    val readerJob = launch {
                        try {
                            val reader = BufferedReader(InputStreamReader(proc.inputStream))
                            var line: String? = reader.readLine()
                            while (line != null && isActive) {
                                val isErr = line.contains("ERROR", ignoreCase = true) || line.contains("FAIL", ignoreCase = true)
                                emitLog("CONTAINER", line, isError = isErr)
                                line = reader.readLine()
                            }
                        } catch (ioEx: Exception) {
                            Log.d(TAG, "Container stream closed: ${ioEx.message}")
                        }
                    }

                    // Watchdog: block until container process terminates
                    val exitCode = proc.waitFor()
                    readerJob.cancel()
                    prootProcess = null

                    Log.i(TAG, "PRoot process terminated with exit code $exitCode (service isActive=$isActive)")

                    if (!isActive) {
                        emitLog("PROOT", "Proot container stopped cleanly (service stopping, exit code $exitCode).", false)
                        break
                    }

                    if (exitCode == 0) {
                        emitLog("PROOT", "PRoot process exited cleanly with exit code 0. Standby mode active.", false)
                        break
                    }

                    emitLog("WATCHDOG", "PRoot process exited unexpectedly with code $exitCode. Restarting in ${restartDelay / 1000}s...", isError = true)
                    delay(restartDelay)
                    restartDelay = min(restartDelay * 2, maxRestartDelay)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to spawn proot container", e)
                    emitLog("PROOT", "Container launch failure: ${e.message}", true)
                    delay(restartDelay)
                    restartDelay = min(restartDelay * 2, maxRestartDelay)
                }
            }
        }
    }

    private fun startPortMonitoring() {
        portMonitorJob?.cancel()
        portMonitorJob = serviceScope.launch {
            while (isActive) {
                val updatedStatus = mutableMapOf<Int, Boolean>()
                for (port in listOf(3306, 6379, 27017)) {
                    val open = isPortListening("127.0.0.1", port)
                    updatedStatus[port] = open
                }
                _portStatus.value = updatedStatus
                delay(2500)
            }
        }
    }

    private fun isPortListening(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 400)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun emitLog(tag: String, text: String, isError: Boolean) {
        val msg = LogMessage(tag = tag, text = text, isError = isError)
        _logStream.tryEmit(msg)
        serviceScope.launch {
            try {
                dbRepository.log(tag, text, if (isError) "ERROR" else "INFO")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist log to repository: ${e.message}", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Linuxxx Database Appliance",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background execution for MariaDB, Redis, and MongoDB daemons"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        _isRunning.value = false
        portMonitorJob?.cancel()
        prootSupervisorJob?.cancel()

        try {
            prootProcess?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying proot process: ${e.message}")
        }

        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
                Log.d(TAG, "Released PARTIAL_WAKE_LOCK")
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing wake lock: ${e.message}")
            }
        }

        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
