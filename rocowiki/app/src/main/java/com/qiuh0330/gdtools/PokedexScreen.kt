package com.qiuh0330.gdtools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val STAGES = listOf("初级", "中级", "高级", "首领")

@Composable
fun PokedexScreen(onPetCardClick: (Pet) -> Unit, gridState: LazyGridState) {
    var search by rememberSaveable { mutableStateOf("") }
    var stage by rememberSaveable { mutableStateOf("") }
    var attr by rememberSaveable { mutableStateOf("") }
    var egg by rememberSaveable { mutableStateOf("") }
    var shiny by rememberSaveable { mutableStateOf(false) }

    val pets = remember(search, stage, attr, egg, shiny) {
        val q = search.trim().lowercase()
        Repo.petsSorted.filter { p ->
            if (q.isNotEmpty() && !p.name.contains(q) &&
                !p.id.toString().contains(q) && !p.bookId.toString().contains(q)
            ) return@filter false
            if (stage.isNotEmpty() && p.stage != stage) return@filter false
            if (attr.isNotEmpty() && !p.attrs.contains(attr)) return@filter false
            if (egg.isNotEmpty() && !p.eggGroups.contains(egg)) return@filter false
            if (shiny && !p.hasShiny) return@filter false
            true
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 搜索 + 筛选
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            SmallSearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "搜索精灵名称、编号...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterDropdown("时期", STAGES, stage, "全部时期") { stage = it }
                FilterDropdown("属性", Repo.allAttrs, attr, "全部属性") { attr = it }
                FilterDropdown("蛋组", Repo.allEggGroupsOfPets, egg, "全部蛋组") { egg = it }
                FilterDropdown("异色", listOf("有异色"), if (shiny) "有异色" else "", "全部") {
                    shiny = it == "有异色"
                }
            }
        }

        if (pets.isEmpty()) {
            EmptyState("🔍", "没有找到匹配的精灵")
        } else {
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 165.dp),
                    state = gridState,
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(pets, key = { it.id }) { pet ->
                        PetCard(pet, showShiny = shiny, onClick = { onPetCardClick(pet) })
                    }
                }
                LazyGridScrollbar(gridState, Modifier.align(Alignment.CenterEnd).padding(end = 2.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PetCard(pet: Pet, showShiny: Boolean = false, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (pet.hasImage) {
                PetImage(pet.id, 100.dp, shiny = showShiny && pet.hasShiny)
                Spacer(Modifier.height(8.dp))
            }
            Text(
                pet.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
            if (pet.form.isNotEmpty()) {
                Text("(${pet.form})", fontSize = 11.sp, color = Color(0xFF888888), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(4.dp))
            Row {
                if (pet.attrs.isNotEmpty()) {
                    Text(pet.attrs.joinToString(" / ") + "·", fontSize = 11.sp, color = TagOrangeFg)
                }
                Text(pet.stage, fontSize = 11.sp, color = Color(0xFF999999))
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (pet.starlight > 0) {
                    Tag("星光${pet.starlight}", TagYellowBg, TagYellowFg, 11.sp)
                }
                if (pet.hasShiny) {
                    Tag("异色", Color(0xFFF9F0FF), Color(0xFF9254DE), 11.sp)
                }
                if (pet.attackTend.isNotEmpty()) {
                    Tag(pet.attackTend, TagPinkBg, TagPinkFg, 11.sp)
                }
                if (pet.eggGroups.isNotEmpty()) {
                    Tag("蛋组：${pet.eggGroups.joinToString(" / ")}", TagCyanBg, TagCyanFg, 11.sp)
                }
            }
        }
    }
}
