package com.openminis.app.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 三合一预览面板
 * - 工具预览：显示当前工具调用的输出（命令/结果/截图）
 * - 文件浏览：内联文件管理器
 * - 网页浏览：内联 WebView
 */
@Composable
fun ChatPreviewPanel(
    initialMode: String = "tool",
    block: AssistantBlock? = null,
    onOpenDetail: () -> Unit = {},
) {
    var activeMode by remember { mutableStateOf(initialMode) }

    Column(
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
            "tool" -> ToolPreviewContent(
                block = block,
                onOpenDetail = onOpenDetail,
            )
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
        Triple("tool", "工具", Icons.Default.Devices),
        Triple("file", "文件", Icons.Default.Folder),
        Triple("web", "网页", Icons.Default.Language),
    )

    TabRow(
        selectedTabIndex = modes.indexOfFirst { it.first == activeMode }.coerceAtLeast(0),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        modes.forEach { (mode, label, icon) ->
            Tab(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(label, fontSize = 11.sp)
                    }
                },
                selected = mode == activeMode,
                onClick = { onModeChange(mode) }
            )
        }
    }
}

/** 工具 tab：渲染当前 AssistantBlock 的真实内容（标题/状态/输出/截图）。 */
@Composable
private fun ToolPreviewContent(
    block: AssistantBlock?,
    onOpenDetail: () -> Unit,
) {
    if (block == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "暂无工具调用",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        return
    }

    val args = remember(block.toolArgs) {
        try { JSONObject(block.toolArgs) } catch (_: Exception) { JSONObject() }
    }
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onOpenDetail)
    ) {
        // 标题行：图标 + 工具名 + 状态 + 时长
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                toolIconFor(block.toolName),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = block.toolTitle.ifEmpty { block.toolName.ifEmpty { "工具" } },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = previewStatusText(block.toolStatus),
                fontSize = 10.sp,
                color = previewStatusColor(block.toolStatus),
            )
            if (block.durationMs > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatToolDuration(block.durationMs),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // 内容区
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val imagePath = block.imageFilePath
            if (imagePath != null) {
                // 截图：离屏解码，避免阻塞主线程
                val bitmap by produceState<Bitmap?>(
                    initialValue = null,
                    key1 = imagePath,
                ) {
                    value = withContext(Dispatchers.IO) {
                        try { BitmapFactory.decodeFile(imagePath) } catch (_: Exception) { null }
                    }
                }
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "工具截图",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                    )
                } else {
                    PreviewFallback(block, args)
                }
            } else {
                PreviewFallback(block, args)
            }
        }
    }
}

/** 无截图时的文本渲染：命令（shell）+ 输出内容（mono 字体、可滚动）。 */
@Composable
private fun PreviewFallback(block: AssistantBlock, args: JSONObject) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (block.toolName == "shell_execute") {
            val command = extractShellCommand(args, block)
            if (command.isNotEmpty() && command != "Shell command") {
                Text(
                    text = "$ $command",
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        val output = block.content
        Text(
            text = output.ifEmpty { "(无输出)" },
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFD0D0D8),
        )
    }
}

private fun previewStatusText(status: ToolBlockStatus?): String = when (status) {
    ToolBlockStatus.STREAMING, ToolBlockStatus.PENDING, ToolBlockStatus.RUNNING -> "运行中"
    ToolBlockStatus.SUCCESS -> "完成"
    ToolBlockStatus.FAILED -> "失败"
    ToolBlockStatus.CANCELLED -> "已取消"
    ToolBlockStatus.TIMEOUT -> "超时"
    null -> ""
}

private fun previewStatusColor(status: ToolBlockStatus?): Color = when (status) {
    ToolBlockStatus.STREAMING, ToolBlockStatus.PENDING, ToolBlockStatus.RUNNING -> Color(0xFF007AFF)
    ToolBlockStatus.SUCCESS -> Color(0xFF34C759)
    ToolBlockStatus.FAILED -> Color(0xFFFF3B30)
    ToolBlockStatus.CANCELLED -> Color(0xFF8E8E93)
    ToolBlockStatus.TIMEOUT -> Color(0xFFFF9500)
    null -> Color(0xFF8E8E93)
}