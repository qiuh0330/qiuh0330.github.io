package com.qiuh0330.gdtools

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EggScreen(onShowPet: (Int) -> Unit, listState: LazyListState) {
    var heightInput by rememberSaveable { mutableStateOf("") }
    var weightInput by rememberSaveable { mutableStateOf("") }
    var eggResult by remember { mutableStateOf<List<EggMatch>?>(null) }
    var eggError by remember { mutableStateOf<String?>(null) }

    var queryInput by rememberSaveable { mutableStateOf("") }
    var queryPetId by rememberSaveable { mutableStateOf<Int?>(null) }
    var querySearch by rememberSaveable { mutableStateOf("") }
    var suggestionsVisible by remember { mutableStateOf(false) }

    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    fun doSearchEgg() {
        val h = heightInput.toDoubleOrNull()
        val w = weightInput.toDoubleOrNull()
        if (h == null || w == null || h <= 0 || w <= 0) {
            eggError = "请输入有效的身高和体重"
            eggResult = null
            return
        }
        eggError = null
        eggResult = Repo.searchEgg(h, w)
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // ===== 查蛋 =====
        item {
            SectionCard("查蛋") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("身高（m）", fontSize = 12.sp, color = TextLight)
                        Spacer(Modifier.height(4.dp))
                        SmallSearchField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            placeholder = "如 0.25",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("体重（kg）", fontSize = 12.sp, color = TextLight)
                        Spacer(Modifier.height(4.dp))
                        SmallSearchField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            placeholder = "如 1.5",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    Button(
                        onClick = { doSearchEgg() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        Text("查蛋")
                    }
                }
                eggError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = TextLight, fontSize = 13.sp)
                }
                eggResult?.let { matches ->
                    Spacer(Modifier.height(10.dp))
                    if (matches.isEmpty()) {
                        Text("没有找到匹配的精灵，请检查身高和体重数值", color = TextLight, fontSize = 13.sp)
                    } else {
                        Text("共找到 ${matches.size} 种可能的精灵，按可能性得分排序：", color = TextLight, fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        matches.forEach { m -> EggMatchCard(m, onShowPet) }
                    }
                }
            }
        }

        // ===== 查询同蛋组精灵 =====
        item {
            SectionCard("查询同蛋组精灵") {
                SmallSearchField(
                    value = queryInput,
                    onValueChange = {
                        queryInput = it
                        suggestionsVisible = it.trim().isNotEmpty()
                    },
                    placeholder = "输入精灵名称或编号搜索...",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (suggestionsVisible) {
                    val q = queryInput.trim().lowercase()
                    val matched = Repo.allPetOptions.filter {
                        it.name.lowercase().contains(q) || it.id.toString().contains(q)
                    }.take(20)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                    ) {
                        matched.forEach { opt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        queryInput = opt.name
                                        queryPetId = opt.id
                                        querySearch = ""
                                        suggestionsVisible = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Text(opt.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("No.${opt.id}", fontSize = 12.sp, color = Color(0xFF999999))
                            }
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                        if (matched.isEmpty()) {
                            Text("无匹配精灵", color = TextLight, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                queryPetId?.let { pid ->
                    val pet = Repo.pets[pid]
                    if (pet == null) {
                        Spacer(Modifier.height(8.dp))
                        Text("该精灵没有蛋组信息", color = TextLight, fontSize = 13.sp)
                    } else {
                        val groups = pet.eggGroups.filter { it.isNotEmpty() }
                        // 同蛋组所有精灵去重（同家族仅高级形态代表）
                        val related = LinkedHashMap<Int, String>()
                        groups.forEach { g ->
                            Repo.eggGroupPets[g]?.forEach { p ->
                                if (p.id != pid) related[p.id] = p.name
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (groups.isEmpty()) {
                            Text("该精灵没有蛋组信息", color = TextLight, fontSize = 13.sp)
                        } else if (related.isEmpty()) {
                            Text("暂无同蛋组的其他精灵", color = TextLight, fontSize = 13.sp)
                        } else {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "与 ${pet.name} 同蛋组的精灵（同家族仅显示高级形态）：",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(8.dp))
                                SmallSearchField(
                                    value = querySearch,
                                    onValueChange = { querySearch = it },
                                    placeholder = "搜索同蛋组精灵...",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                                val sq = querySearch.trim().lowercase()
                                val filtered = related.entries.filter { (id, name) ->
                                    sq.isEmpty() || name.lowercase().contains(sq) || id.toString().contains(sq)
                                }
                                if (filtered.isEmpty()) {
                                    Text("没有匹配的精灵", color = Color(0xFF999999), fontSize = 13.sp)
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        filtered.forEach { (id, name) ->
                                            val petData = Repo.pets[id]
                                            val attack = petData?.attackTend ?: ""
                                            val eggs = petData?.eggGroups
                                                ?.filter { it.isNotEmpty() }
                                                ?.joinToString(" / ") ?: ""
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .background(Color.White, RoundedCornerShape(14.dp))
                                                    .clickable { onShowPet(id) }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                            ) {
                                                Text(name, fontSize = 13.sp)
                                                if (attack.isNotEmpty()) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Tag(attack, TagPinkBg, TagPinkFg, 10.sp)
                                                }
                                                if (eggs.isNotEmpty()) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Tag(eggs, TagCyanBg, TagCyanFg, 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== 全部蛋组 =====
        items(Repo.eggGroupNames, key = { it }) { groupName ->
            val pets = Repo.eggGroupPets[groupName] ?: emptyList()
            if (pets.isEmpty()) return@items
            val expanded = expandedGroups[groupName] ?: false
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
                            .clickable { expandedGroups[groupName] = !expanded }
                            .padding(16.dp),
                    ) {
                        Text(groupName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text("(${pets.size}只)", fontSize = 13.sp, color = Color(0xFF999999))
                        Spacer(Modifier.weight(1f))
                        Text(if (expanded) "▲" else "▼", fontSize = 12.sp, color = TextLight)
                    }
                    if (expanded) {
                        // 按基准宽度算列数：屏幕越宽列数越多，单个不会被撑得过大
                        BoxWithConstraints(
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                        ) {
                            val baseWidth = 108.dp
                            val columns = (maxWidth / baseWidth).toInt().coerceAtLeast(3)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                pets.chunked(columns).forEach { rowPets ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        rowPets.forEach { p ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onShowPet(p.id) },
                                            ) {
                                                PetImageFill(
                                                    p.id,
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f),
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    p.name,
                                                    fontSize = 12.sp,
                                                    lineHeight = 14.sp,
                                                    maxLines = 2,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                )
                                                val attack = Repo.pets[p.id]?.attackTend ?: ""
                                                if (attack.isNotEmpty()) {
                                                    Spacer(Modifier.height(2.dp))
                                                    Tag(attack, TagPinkBg, TagPinkFg, 10.sp)
                                                }
                                            }
                                        }
                                        // 末行不足时占位，保持等宽
                                        repeat(columns - rowPets.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    LazyListScrollbar(listState, Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
    } // Box
}

@Composable
private fun EggMatchCard(m: EggMatch, onShowPet: (Int) -> Unit) {
    val scoreColor = when {
        m.score >= 70 -> GoodGreen
        m.score >= 40 -> TagOrangeFg
        else -> Color(0xFF999999)
    }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    m.egg.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (m.egg.petId != 0) Primary else Color(0xFF333333),
                    modifier = if (m.egg.petId != 0) {
                        Modifier.clickable { onShowPet(m.egg.petId) }
                    } else Modifier,
                )
                Spacer(Modifier.weight(1f))
                Text("得分 ", fontSize = 12.sp, color = TextLight)
                Text(m.score.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scoreColor)
            }
            Spacer(Modifier.height(6.dp))
            RangeBar("身高", "${m.egg.heightMin}–${m.egg.heightMax} cm", m.hPct)
            Spacer(Modifier.height(4.dp))
            RangeBar("体重", "${m.egg.weightMin}–${m.egg.weightMax} g", m.wPct)
        }
    }
}

@Composable
private fun RangeBar(label: String, range: String, pct: Int) {
    Column {
        Row {
            Text("$label $range", fontSize = 12.sp, color = Color(0xFF333333))
            Spacer(Modifier.weight(1f))
            Text("位于 $pct%", fontSize = 11.sp, color = TextLight)
        }
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (pct.coerceIn(0, 100)) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            color = Primary,
            trackColor = Color(0xFFE8E8E8),
        )
    }
}
