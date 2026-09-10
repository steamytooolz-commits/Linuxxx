package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bridge.FileBridge
import com.example.core.UbuntuRootfsManager
import com.example.ui.MainUiState

/**
 * CodeMirror 6 Code Editor embedded inside an Android WebView with JavaScript bridge.
 * Directly reads/writes files in the proot rootfs workspace and executes SQL/Redis/Mongo queries.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodeMirrorEditorScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rootfsManager = remember { UbuntuRootfsManager(context) }
    val workspaceDir = rootfsManager.workspaceDir

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    // Expose AndroidFileBridge to JS as required in Section 9.2
                    addJavascriptInterface(FileBridge(workspaceDir), "AndroidFileBridge")

                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()

                    loadUrl("file:///android_asset/editor/index.html")
                }
            }
        )
    }
}
