package com.qiuh0330.gdtools

data class Pet(
    val bookId: Int,          // 图鉴编号
    val id: Int,              // 编号
    val name: String,         // 名称
    val family: String,       // 家族
    val finalFormIds: List<Int>, // 最终形态编号
    val form: String,         // 形态
    val stage: String,        // 时期
    val attrs: List<String>,  // 属性
    val hp: Int,
    val phyAtk: Int,
    val magAtk: Int,
    val phyDef: Int,
    val magDef: Int,
    val speed: Int,
    val traitId: Long,        // 特性
    val eggGroups: List<String>, // 蛋组
    val evolution: String?,   // 进化链
    val levelSkills: List<Long>,  // 升级技能
    val learnSkills: List<Long>,  // 学习技能
    val bloodSkills: List<Long>,  // 血脉技能
    val starlight: Int,       // 星光值
    val hasImage: Boolean,    // 图片
    val hasShiny: Boolean,    // 异色图片
    val attackTend: String,   // 攻击倾向
    val total: Int,           // 种族值
)

data class Skill(
    val id: String,
    val name: String,
    val desc: String,
    val cost: Int,    // 耗能
    val power: Int,   // 威力
    val attr: String, // 属性
    val category: String, // 分类
)

data class Trait(
    val id: String,
    val name: String,
    val desc: String,
)

data class HandbookTask(val taskId: Int, val desc: String)

data class Handbook(
    val bookId: Int,        // 图鉴编号
    val petName: String,    // 精灵名称
    val tasks: List<HandbookTask>,
)

data class EggPet(
    val name: String,   // 名称
    val id: Int,        // 编号
    val bookId: Int,    // 图鉴编号
)

data class PetOption(
    val name: String,
    val id: Int,
    val bookId: Int,
    val eggGroups: List<String>,
)

data class PetEgg(
    val name: String,
    val petId: Int,
    val bookId: Int,
    val weightMin: Int, // g
    val weightMax: Int,
    val heightMin: Int, // cm
    val heightMax: Int,
)

data class EggMatch(
    val egg: PetEgg,
    val hPct: Int,
    val wPct: Int,
    val score: Int,
)
