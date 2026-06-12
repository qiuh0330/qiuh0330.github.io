package com.qiuh0330.gdtools

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONObject

/**
 * 任务进度与收集记录的持久化。
 * 存储/导出格式与网页版 localStorage 完全一致，导出的 JSON 可在网页版导入，反之亦然：
 *  - 任务:   {"<图鉴编号>": {"<任务ID>": true/false}}
 *  - 收集:   {"<精灵编号>": {"<类型>": true/false}}
 */
object Store {

    val BREEDER_TYPES = listOf("炫彩", "异色", "固执", "聪明", "开朗", "胆小", "平和", "沉默", "踏实")
    val BREEDER_GROUPS = listOf(
        listOf("炫彩", "异色"),
        listOf("固执", "聪明", "开朗", "胆小"),
        listOf("平和", "沉默", "踏实"),
    )

    private const val TASKS_KEY = "roco_pet_tasks_v1"
    private const val BREEDERS_KEY = "roco_egggroup_breeders_v1"

    private lateinit var prefs: SharedPreferences

    /** key = "图鉴编号|任务ID"，只存 true 项 */
    val tasksDone = mutableStateMapOf<String, Boolean>()

    /** key = "精灵编号|类型"，只存 true 项 */
    val breeders = mutableStateMapOf<String, Boolean>()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("rocowiki", Context.MODE_PRIVATE)
        tasksDone.putAll(parseNested(prefs.getString(TASKS_KEY, null)))
        breeders.putAll(parseNested(prefs.getString(BREEDERS_KEY, null)))
    }

    // ---------- 任务 ----------

    fun isTaskDone(hbId: String, taskId: Int): Boolean = tasksDone["$hbId|$taskId"] == true

    fun toggleTask(hbId: String, taskId: Int) {
        val key = "$hbId|$taskId"
        if (tasksDone[key] == true) tasksDone.remove(key) else tasksDone[key] = true
        persistTasks()
    }

    fun handbookProgress(hb: Handbook): Pair<Int, Int> {
        val done = hb.tasks.count { isTaskDone(hb.bookId.toString(), it.taskId) }
        return done to hb.tasks.size
    }

    fun exportTasksJson(): String = buildNested(tasksDone).toString(2)

    /** 返回 null 表示失败；返回 true 表示检测到旧格式并已转换 */
    fun importTasksJson(text: String): Boolean? {
        return try {
            var json = JSONObject(text)
            var converted = false
            val keys = json.keys().asSequence().toList()
            if (keys.isNotEmpty() && keys[0].startsWith("task_")) {
                json = convertOldProgress(json)
                converted = true
            }
            tasksDone.clear()
            tasksDone.putAll(parseNestedObj(json))
            persistTasks()
            converted
        } catch (e: Exception) {
            null
        }
    }

    fun resetTasks() {
        tasksDone.clear()
        prefs.edit().remove(TASKS_KEY).apply()
    }

    /** 旧格式 task_<图鉴编号>_<任务下标> → 新格式 */
    private fun convertOldProgress(old: JSONObject): JSONObject {
        val out = JSONObject()
        val regex = Regex("^task_(\\d+)_(\\d+)$")
        for (key in old.keys()) {
            val m = regex.matchEntire(key) ?: continue
            val hbId = m.groupValues[1]
            val taskIndex = m.groupValues[2].toInt()
            val hb = Repo.handbookMap[hbId] ?: continue
            if (taskIndex >= hb.tasks.size) continue
            val taskId = hb.tasks[taskIndex].taskId.toString()
            val obj = out.optJSONObject(hbId) ?: JSONObject().also { out.put(hbId, it) }
            obj.put(taskId, old.optBoolean(key))
        }
        return out
    }

    private fun persistTasks() {
        prefs.edit().putString(TASKS_KEY, buildNested(tasksDone).toString()).apply()
    }

    // ---------- 收集记录 ----------

    fun isBreederChecked(petId: Int, type: String): Boolean = breeders["$petId|$type"] == true

    fun toggleBreeder(petId: Int, type: String) {
        val key = "$petId|$type"
        if (breeders[key] == true) breeders.remove(key) else breeders[key] = true
        persistBreeders()
    }

    fun exportBreedersJson(): String = buildNested(breeders).toString(2)

    fun importBreedersJson(text: String): Boolean {
        return try {
            val json = JSONObject(text)
            breeders.clear()
            breeders.putAll(parseNestedObj(json))
            persistBreeders()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resetBreeders() {
        breeders.clear()
        prefs.edit().remove(BREEDERS_KEY).apply()
    }

    private fun persistBreeders() {
        prefs.edit().putString(BREEDERS_KEY, buildNested(breeders).toString()).apply()
    }

    // ---------- 嵌套 JSON ↔ 扁平 map ----------

    private fun parseNested(text: String?): Map<String, Boolean> {
        if (text.isNullOrEmpty()) return emptyMap()
        return try {
            parseNestedObj(JSONObject(text))
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun parseNestedObj(json: JSONObject): Map<String, Boolean> {
        val out = HashMap<String, Boolean>()
        for (outer in json.keys()) {
            val inner = json.optJSONObject(outer) ?: continue
            for (k in inner.keys()) {
                if (inner.optBoolean(k)) out["$outer|$k"] = true
            }
        }
        return out
    }

    private fun buildNested(flat: Map<String, Boolean>): JSONObject {
        val out = JSONObject()
        for ((key, v) in flat) {
            if (!v) continue
            val parts = key.split("|", limit = 2)
            if (parts.size != 2) continue
            val obj = out.optJSONObject(parts[0]) ?: JSONObject().also { out.put(parts[0], it) }
            obj.put(parts[1], true)
        }
        return out
    }
}
