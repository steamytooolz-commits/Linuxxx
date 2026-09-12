package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.StudioTab
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppDrawerContent
import com.example.ui.components.AppTopBar
import com.example.ui.screens.CodeMirrorEditorScreen
import com.example.ui.screens.DaemonsScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.screens.UniversalStudioScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = uiState.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (uiState.isDarkMode) Color(0xFF030712) else Color(0xFFF8FAFC)
                ) {
                    val context = LocalContext.current
                    val notificationLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (!Environment.isExternalStorageManager()) {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                    intent.data = Uri.parse("package:${context.packageName}")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                    LinuxDashboardApp(
                        uiState = uiState,
                        onSelectScreen = { screen -> viewModel.setScreen(screen) },
                        onToggleService = { viewModel.toggleService() },
                        onExtractBootstrap = { force -> viewModel.extractBootstrap(force) },
                        onProbePorts = { viewModel.probePortsNow() },
                        onClearLogs = { viewModel.clearLogs() },
                        onToggleAutoScroll = { enabled -> viewModel.setAutoScroll(enabled) },
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onWiredTigerChange = { viewModel.setWiredTigerCacheSizeMb(it) },
                        onInnodbChange = { viewModel.setInnodbBufferPoolMb(it) },
                        onProbeIntervalChange = { viewModel.setProbeIntervalSec(it) },
                        onMaxLogBufferChange = { viewModel.setMaxLogBufferSize(it) },
                        onExecuteCommand = { viewModel.executeCommand(it) },
                        onStartInteractiveShell = { viewModel.startInteractiveShell(it) },
                        onSendInteractiveInput = { viewModel.sendInteractiveInput(it) },
                        onSendControlSignal = { viewModel.sendControlSignal(it) },
                        onKillInteractiveSession = { viewModel.killInteractiveSession() },
                        onCodeChange = { viewModel.setPlaygroundCode(it) },
                        onLanguageChange = { viewModel.setPlaygroundLanguage(it) },
                        onRunScript = { code, lang -> viewModel.runPlaygroundScript(code, lang) },
                        onStopScript = { viewModel.stopPlaygroundScript() },
                        onUrlChange = { viewModel.setPreviewUrl(it) },
                        onInstallPackage = { viewModel.installPackage(it) },
                        onStudioTabSelected = { viewModel.setStudioTab(it) },
                        onStudioEngineSelected = { viewModel.setStudioEngine(it) },
                        onStudioSqlQueryChange = { viewModel.setStudioSqlQuery(it) },
                        onStudioRedisCommandChange = { viewModel.setStudioRedisCommand(it) },
                        onStudioMongoQueryChange = { viewModel.setStudioMongoQuery(it) },
                        onStudioExecuteQuery = { viewModel.executeStudioQuery() },
                        onStudioLanguageSelected = { viewModel.setStudioLanguage(it) },
                        onStudioSeedDemoData = { viewModel.seedStudioData() },
                        onStudioPurgeDemoData = { viewModel.purgeStudioData() },
                        onStudioTestHealth = { viewModel.testStudioHealth() },
                        onStudioRunBenchmark = { viewModel.runStudioBenchmark() },
                        onStudioRefreshBrowser = { viewModel.refreshStudioBrowserData() },
                        onStudioSelectRedisKey = { viewModel.selectStudioRedisKey(it) },
                        onStudioDeleteRedisKey = { viewModel.deleteStudioRedisKey(it) },
                        onStudioSelectSqlTable = { viewModel.selectStudioSqlTable(it) },
                        onStudioSelectMongoCollection = { viewModel.selectStudioMongoCollection(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun LinuxDashboardApp(
    uiState: MainUiState,
    onSelectScreen: (AppScreen) -> Unit,
    onToggleService: () -> Unit,
    onExtractBootstrap: (force: Boolean) -> Unit,
    onProbePorts: () -> Unit,
    onClearLogs: () -> Unit,
    onToggleAutoScroll: (Boolean) -> Unit,
    onToggleDarkMode: () -> Unit,
    onWiredTigerChange: (Int) -> Unit = {},
    onInnodbChange: (Int) -> Unit = {},
    onProbeIntervalChange: (Int) -> Unit = {},
    onMaxLogBufferChange: (Int) -> Unit = {},
    onExecuteCommand: (String) -> Unit = {},
    onStartInteractiveShell: (String) -> Unit = {},
    onSendInteractiveInput: (String) -> Unit = {},
    onSendControlSignal: (String) -> Unit = {},
    onKillInteractiveSession: () -> Unit = {},
    onCodeChange: (String) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onRunScript: (code: String, lang: String) -> Unit = { _, _ -> },
    onStopScript: () -> Unit = {},
    onUrlChange: (String) -> Unit = {},
    onInstallPackage: (String) -> Unit = {},
    onStudioTabSelected: (StudioTab) -> Unit = {},
    onStudioEngineSelected: (String) -> Unit = {},
    onStudioSqlQueryChange: (String) -> Unit = {},
    onStudioRedisCommandChange: (String) -> Unit = {},
    onStudioMongoQueryChange: (String) -> Unit = {},
    onStudioExecuteQuery: () -> Unit = {},
    onStudioLanguageSelected: (String) -> Unit = {},
    onStudioSeedDemoData: () -> Unit = {},
    onStudioPurgeDemoData: () -> Unit = {},
    onStudioTestHealth: () -> Unit = {},
    onStudioRunBenchmark: () -> Unit = {},
    onStudioRefreshBrowser: () -> Unit = {},
    onStudioSelectRedisKey: (String) -> Unit = {},
    onStudioDeleteRedisKey: (String) -> Unit = {},
    onStudioSelectSqlTable: (String) -> Unit = {},
    onStudioSelectMongoCollection: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val isDark = uiState.isDarkMode

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = if (isDark) Color(0xFF090D16) else Color(0xFFF8FAFC)
            ) {
                AppDrawerContent(
                    uiState = uiState,
                    onSelectScreen = onSelectScreen,
                    onToggleService = onToggleService,
                    onToggleDarkMode = onToggleDarkMode,
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isTabletOrLandscape = maxWidth >= 840.dp

            if (isTabletOrLandscape) {
                // Adaptive layout for tablets / large screens
                Row(modifier = Modifier.fillMaxSize()) {
                    AppDrawerContent(
                        uiState = uiState,
                        onSelectScreen = onSelectScreen,
                        onToggleService = onToggleService,
                        onToggleDarkMode = onToggleDarkMode,
                        onCloseDrawer = {},
                        modifier = Modifier.fillMaxHeight()
                    )

                    Scaffold(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        containerColor = if (isDark) Color(0xFF030712) else Color(0xFFF8FAFC),
                        topBar = {
                            AppTopBar(
                                uiState = uiState,
                                onOpenDrawer = {
                                    coroutineScope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                onToggleService = onToggleService,
                                onProbePorts = onProbePorts,
                                onToggleDarkMode = onToggleDarkMode
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(12.dp)
                        ) {
                            ScreenRouter(
                                uiState = uiState,
                                onSelectScreen = onSelectScreen,
                                onToggleService = onToggleService,
                                onExtractBootstrap = onExtractBootstrap,
                                onProbePorts = onProbePorts,
                                onClearLogs = onClearLogs,
                                onToggleAutoScroll = onToggleAutoScroll,
                                onWiredTigerChange = onWiredTigerChange,
                                onInnodbChange = onInnodbChange,
                                onProbeIntervalChange = onProbeIntervalChange,
                                onMaxLogBufferChange = onMaxLogBufferChange,
                                onExecuteCommand = onExecuteCommand,
                                onStartInteractiveShell = onStartInteractiveShell,
                                onSendInteractiveInput = onSendInteractiveInput,
                                onSendControlSignal = onSendControlSignal,
                                onKillInteractiveSession = onKillInteractiveSession,
                                onCodeChange = onCodeChange,
                                onLanguageChange = onLanguageChange,
                                onRunScript = onRunScript,
                                onStopScript = onStopScript,
                                onUrlChange = onUrlChange,
                                onInstallPackage = onInstallPackage,
                                onStudioTabSelected = onStudioTabSelected,
                                onStudioEngineSelected = onStudioEngineSelected,
                                onStudioSqlQueryChange = onStudioSqlQueryChange,
                                onStudioRedisCommandChange = onStudioRedisCommandChange,
                                onStudioMongoQueryChange = onStudioMongoQueryChange,
                                onStudioExecuteQuery = onStudioExecuteQuery,
                                onStudioLanguageSelected = onStudioLanguageSelected,
                                onStudioSeedDemoData = onStudioSeedDemoData,
                                onStudioPurgeDemoData = onStudioPurgeDemoData,
                                onStudioTestHealth = onStudioTestHealth,
                                onStudioRunBenchmark = onStudioRunBenchmark,
                                onStudioRefreshBrowser = onStudioRefreshBrowser,
                                onStudioSelectRedisKey = onStudioSelectRedisKey,
                                onStudioDeleteRedisKey = onStudioDeleteRedisKey,
                                onStudioSelectSqlTable = onStudioSelectSqlTable,
                                onStudioSelectMongoCollection = onStudioSelectMongoCollection
                            )
                        }
                    }
                }
            } else {
                // Mobile layout with full-height viewports and bottom quick navigation
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (isDark) Color(0xFF030712) else Color(0xFFF8FAFC),
                    topBar = {
                        AppTopBar(
                            uiState = uiState,
                            onOpenDrawer = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onToggleService = onToggleService,
                            onProbePorts = onProbePorts,
                            onToggleDarkMode = onToggleDarkMode
                        )
                    },
                    bottomBar = {
                        AppBottomBar(
                            currentScreen = uiState.currentScreen,
                            isDarkMode = isDark,
                            onSelectScreen = onSelectScreen
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        ScreenRouter(
                            uiState = uiState,
                            onSelectScreen = onSelectScreen,
                            onToggleService = onToggleService,
                            onExtractBootstrap = onExtractBootstrap,
                            onProbePorts = onProbePorts,
                            onClearLogs = onClearLogs,
                            onToggleAutoScroll = onToggleAutoScroll,
                            onWiredTigerChange = onWiredTigerChange,
                            onInnodbChange = onInnodbChange,
                            onProbeIntervalChange = onProbeIntervalChange,
                            onMaxLogBufferChange = onMaxLogBufferChange,
                            onExecuteCommand = onExecuteCommand,
                            onStartInteractiveShell = onStartInteractiveShell,
                            onSendInteractiveInput = onSendInteractiveInput,
                            onSendControlSignal = onSendControlSignal,
                            onKillInteractiveSession = onKillInteractiveSession,
                            onCodeChange = onCodeChange,
                            onLanguageChange = onLanguageChange,
                            onRunScript = onRunScript,
                            onStopScript = onStopScript,
                            onUrlChange = onUrlChange,
                            onInstallPackage = onInstallPackage,
                            onStudioTabSelected = onStudioTabSelected,
                            onStudioEngineSelected = onStudioEngineSelected,
                            onStudioSqlQueryChange = onStudioSqlQueryChange,
                            onStudioRedisCommandChange = onStudioRedisCommandChange,
                            onStudioMongoQueryChange = onStudioMongoQueryChange,
                            onStudioExecuteQuery = onStudioExecuteQuery,
                            onStudioLanguageSelected = onStudioLanguageSelected,
                            onStudioSeedDemoData = onStudioSeedDemoData,
                            onStudioPurgeDemoData = onStudioPurgeDemoData,
                            onStudioTestHealth = onStudioTestHealth,
                            onStudioRunBenchmark = onStudioRunBenchmark,
                            onStudioRefreshBrowser = onStudioRefreshBrowser,
                            onStudioSelectRedisKey = onStudioSelectRedisKey,
                            onStudioDeleteRedisKey = onStudioDeleteRedisKey,
                            onStudioSelectSqlTable = onStudioSelectSqlTable,
                            onStudioSelectMongoCollection = onStudioSelectMongoCollection
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenRouter(
    uiState: MainUiState,
    onSelectScreen: (AppScreen) -> Unit,
    onToggleService: () -> Unit,
    onExtractBootstrap: (force: Boolean) -> Unit,
    onProbePorts: () -> Unit,
    onClearLogs: () -> Unit,
    onToggleAutoScroll: (Boolean) -> Unit,
    onWiredTigerChange: (Int) -> Unit,
    onInnodbChange: (Int) -> Unit,
    onProbeIntervalChange: (Int) -> Unit,
    onMaxLogBufferChange: (Int) -> Unit,
    onExecuteCommand: (String) -> Unit,
    onStartInteractiveShell: (String) -> Unit = {},
    onSendInteractiveInput: (String) -> Unit = {},
    onSendControlSignal: (String) -> Unit = {},
    onKillInteractiveSession: () -> Unit = {},
    onCodeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRunScript: (code: String, lang: String) -> Unit,
    onStopScript: () -> Unit,
    onUrlChange: (String) -> Unit,
    onInstallPackage: (String) -> Unit,
    onStudioTabSelected: (StudioTab) -> Unit = {},
    onStudioEngineSelected: (String) -> Unit = {},
    onStudioSqlQueryChange: (String) -> Unit = {},
    onStudioRedisCommandChange: (String) -> Unit = {},
    onStudioMongoQueryChange: (String) -> Unit = {},
    onStudioExecuteQuery: () -> Unit = {},
    onStudioLanguageSelected: (String) -> Unit = {},
    onStudioSeedDemoData: () -> Unit = {},
    onStudioPurgeDemoData: () -> Unit = {},
    onStudioTestHealth: () -> Unit = {},
    onStudioRunBenchmark: () -> Unit = {},
    onStudioRefreshBrowser: () -> Unit = {},
    onStudioSelectRedisKey: (String) -> Unit = {},
    onStudioDeleteRedisKey: (String) -> Unit = {},
    onStudioSelectSqlTable: (String) -> Unit = {},
    onStudioSelectMongoCollection: (String) -> Unit = {}
) {
    AnimatedContent(
        targetState = uiState.currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.SETUP -> {
                SetupScreen(
                    uiState = uiState,
                    onSetupCompleted = { onSelectScreen(AppScreen.STUDIO) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.STUDIO -> {
                UniversalStudioScreen(
                    uiState = uiState,
                    onTabSelected = onStudioTabSelected,
                    onEngineSelected = onStudioEngineSelected,
                    onSqlQueryChange = onStudioSqlQueryChange,
                    onRedisCommandChange = onStudioRedisCommandChange,
                    onMongoQueryChange = onStudioMongoQueryChange,
                    onExecuteQuery = onStudioExecuteQuery,
                    onLanguageSelected = onStudioLanguageSelected,
                    onSeedDemoData = onStudioSeedDemoData,
                    onPurgeDemoData = onStudioPurgeDemoData,
                    onTestHealth = onStudioTestHealth,
                    onRunBenchmark = onStudioRunBenchmark,
                    onRefreshBrowser = onStudioRefreshBrowser,
                    onSelectRedisKey = onStudioSelectRedisKey,
                    onDeleteRedisKey = onStudioDeleteRedisKey,
                    onSelectSqlTable = onStudioSelectSqlTable,
                    onSelectMongoCollection = onStudioSelectMongoCollection,
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.CODEMIRROR -> {
                CodeMirrorEditorScreen(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppScreen.DAEMONS -> {
                DaemonsScreen(
                    uiState = uiState,
                    onToggleService = onToggleService,
                    onProbePorts = onProbePorts,
                    onExecuteCommand = onExecuteCommand
                )
            }
            AppScreen.TERMINAL -> {
                TerminalScreen(
                    uiState = uiState,
                    onStartInteractiveShell = onStartInteractiveShell,
                    onSendInteractiveInput = onSendInteractiveInput,
                    onSendControlSignal = onSendControlSignal,
                    onKillInteractiveSession = onKillInteractiveSession,
                    onClearLogs = onClearLogs,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
