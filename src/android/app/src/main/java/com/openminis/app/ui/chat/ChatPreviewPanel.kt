package com.openminis.app.ui.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

/**
 * 三合一预览面板
 * - 工具预览：显示工具执行输出
 * - 文件浏览：内联文件管理器
 * - 网页浏览：内联 WebView
 */
@Composable
fun ChatPreviewPanel(
    initialMode: String = "tool",
    viewModel: Any? = null,
) {
    var activeMode by remember { mutableStateOf(initialMode) }
    val context = LocalContext.current
    
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .height(350.dp)
    ) {
        // Tab 切换栏
        PreviewModeTabs(
            activeMode = activeMode,
            onModeChange = { activeMode = it }
        )
        
        // 内容区
        when (activeMode) {
            "tool" -> ToolPreviewContent(viewModel)
            "file" -> InlineFileBrowser(
                startPath = "/var/minis/workspace",
                modifier = Modifier.fillMaxSize()
            )
            "web" -> InlineWebPreview(
                modifier = Modifier.fillMaxSize(),
                initialUrl = "https://openminis.app"
            )
        }
    }
}

@Composable
private fun PreviewModeTabs(
    activeMode: String,
    onModeChange: (String) -> Unit,
) {
    val modes = listOf(
        Triple("tool", "工具预览", com.compose.material.icons.Icons.Default.Devices),
        Triple("file", "文件浏览", com.compose.material.icons.Icons.Default.Folder),
        Triple("web", "网页浏览", com.compose.material.icons.Icons.Default.Language),
    )
    
    TabRow(
        selectedTabIndex = modes.indexOfFirst { it.first == activeMode }.coerceAtLeast(0),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
    ) {
        modes.forEach { (mode, label, icon) ->
            Tab(
                text = { Text(label, fontSize = 11.sp) },
                selected = mode == activeMode,
                onClick = { onModeChange(mode) }
            )
        }
    }
}

@Composable
private fun ToolPreviewContent(viewModel: Any?) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "工具预览内容",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
