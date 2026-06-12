package com.qiuh0330.gdtools

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val VERSION_URL = "https://qiuh0330.github.io/rocowiki/version.json"

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
)

fun currentVersionCode(context: Context): Long {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return PackageInfoCompat.getLongVersionCode(info)
}

fun currentVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

/** 拉取站点上的 version.json，失败返回 null */
fun fetchUpdateInfo(): UpdateInfo? {
    return try {
        // 加时间戳绕过 CDN 缓存
        val url = URL("$VERSION_URL?t=${System.currentTimeMillis()}")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val json = JSONObject(text)
        UpdateInfo(
            versionCode = json.optLong("versionCode"),
            versionName = json.optString("versionName"),
            apkUrl = json.optString("apkUrl"),
            changelog = json.optString("changelog"),
        )
    } catch (e: Exception) {
        null
    }
}

/** 下载 APK 到应用私有目录，onProgress 回调 0~1 进度，失败返回 null */
private fun downloadApk(context: Context, apkUrl: String, onProgress: (Float) -> Unit): File? {
    return try {
        val conn = URL(apkUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 60000
        val total = conn.contentLength
        val dir = File(context.filesDir, "apk").apply { mkdirs() }
        val file = File(dir, "update.apk")
        conn.inputStream.use { input ->
            FileOutputStream(file).use { out ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val read = input.read(buf)
                    if (read == -1) break
                    out.write(buf, 0, read)
                    done += read
                    if (total > 0) onProgress(done.toFloat() / total)
                }
            }
        }
        conn.disconnect()
        file
    } catch (e: Exception) {
        null
    }
}

/** 通过 FileProvider 拉起系统安装器（首次会引导授予「安装未知应用」权限） */
private fun installApk(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "无法启动安装：${e.message}", Toast.LENGTH_LONG).show()
    }
}

/** 新版本提示框：应用内下载（带进度）后拉起安装 */
@Composable
fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<Float?>(null) }
    var failed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        title = { Text("发现新版本 v${info.versionName}") },
        text = {
            Column {
                Text(
                    if (info.changelog.isNotEmpty()) info.changelog else "有新版本可用，是否立即更新？",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
                progress?.let { p ->
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { p.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("下载中 ${(p * 100).toInt()}%", fontSize = 12.sp, color = TextLight)
                }
                if (failed) {
                    Spacer(Modifier.height(8.dp))
                    Text("下载失败，请检查网络后重试", fontSize = 12.sp, color = BadRed)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = progress == null,
                onClick = {
                    failed = false
                    progress = 0f
                    scope.launch {
                        val file = withContext(Dispatchers.IO) {
                            downloadApk(context, info.apkUrl) { p -> progress = p }
                        }
                        progress = null
                        if (file != null) {
                            installApk(context, file)
                            onDismiss()
                        } else {
                            failed = true
                        }
                    }
                },
            ) { Text(if (progress == null) "立即更新" else "下载中…") }
        },
        dismissButton = {
            if (progress == null) {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
