package com.qiuh0330.gdtools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class SkillRow(val skill: Skill, val source: String)

/** 精灵详情：占满整个屏幕的独立页面 */
@Composable
fun PetDetailScreen(petId: Int, onClose: () -> Unit) {
    val pet = Repo.pets[petId] ?: run { onClose(); return }
    val trait = if (pet.traitId != 0L) Repo.traits[pet.traitId.toString()] else null

    BackHandler(onBack = onClose)

    val allSkills = buildList {
        pet.levelSkills.forEach { id -> Repo.skills[id.toString()]?.let { add(SkillRow(it, "升级")) } }
        pet.learnSkills.forEach { id -> Repo.skills[id.toString()]?.let { add(SkillRow(it, "学习")) } }
        pet.bloodSkills.forEach { id -> Repo.skills[id.toString()]?.let { add(SkillRow(it, "血脉")) } }
    }.filter { it.skill.name.isNotEmpty() && it.skill.name != "-" }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            // 拦截空白处点击，避免穿透到下层标签页
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
    ) {
        // 顶部返回栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.Black)
            }
            Text("精灵详情", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = Color(0xFFE0E0E0))

        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DetailHeader(pet) }

            if (trait != null) {
                item { SectionCard("特性") { TraitCard(trait) } }
            }

            if (!pet.evolution.isNullOrEmpty()) {
                item {
                    SectionCard("进化链") {
                        HtmlBoldText(pet.evolution, fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFFD46B08))
                    }
                }
            }

            item {
                SectionCard("技能 (${allSkills.size}个)") {
                    if (allSkills.isEmpty()) {
                        Text("暂无技能数据", color = Color(0xFF999999), fontSize = 13.sp)
                    } else {
                        allSkills.forEachIndexed { index, row ->
                            SkillEntry(row)
                            if (index < allSkills.lastIndex) {
                                HorizontalDivider(
                                    color = Color(0xFFF0F0F0),
                                    modifier = Modifier.padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(pet: Pet) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (pet.hasImage) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PetImage(pet.id, 140.dp, corner = 12.dp)
                    if (pet.hasShiny) {
                        PetImage(pet.id, 140.dp, shiny = true, corner = 12.dp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(pet.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (pet.form.isNotEmpty()) {
                    Text(" (${pet.form})", fontSize = 14.sp, color = Color(0xFF888888))
                }
                Spacer(Modifier.weight(1f))
                Text("#${pet.bookId} · No.${pet.id}", fontSize = 12.sp, color = Color(0xFF999999))
            }
            Spacer(Modifier.height(6.dp))
            val subtitle = buildString {
                append(pet.attrs.takeIf { it.isNotEmpty() }?.joinToString(" / ") ?: "无")
                if (pet.starlight > 0) append(" · 星光值：${pet.starlight}")
                append(" · 攻击倾向：")
                append(pet.attackTend)
                if (pet.eggGroups.isNotEmpty()) append("\n蛋组：" + pet.eggGroups.joinToString(" / "))
            }
            Text(subtitle, fontSize = 15.sp, color = TextLight, lineHeight = 24.sp)
            Spacer(Modifier.height(12.dp))
            StatsGrid(pet)
        }
    }
}

private data class StatCell(val label: String, val value: Int, val hi: Int?, val lo: Int?)

@Composable
private fun StatsGrid(pet: Pet) {
    val cells = listOf(
        StatCell("生命", pet.hp, 120, 80),
        StatCell("物攻", pet.phyAtk, 120, 50),
        StatCell("魔攻", pet.magAtk, 120, 50),
        StatCell("物防", pet.phyDef, 120, 70),
        StatCell("魔防", pet.magDef, 120, 70),
        StatCell("速度", pet.speed, 120, 60),
        StatCell("种族值", pet.total, null, null),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        cells.forEach { c ->
            val isGood = c.hi != null && c.value >= c.hi
            val isBad = c.lo != null && c.value <= c.lo
            val bg = when {
                isGood -> GoodGreen
                isBad -> BadRed
                else -> Color(0xFFF5F7FA)
            }
            val fg = if (isGood || isBad) Color.White else Color(0xFF333333)
            val labelFg = if (isGood || isBad) Color.White.copy(alpha = 0.85f) else TextLight
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .background(bg, RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp),
            ) {
                Text(c.label, fontSize = 10.sp, color = labelFg, maxLines = 1)
                Text(c.value.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg)
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun TraitCard(trait: Trait) {
    Column(Modifier.fillMaxWidth()) {
        Text(trait.name, fontWeight = FontWeight.Bold, color = Color(0xFF389E0D), fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        TermText(trait.desc, fontSize = 13.sp)
    }
}

@Composable
private fun SkillEntry(row: SkillRow) {
    val s = row.skill
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (s.attr.isNotEmpty()) Tag(s.attr, TagOrangeBg, TagOrangeFg, 11.sp)
            if (s.category.isNotEmpty()) Tag(s.category, TagPinkBg, TagPinkFg, 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${row.source}获得 · 威力 ${s.power} · 耗能 ${s.cost}",
            fontSize = 13.sp,
            color = Color(0xFF333333),
        )
        Spacer(Modifier.height(4.dp))
        TermText(s.desc, fontSize = 13.sp)
    }
}
