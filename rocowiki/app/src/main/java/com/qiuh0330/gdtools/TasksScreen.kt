package com.qiuh0330.gdtools

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen(listState: LazyListState) {
    val context = LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }  // all / undone / done
    var showResetConfirm by remember { mutableStateOf(false) }
    // 手动展开/收起的覆盖项（默认状态见 defaultExpanded）
    val expandOverride = remember { mutableStateMapOf<Int, Boolean>() }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = readTextFromUri(context, uri)
        if (text == null) {
            Toast.makeText(context, "读取文件失败", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        when (Store.importTasksJson(text)) {
            true -> Toast.makeText(context, "检测到旧版进度格式，已自动转换并导入！", Toast.LENGTH_LONG).show()
            false -> Toast.makeText(context, "导入成功！", Toast.LENGTH_SHORT).show()
            null -> Toast.makeText(context, "导入失败：文件格式错误", Toast.LENGTH_LONG).show()
        }
    }

    // 读取 tasksDone 使筛选随勾选变化自动刷新
    val doneVersion = Store.tasksDone.size
    val handbooks = remember(search, filter, doneVersion) {
        val q = search.trim().lowercase()
        Repo.handbooks.filter { hb ->
            if (q.isNotEmpty() && !hb.bookId.toString().contains(q) && !hb.petName.lowercase().contains(q)) {
                return@filter false
            }
            val (done, total) = Store.handbookProgress(hb)
            when (filter) {
                "done" -> done >= total
                "undone" -> done < total
                else -> true
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            SmallSearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "搜索图鉴编号或精灵名称...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterDropdown(
                    "全部任务",
                    listOf("未完成", "已完成"),
                    when (filter) { "undone" -> "未完成"; "done" -> "已完成"; else -> "" },
                ) {
                    filter = when (it) { "未完成" -> "undone"; "已完成" -> "done"; else -> "all" }
                }
                TextButton(onClick = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    saveToDownloads(context, "roco_tasks_$date.json", Store.exportTasksJson())
                }) { Text("📤 导出", fontSize = 13.sp) }
                TextButton(onClick = { importLauncher.launch("*/*") }) { Text("📥 导入", fontSize = 13.sp) }
                TextButton(onClick = { showResetConfirm = true }) {
                    Text("🔄 重置", fontSize = 13.sp, color = Color(0xFFFF4D4F))
                }
            }
        }

        if (handbooks.isEmpty()) {
            EmptyState("📝", "没有找到匹配的任务")
        } else {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(handbooks, key = { it.bookId }) { hb ->
                        HandbookCard(
                            hb = hb,
                            forceContext = search.isNotEmpty() || filter != "all",
                            expandOverride = expandOverride,
                        )
                    }
                }
                LazyListScrollbar(listState, Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("重置全部进度") },
            text = { Text("确定要重置所有任务进度吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { Store.resetTasks(); showResetConfirm = false }) {
                    Text("重置", color = Color(0xFFFF4D4F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun HandbookCard(
    hb: Handbook,
    forceContext: Boolean,
    expandOverride: MutableMap<Int, Boolean>,
) {
    val (done, total) = Store.handbookProgress(hb)
    val pct = if (total > 0) done * 100 / total else 0
    // 与网页版一致：搜索/筛选中，或未完成时默认展开
    val defaultExpanded = forceContext || pct < 100
    val expanded = expandOverride[hb.bookId] ?: defaultExpanded

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandOverride[hb.bookId] = !expanded }
                    .background(Color(0xFFFAFAFA))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    "#${hb.bookId} ${hb.petName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text("$done/$total ($pct%)", fontSize = 12.sp, color = TextLight)
            }
            if (expanded) {
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    hb.tasks.forEach { task ->
                        val taskDone = Store.isTaskDone(hb.bookId.toString(), task.taskId)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { Store.toggleTask(hb.bookId.toString(), task.taskId) },
                        ) {
                            Checkbox(
                                checked = taskDone,
                                onCheckedChange = { Store.toggleTask(hb.bookId.toString(), task.taskId) },
                            )
                            Text(
                                task.desc,
                                fontSize = 13.sp,
                                color = if (taskDone) Color(0xFFAAAAAA) else Color(0xFF333333),
                                textDecoration = if (taskDone) TextDecoration.LineThrough else null,
                            )
                        }
                    }
                }
            }
        }
    }
}
