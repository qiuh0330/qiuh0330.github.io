package com.qiuh0330.gdtools

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 从 assets/json 加载全部图鉴数据（与网页版同源） */
object Repo {

    @Volatile
    var isLoaded = false
        private set

    var pets: Map<Int, Pet> = emptyMap()            // key = 编号
        private set
    var petsSorted: List<Pet> = emptyList()         // 按图鉴编号排序
        private set
    var skills: Map<String, Skill> = emptyMap()
        private set
    var traits: Map<String, Trait> = emptyMap()
        private set
    var handbooks: List<Handbook> = emptyList()     // 按图鉴编号排序
        private set
    var handbookMap: Map<String, Handbook> = emptyMap()  // key = 图鉴编号字符串
        private set
    var eggGroupNames: List<String> = emptyList()   // 蛋组列表
        private set
    var eggGroupPets: Map<String, List<EggPet>> = emptyMap()
        private set
    var allPetOptions: List<PetOption> = emptyList()
        private set
    var petEggs: List<PetEgg> = emptyList()
        private set
    var allAttrs: List<String> = emptyList()
        private set
    var allAttackTends: List<String> = emptyList()
        private set
    var allEggGroupsOfPets: List<String> = emptyList()
        private set

    @Synchronized
    fun load(context: Context) {
        if (isLoaded) return

        fun read(name: String): String =
            context.assets.open("json/$name").bufferedReader().use { it.readText() }

        // ---- pet_summary ----
        val petJson = JSONObject(read("pet_summary.json"))
        val petMap = HashMap<Int, Pet>()
        for (key in petJson.keys()) {
            val o = petJson.getJSONObject(key)
            val pet = Pet(
                bookId = o.optInt("图鉴编号"),
                id = o.optInt("编号"),
                name = o.optString("名称"),
                family = o.optString("家族"),
                finalFormIds = o.optJSONArray("最终形态编号").toIntList(),
                form = o.optString("形态"),
                stage = o.optString("时期"),
                attrs = o.optJSONArray("属性").toStringList(),
                hp = o.optInt("生命"),
                phyAtk = o.optInt("物攻"),
                magAtk = o.optInt("魔攻"),
                phyDef = o.optInt("物防"),
                magDef = o.optInt("魔防"),
                speed = o.optInt("速度"),
                traitId = o.optLong("特性"),
                eggGroups = o.optJSONArray("蛋组").toStringList(),
                evolution = o.optString("进化链").takeIf { it.isNotEmpty() && it != "null" && it != "-" },
                levelSkills = o.optJSONArray("升级技能").toLongList(),
                learnSkills = o.optJSONArray("学习技能").toLongList(),
                bloodSkills = o.optJSONArray("血脉技能").toLongList(),
                starlight = o.optInt("星光值"),
                hasImage = o.optBoolean("图片"),
                hasShiny = o.optBoolean("异色图片"),
                attackTend = o.optString("攻击倾向"),
                total = o.optInt("种族值"),
            )
            petMap[pet.id] = pet
        }
        pets = petMap
        petsSorted = petMap.values.sortedWith(compareBy({ it.bookId }, { it.id }))
        allAttrs = petMap.values.flatMap { it.attrs }.filter { it.isNotEmpty() }.distinct().sorted()
        allAttackTends = petMap.values.map { it.attackTend }.filter { it.isNotEmpty() }.distinct().sorted()
        allEggGroupsOfPets = petMap.values.flatMap { it.eggGroups }.filter { it.isNotEmpty() }.distinct().sorted()

        // ---- skill_summary ----
        val skillJson = JSONObject(read("skill_summary.json"))
        val skillMap = HashMap<String, Skill>()
        for (key in skillJson.keys()) {
            val o = skillJson.getJSONObject(key)
            skillMap[key] = Skill(
                id = key,
                name = o.optString("名称"),
                desc = o.optString("描述"),
                cost = o.optInt("耗能"),
                power = o.optInt("威力"),
                attr = o.optString("属性"),
                category = o.optString("分类"),
            )
        }
        skills = skillMap

        // ---- trait_summary ----
        val traitJson = JSONObject(read("trait_summary.json"))
        val traitMap = HashMap<String, Trait>()
        for (key in traitJson.keys()) {
            val o = traitJson.getJSONObject(key)
            traitMap[key] = Trait(key, o.optString("名称"), o.optString("描述"))
        }
        traits = traitMap

        // ---- handbook_tasks ----
        val hbJson = JSONObject(read("handbook_tasks.json"))
        val hbMap = HashMap<String, Handbook>()
        for (key in hbJson.keys()) {
            val o = hbJson.getJSONObject(key)
            val tasksArr = o.optJSONArray("任务") ?: JSONArray()
            val tasks = ArrayList<HandbookTask>(tasksArr.length())
            for (i in 0 until tasksArr.length()) {
                val t = tasksArr.getJSONObject(i)
                tasks.add(HandbookTask(t.optInt("任务ID"), t.optString("描述")))
            }
            hbMap[key] = Handbook(o.optInt("图鉴编号"), o.optString("精灵名称"), tasks)
        }
        handbookMap = hbMap
        handbooks = hbMap.values.sortedBy { it.bookId }

        // ---- egg_group ----
        val eggJson = JSONObject(read("egg_group.json"))
        eggGroupNames = eggJson.optJSONArray("蛋组列表").toStringList()
        val mapping = HashMap<String, List<EggPet>>()
        val mappingJson = eggJson.optJSONObject("蛋组精灵映射") ?: JSONObject()
        for (group in mappingJson.keys()) {
            val arr = mappingJson.getJSONArray(group)
            val list = ArrayList<EggPet>(arr.length())
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                list.add(EggPet(p.optString("名称"), p.optInt("编号"), p.optInt("图鉴编号")))
            }
            mapping[group] = list
        }
        eggGroupPets = mapping
        val optionsArr = eggJson.optJSONArray("所有精灵选项") ?: JSONArray()
        val options = ArrayList<PetOption>(optionsArr.length())
        for (i in 0 until optionsArr.length()) {
            val p = optionsArr.getJSONObject(i)
            options.add(
                PetOption(
                    p.optString("名称"), p.optInt("编号"), p.optInt("图鉴编号"),
                    p.optJSONArray("蛋组").toStringList()
                )
            )
        }
        allPetOptions = options

        // ---- pet_egg ----
        val petEggArr = JSONArray(read("pet_egg.json"))
        val eggs = ArrayList<PetEgg>(petEggArr.length())
        for (i in 0 until petEggArr.length()) {
            val o = petEggArr.getJSONObject(i)
            eggs.add(
                PetEgg(
                    name = o.optString("name"),
                    petId = o.optInt("pet_id"),
                    bookId = o.optInt("book_id"),
                    weightMin = o.optInt("weight_min"),
                    weightMax = o.optInt("weight_max"),
                    heightMin = o.optInt("height_min"),
                    heightMax = o.optInt("height_max"),
                )
            )
        }
        petEggs = eggs

        isLoaded = true
    }

    /** 查蛋：输入身高(m)体重(kg)，返回按得分排序的匹配 */
    fun searchEgg(heightM: Double, weightKg: Double): List<EggMatch> {
        val h = Math.round(heightM * 100).toInt()
        val w = Math.round(weightKg * 1000).toInt()
        return petEggs
            .filter { h >= it.heightMin && h <= it.heightMax && w >= it.weightMin && w <= it.weightMax }
            .map {
                val hRange = (it.heightMax - it.heightMin).takeIf { r -> r != 0 } ?: 1
                val wRange = (it.weightMax - it.weightMin).takeIf { r -> r != 0 } ?: 1
                val hPct = (h - it.heightMin).toDouble() / hRange
                val wPct = (w - it.weightMin).toDouble() / wRange
                val score = Math.round((1 - maxOf(Math.abs(hPct - 0.5), Math.abs(wPct - 0.5)) * 2) * 100).toInt()
                EggMatch(it, Math.round(hPct * 100).toInt(), Math.round(wPct * 100).toInt(), score)
            }
            .sortedByDescending { it.score }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() && s != "null" } }
}

private fun JSONArray?.toIntList(): List<Int> {
    if (this == null) return emptyList()
    return (0 until length()).map { optInt(it) }
}

private fun JSONArray?.toLongList(): List<Long> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { if (isNull(it)) null else optLong(it) }
}
