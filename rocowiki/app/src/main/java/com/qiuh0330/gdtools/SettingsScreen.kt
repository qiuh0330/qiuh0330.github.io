package com.qiuh0330.gdtools

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    val versionName = remember { currentVersionName(context) }
    val versionCode = remember { currentVersionCode(context) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !checking) {
                        checking = true
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { fetchUpdateInfo() }
                            checking = false
                            result.fold(
                                onSuccess = { info ->
                                    if (info.versionCode <= versionCode)
                                        Toast.makeText(context, "已是最新版本 v$versionName", Toast.LENGTH_SHORT).show()
                                    else
                                        update = info
                                },
                                onFailure = { e ->
                                    val msg = e.message?.take(120) ?: "未知错误"
                                    Toast.makeText(context, "检查失败：$msg", Toast.LENGTH_LONG).show()
                                },
                            )
                        }
                    }
                    .padding(16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("检查更新", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("当前版本 v$versionName", fontSize = 12.sp, color = TextLight)
                }
                if (checking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("›", fontSize = 20.sp, color = Color(0xFF999999))
                }
            }
        }
    }

    update?.let { info ->
        UpdateDialog(info = info, onDismiss = { update = null })
    }
}
