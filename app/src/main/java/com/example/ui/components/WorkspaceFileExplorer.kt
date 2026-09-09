package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileEntry(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val isExecutable: Boolean,
    val lastModified: Long
)

/**
 * Acode-inspired Workspace File & Config Inspector & Live Editor.
 * Enables live browsing of unpacked Linux directories ($PREFIX, $HOME, etc.),
 * inspecting and modifying configuration files (my.cnf, redis.conf, mongod.conf),
 * and viewing/editing raw file contents with line counts and direct bash execution.
 */
@Composable
fun WorkspaceFileExplorer(
    rootDirPath: String,
    homeDirPath: String,
    onExecuteFile: (String) -> Unit = {},
    onFileSaved: (File) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentDirectory by remember(rootDirPath) {
        val f = File(rootDirPath)
        mutableStateOf(if (f.exists()) f else File("/data/local/tmp"))
    }

    var fileList by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedFileForPreview by remember { mutableStateOf<File?>(null) }
    var previewContent by remember { mutableStateOf<String?>(null) }
    var isEditingMode by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf("") }
    var saveStatusMsg by remember { mutableStateOf<String?>(null) }

    fun refreshDirectory(dir: File) {
        isLoading = true
        coroutineScope.launch(Dispatchers.IO) {
            val entries = if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.map {
                    FileEntry(
                        file = it,
                        name = it.name,
                        isDirectory = it.isDirectory,
                        sizeBytes = if (it.isFile) it.length() else 0L,
                        isExecutable = it.canExecute(),
                        lastModified = it.lastModified()
                    )
                }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
            } else {
                emptyList()
            }

            withContext(Dispatchers.Main) {
                fileList = entries
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentDirectory) {
        refreshDirectory(currentDirectory)
    }

    fun openFilePreview(file: File) {
        selectedFileForPreview = file
        isEditingMode = false
        saveStatusMsg = null
        coroutineScope.launch(Dispatchers.IO) {
            val content = try {
                if (file.length() > 250_000) {
                    file.bufferedReader().useLines { lines ->
                        lines.take(200).joinToString("\n") + "\n\n... [Truncated: File exceeds preview threshold] ..."
                    }
                } else {
                    file.readText()
                }
            } catch (e: Exception) {
                "Error loading file content: ${e.message}"
            }
            withContext(Dispatchers.Main) {
                previewContent = content
                editedContent = content
            }
        }
    }

    fun saveFileContent() {
        val target = selectedFileForPreview ?: return
        coroutineScope.launch(Dispatchers.IO) {
            val success = try {
                target.writeText(editedContent)
                true
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                if (success) {
                    previewContent = editedContent
                    isEditingMode = false
                    saveStatusMsg = "Saved successfully"
                    onFileSaved(target)
                } else {
                    saveStatusMsg = "Write failed: Permission denied or read-only"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF090D16))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        // Explorer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ACODE // WORKSPACE INSPECTOR",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { refreshDirectory(currentDirectory) },
                    modifier = Modifier.size(28.dp).testTag("refresh_explorer_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh Directory",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Quick Navigation Bookmarks Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1322))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val bookmarks = listOf(
                "ROOTFS" to rootDirPath,
                "HOME" to homeDirPath,
                "BIN" to "$rootDirPath/bin",
                "ETC" to "$rootDirPath/etc",
                "LIB" to "$rootDirPath/lib",
                "TMP" to "$rootDirPath/tmp"
            )

            bookmarks.forEach { (label, path) ->
                val isCurrent = currentDirectory.absolutePath == path
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCurrent) Color(0xFF0284C7) else Color(0xFF1E293B))
                        .clickable {
                            val target = File(path)
                            if (target.exists()) {
                                currentDirectory = target
                                selectedFileForPreview = null
                                isEditingMode = false
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isCurrent) Color.White else Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Path Breadcrumb & Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B101E))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentDirectory.parentFile != null && selectedFileForPreview == null) {
                IconButton(
                    onClick = {
                        currentDirectory.parentFile?.let { currentDirectory = it }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Up directory",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = if (selectedFileForPreview != null) {
                    "File: ${selectedFileForPreview?.name}"
                } else {
                    currentDirectory.absolutePath
                },
                color = Color(0xFFE2E8F0),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (selectedFileForPreview != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isEditingMode) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF10B981))
                                .clickable { saveFileContent() }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Save",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF475569))
                                .clickable { isEditingMode = false }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFFF1F5F9),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0284C7))
                                .clickable {
                                    editedContent = previewContent ?: ""
                                    isEditingMode = true
                                }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF334155))
                                .clickable {
                                    selectedFileForPreview = null
                                    previewContent = null
                                    isEditingMode = false
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Close",
                                color = Color(0xFFF1F5F9),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        if (saveStatusMsg != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = saveStatusMsg!!,
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

        // Main Content: File Editor, Preview or File List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 2.dp
                    )
                }
            } else if (selectedFileForPreview != null) {
                if (isEditingMode) {
                    // Interactive Raw Editor
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF070B14))
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editedContent,
                            onValueChange = { editedContent = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("file_editor_textfield"),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 15.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0284C7),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedContainerColor = Color(0xFF070B14),
                                unfocusedContainerColor = Color(0xFF070B14)
                            )
                        )
                    }
                } else {
                    // File Text Viewer with Line Numbers (Acode style)
                    FileContentReader(
                        file = selectedFileForPreview!!,
                        content = previewContent,
                        onExecute = { onExecuteFile(selectedFileForPreview!!.absolutePath) }
                    )
                }
            } else if (fileList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Directory is empty or inaccessible",
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(fileList, key = { it.file.absolutePath }) { entry ->
                        FileRowItem(
                            entry = entry,
                            onClick = {
                                if (entry.isDirectory) {
                                    currentDirectory = entry.file
                                } else {
                                    openFilePreview(entry.file)
                                }
                            }
                        )
                        HorizontalDivider(color = Color(0xFF131C2E), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRowItem(
    entry: FileEntry,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd HH:mm", Locale.US)
    val formattedDate = remember(entry.lastModified) { dateFormat.format(Date(entry.lastModified)) }

    val icon = when {
        entry.isDirectory -> Icons.Filled.Folder
        entry.isExecutable -> Icons.Filled.Code
        entry.name.endsWith(".cnf") || entry.name.endsWith(".conf") -> Icons.Outlined.Settings
        entry.name.endsWith(".log") -> Icons.Outlined.Terminal
        entry.name.endsWith(".sh") -> Icons.Outlined.Code
        else -> Icons.Filled.InsertDriveFile
    }

    val iconColor = when {
        entry.isDirectory -> Color(0xFF38BDF8)
        entry.isExecutable -> Color(0xFF4ADE80)
        entry.name.endsWith(".cnf") || entry.name.endsWith(".conf") -> Color(0xFFFBBF24)
        entry.name.endsWith(".log") -> Color(0xFFA78BFA)
        else -> Color(0xFF94A3B8)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.name,
                    color = if (entry.isDirectory) Color(0xFFF1F5F9) else Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (entry.isDirectory || entry.isExecutable) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.isExecutable && !entry.isDirectory) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x2622C55E))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "x",
                            color = Color(0xFF4ADE80),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Text(
                text = "$formattedDate • ${if (entry.isDirectory) "dir" else formatFileSize(entry.sizeBytes)}",
                color = Color(0xFF64748B),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun FileContentReader(
    file: File,
    content: String?,
    onExecute: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
    ) {
        // Quick Action Top Bar for File
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${file.name} (${formatFileSize(file.length())})",
                color = Color(0xFF38BDF8),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            if (file.canExecute()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF22C55E))
                        .clickable(onClick = onExecute)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Run in Terminal",
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (content == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF38BDF8),
                    strokeWidth = 2.dp
                )
            }
        } else {
            val lines = remember(content) { content.lines() }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(lines.size) { index ->
                    val line = lines[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}".padStart(4, ' '),
                            color = Color(0xFF475569),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )

                        Text(
                            text = line,
                            color = if (line.trimStart().startsWith("#")) Color(0xFF64748B) else Color(0xFFE2E8F0),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}
