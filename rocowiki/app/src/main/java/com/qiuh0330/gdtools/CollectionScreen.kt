package com.qiuh0330.gdtools

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CollectionScreen(onShowPet: (Int) -> Unit) {
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = readTextFromUri(context, uri)
        if (text == null) {
            Toast.makeText(context, "读取文件失败", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        if (Store.importBreedersJson(text)) {
            Toast.makeText(context, "导入成功！", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "导入失败：文件格式错误", Toast.LENGTH_LONG).show()
        }
    }

    var search by rememberSaveable { mutableStateOf("") }
    var eggFilter by rememberSaveable { mutableStateOf("") }

    // 只显示高级时期的最终形态，按图鉴编号排序
    val pets = remember(search, eggFilter) {
        val q = search.trim().lowercase()
        Repo.allPetOptions.filter { p ->
            if (Repo.pets[p.id]?.stage != "高级") return@filter false
            if (q.isNotEmpty() && !p.name.lowercase().contains(q) && !p.id.toString().contains(q)) {
                return@filter false
            }
            if (eggFilter.isNotEmpty() && !p.eggGroups.contains(eggFilter)) return@filter false
            true
        }.sortedWith(compareBy({ it.bookId }, { it.id }))
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallSearchField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "搜索精灵名称、编号...",
                    modifier = Modifier.weight(1f),
                )
                FilterDropdown("全部蛋组", Repo.eggGroupNames, eggFilter) { eggFilter = it }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    saveToDownloads(context, "roco_breeders_$date.json", Store.exportBreedersJson())
                }) { Text("📤 导出记录", fontSize = 13.sp) }
                TextButton(onClick = { importLauncher.launch("*/*") }) { Text("📥 导入记录", fontSize = 13.sp) }
                TextButton(onClick = { showResetConfirm = true }) {
                    Text("🔄 重置全部", fontSize = 13.sp, color = Color(0xFFFF4D4F))
                }
            }
        }

        if (pets.isEmpty()) {
            EmptyState("🔍", "没有找到匹配的精灵")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(pets, key = { it.id }) { pet ->
                    CollectionPetCard(pet, onShowPet)
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("重置全部记录") },
            text = { Text("确定要重置所有收集记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { Store.resetBreeders(); showResetConfirm = false }) {
                    Text("重置", color = Color(0xFFFF4D4F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollectionPetCard(pet: PetOption, onShowPet: (Int) -> Unit) {
    val petData = Repo.pets[pet.id]
    val attack = petData?.attackTend ?: ""
    val form = petData?.form ?: ""
    val eggs = pet.eggGroups.filter { it.isNotEmpty() }.joinToString(" / ")
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onShowPet(pet.id) },
            ) {
                PetImage(pet.id, 40.dp, corner = 6.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    // 名称、攻击倾向、蛋组放在同一行（窄屏自动换行）
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(pet.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (attack.isNotEmpty()) {
                            Tag(attack, TagPinkBg, TagPinkFg, 10.sp)
                        }
                        if (eggs.isNotEmpty()) {
                            Tag(eggs, TagCyanBg, TagCyanFg, 10.sp)
                        }
                    }
                    // 形态放在名称下面一行
                    if (form.isNotEmpty()) {
                        Text("(${form})", fontSize = 12.sp, color = Color(0xFF888888))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Store.BREEDER_GROUPS.forEach { group ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    group.forEach { type ->
                        val checked = Store.isBreederChecked(pet.id, type)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { Store.toggleBreeder(pet.id, type) }
                                .padding(end = 6.dp),
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { Store.toggleBreeder(pet.id, type) },
                                modifier = Modifier.size(32.dp),
                            )
                            Text(type, fontSize = 12.sp, color = if (checked) Primary else Color(0xFF555555))
                        }
                    }
                }
            }
        }
    }
}
