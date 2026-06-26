package com.qiuh0330.gdtools

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val VERSION_URL = "https://qiuh0330.github.io/rocowiki/version.json"
private const val APK_FILE_NAME = "gdtools-update.apk"

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

/** 拉取站点上的 version.json，返回 Result（失败时携带具体异常信息） */
fun fetchUpdateInfo(): Result<UpdateInfo> {
    return try {
        // 加时间戳绕过 CDN 缓存
        val url = URL("$VERSION_URL?t=${System.currentTimeMillis()}")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.instanceFollowRedirects = true
        val code = conn.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            conn.disconnect()
            return Result.failure(Exception("HTTP $code"))
        }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val json = JSONObject(text)
        Result.success(UpdateInfo(
            versionCode = json.optLong("versionCode"),
            versionName = json.optString("versionName"),
            apkUrl = json.optString("apkUrl"),
            changelog = json.optString("changelog"),
        ))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * 用系统 DownloadManager 在后台下载更新包：
 * 下载过程显示在系统通知栏，app 可正常使用/退到后台；下载完成后自动拉起安装器。
 */
fun startBackgroundUpdate(context: Context, info: UpdateInfo) {
    val appCtx = context.applicationContext
    val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // 清掉上一次可能残留的包，避免出现 gdtools-update-1.apk
    File(appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME).delete()

    val request = DownloadManager.Request(Uri.parse(info.apkUrl)).apply {
        setTitle("果冻工具 v${info.versionName}")
        setDescription("正在后台下载更新")
        setMimeType("application/vnd.android.package-archive")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalFilesDir(appCtx, Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
    }
    val downloadId = dm.enqueue(request)

    // 下载完成广播：核对 id → 成功则拉起安装
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val doneId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (doneId != downloadId) return
            try {
                appCtx.unregisterReceiver(this)
            } catch (_: Exception) {
            }
            val status = dm.query(DownloadManager.Query().setFilterById(downloadId)).use { cur ->
                if (cur.moveToFirst()) {
                    cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                } else {
                    DownloadManager.STATUS_FAILED
                }
            }
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val file = File(appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)
                installApk(appCtx, file)
            } else {
                Toast.makeText(appCtx, "更新下载失败，请稍后重试", Toast.LENGTH_LONG).show()
            }
        }
    }
    ContextCompat.registerReceiver(
        appCtx,
        receiver,
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
        ContextCompat.RECEIVER_EXPORTED,
    )

    Toast.makeText(context, "已在后台开始下载，完成后会自动提示安装", Toast.LENGTH_LONG).show()
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

/** 新版本提示框：点「立即更新」后转入后台下载，对话框随即关闭 */
@Composable
fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本 v${info.versionName}") },
        text = {
            Column {
                Text(
                    if (info.changelog.isNotEmpty()) info.changelog else "有新版本可用，是否立即更新？",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                startBackgroundUpdate(context, info)
                onDismiss()
            }) { Text("后台下载") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
