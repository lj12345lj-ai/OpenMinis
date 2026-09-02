package com.openminis.app.ui.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * 内联文件浏览器 - 支持导航、查看、编辑、删除
 */
@Composable
fun InlineFileBrowser(
    startPath: String = "/var/minis/workspace",
    onFileSelect: ((File) -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf(startPath) }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // 文件编辑状态
    var editingFile by remember { mutableStateOf<File?>(null) }
    var editContent by remember { mutableStateOf("") }
    
    // 删除确认
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    
    // 加载目录
    androidx.compose.runtime.LaunchedEffect(currentPath) {
        isLoading = true
        files = loadDirectory(context, currentPath)
        isLoading = false
    }
    
    Column(modifier = modifier) {
        // 路径栏
        PathBar(
            path = currentPath,
            onHome = { currentPath = startPath },
            onRefresh = { 
                isLoading = true
                files = loadDirectory(context, currentPath)
                isLoading = false
            }
        )
        
        Divider()
        
        // 文件列表
        if (isLoading) {
            LoadingIndicator()
        } else if (files.isEmpty()) {
            EmptyState()
        } else {
            FileList(
                files = files,
                onFolderClick = { currentPath = it },
                onFileClick = { file ->
                    if (file.isTextFile) {
                        editingFile = file
                        editContent = runCatching { file.readText() }.getOrDefault("")
                    } else if (onFileSelect != null) {
                        onFileSelect(file)
                    }
                },
                onDelete = { deleteTarget = it }
            )
        }
    }
    
    // 编辑器对话框
    editingFile?.let { file ->
        FileEditorDialog(
            file = file,
            content = editContent,
            onSave = { newContent ->
                runCatching { file.writeText(newContent) }
                editingFile = null
                files = loadDirectory(context, currentPath)
            },
            onDismiss = { editingFile = null }
        )
    }
    
    // 删除确认对话框
    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除 ${file.name} 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { file.delete() }
                    deleteTarget = null
                    files = loadDirectory(context, currentPath)
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            }
        )
    }
}

// ========== UI 组件 ==========

@Composable
private fun PathBar(
    path: String,
    onHome: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onHome) {
            Icon(Icons.Default.Home, contentDescription = "首页", modifier = Modifier.size(20.dp))
        }
        
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "刷新", modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = path,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FileList(
    files: List<FileItem>,
    onFolderClick: (String) -> Unit,
    onFileClick: (File) -> Unit,
    onDelete: (File) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(files, key = { it.file.absolutePath }) { item ->
            FileRow(
                item = item,
                onFolderClick = onFolderClick,
                onFileClick = onFileClick,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun FileRow(
    item: FileItem,
    onFolderClick: (String) -> Unit,
    onFileClick: (File) -> Unit,
    onDelete: (File) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (item.file.isDirectory) onFolderClick(item.file.absolutePath)
                else onFileClick(item.file)
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Icon(
            imageVector = if (item.file.isDirectory) Icons.Default.Folder else getFileIcon(item.file),
            contentDescription = null,
            tint = if (item.file.isDirectory) Color(0xFFFFB86C) else Color(0xFF8BE9FD),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 文件名和元数据
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.file.name,
                fontSize = 12.sp,
                fontWeight = if (item.file.isDirectory) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.file.isFile) {
                Text(
                    text = "${formatFileSize(item.file.length())} · ${formatDate(item.file.lastModified())}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 编辑/删除按钮（仅文件显示）
        if (!item.file.isDirectory) {
            IconButton(
                onClick = { onFileClick(item.file) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(14.dp))
            }
            
            IconButton(
                onClick = { onDelete(item.file) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FileEditorDialog(
    file: File,
    content: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editContent by remember { mutableStateOf(content) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑: ${file.name}") },
        text = {
            Column {
                Text(
                    text = file.absolutePath,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                BasicTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        confirmButton = {
            Row {
                IconButton(onClick = { onSave(editContent); onDismiss() }) {
                    Icon(Icons.Default.Save, contentDescription = "保存")
                }
                TextButton(onClick = { onSave(editContent); onDismiss() }) {
                    Text("保存并关闭")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "目录为空",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ========== 辅助函数 ==========

data class FileItem(val file: File)

fun loadDirectory(context: Context, path: String): List<FileItem> {
    return try {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        
        dir.listFiles()?.filter { it.isVisible }
            ?.map { FileItem(it) }
            ?.sortedBy { item ->
                if (item.file.isDirectory) 0 else 1
            } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

fun getFileIcon(file: File): ImageVector {
    return when {
        file.extension.lowercase() in setOf("md", "txt", "log") -> Icons.Default.TextFields
        file.extension.lowercase() in setOf("json", "xml", "yaml", "yml") -> Icons.Default.TextFields
        file.extension.lowercase() in setOf("py", "js", "kt", "java", "c", "cpp") -> Icons.Default.Code
        file.extension.lowercase() in setOf("png", "jpg", "jpeg", "gif") -> Icons.Default.Image
        file.extension.lowercase() in setOf("mp4", "mov", "avi") -> Icons.Default.VideoFile
        file.extension.lowercase() in setOf("mp3", "wav") -> Icons.Default.AudioFile
        else -> Icons.Default.TextFields
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}

val File.isVisible: Boolean
    get() = !name.startsWith(".")

val File.isTextFile: Boolean
    get() {
        val ext = extension.lowercase()
        return ext in setOf(
            "txt", "md", "json", "xml", "yaml", "yml", "conf", "cfg", "ini",
            "log", "csv", "sh", "bash", "py", "js", "ts", "kt", "java", "c", "cpp",
            "html", "css", "scss", "toml", "env", "gitignore", "dockerfile", "makefile"
        ) || ext.isEmpty()
    }
