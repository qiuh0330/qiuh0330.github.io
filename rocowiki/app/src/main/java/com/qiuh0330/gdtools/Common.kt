package com.qiuh0330.gdtools

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

val Primary = Color(0xFF4A90D9)
val TextLight = Color(0xFF666666)
val TagBlueBg = Color(0xFFEEF5FF)
val TagOrangeBg = Color(0xFFFFF2E8)
val TagOrangeFg = Color(0xFFFA8C16)
val TagPinkBg = Color(0xFFFFF0F6)
val TagPinkFg = Color(0xFFEB2F96)
val TagCyanBg = Color(0xFFE6FFFB)
val TagCyanFg = Color(0xFF13C2C2)
val TagYellowBg = Color(0xFFFFF9E6)
val TagYellowFg = Color(0xFFD48806)
val GoodGreen = Color(0xFF52C41A)
val BadRed = Color(0xFFFF7875)

/** 精灵图片（assets/image/<编号>.webp） */
@Composable
fun PetImage(petId: Int, size: Dp, shiny: Boolean = false, corner: Dp = 8.dp) {
    AsyncImage(
        model = "file:///android_asset/image/$petId${if (shiny) "_shiny" else ""}.webp",
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(size)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(corner))
            .padding(2.dp)
    )
}

/** 精灵图片（由调用方控制尺寸，配合 weight/aspectRatio 自适应屏幕宽度） */
@Composable
fun PetImageFill(petId: Int, modifier: Modifier, corner: Dp = 8.dp) {
    AsyncImage(
        model = "file:///android_asset/image/$petId.webp",
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(corner))
            .padding(2.dp)
    )
}

/** 小圆角标签 */
@Composable
fun Tag(text: String, bg: Color = TagBlueBg, fg: Color = Primary, fontSize: TextUnit = 12.sp) {
    Text(
        text = text,
        color = fg,
        fontSize = fontSize,
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/**
 * 渲染可能含有 game-term 标记的描述文本，
 * 术语带下划线，点击弹出释义。
 */
@Composable
fun TermText(
    html: String,
    fontSize: TextUnit = 13.sp,
    color: Color = Color(0xFF333333),
) {
    var termTitle by remember { mutableStateOf<String?>(null) }
    var termDef by remember { mutableStateOf("") }

    val regex = remember { Regex("<span class=\"game-term\" data-tooltip=\"([^\"]*)\">(.*?)</span>") }
    val annotated: AnnotatedString = remember(html) {
        buildAnnotatedString {
            var pos = 0
            for (m in regex.findAll(html)) {
                append(unescapeHtml(html.substring(pos, m.range.first)))
                val tooltip = unescapeHtml(m.groupValues[1])
                val term = unescapeHtml(m.groupValues[2])
                pushStringAnnotation("term", tooltip)
                withStyle(SpanStyle(color = Primary, textDecoration = TextDecoration.Underline)) {
                    append(term)
                }
                pop()
                pos = m.range.last + 1
            }
            append(unescapeHtml(html.substring(pos)))
        }
    }

    ClickableText(
        text = annotated,
        style = TextStyle(fontSize = fontSize, color = color, lineHeight = fontSize * 1.6),
        onClick = { offset ->
            annotated.getStringAnnotations("term", offset, offset).firstOrNull()?.let { ann ->
                termTitle = annotated.text.substring(
                    annotated.getStringAnnotations("term", offset, offset).first().start,
                    annotated.getStringAnnotations("term", offset, offset).first().end
                )
                termDef = ann.item
            }
        }
    )

    termTitle?.let { title ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { termTitle = null }) {
            Column(
                Modifier
                    .fillMaxWidth(0.96f)
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(4.dp, 16.dp)
                            .background(Primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        title,
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    termDef,
                    fontSize = 14.sp,
                    color = Color(0xFF555555),
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "知道了",
                    fontSize = 14.sp,
                    color = Primary,
                    style = TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.End)
                        .background(Color(0xFFEEF5FF), RoundedCornerShape(16.dp))
                        .clickable { termTitle = null }
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** 把 <strong> 渲染成粗体，其余标签剥离（用于进化链等含简单 HTML 的字段） */
@Composable
fun HtmlBoldText(
    html: String,
    fontSize: TextUnit = 13.sp,
    color: Color = Color(0xFF333333),
    lineHeight: TextUnit = fontSize * 1.6,
) {
    val annotated = remember(html) {
        buildAnnotatedString {
            val regex = Regex("<strong>(.*?)</strong>")
            var pos = 0
            for (m in regex.findAll(html)) {
                append(unescapeHtml(html.substring(pos, m.range.first)))
                withStyle(SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                    append(unescapeHtml(m.groupValues[1]))
                }
                pos = m.range.last + 1
            }
            append(unescapeHtml(html.substring(pos)))
        }
    }
    Text(annotated, fontSize = fontSize, color = color, lineHeight = lineHeight)
}

private fun unescapeHtml(s: String): String = s
    .replace(Regex("<[^>]+>"), "")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")

/** 写入系统下载目录（Android 10+ 走 MediaStore，旧系统写应用外部目录） */
fun saveToDownloads(context: Context, name: String, content: String) {
    try {
        val data = content.toByteArray(Charsets.UTF_8)
        val where: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
            }
            val uri: Uri = context.contentResolver
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("无法创建下载文件")
            context.contentResolver.openOutputStream(uri).use { os: OutputStream? ->
                os?.write(data)
            }
            where = "下载/$name"
        } else {
            val file = File(context.getExternalFilesDir(null), name)
            FileOutputStream(file).use { it.write(data) }
            where = file.absolutePath
        }
        Toast.makeText(context, "已保存到 $where", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_LONG).show()
    }
}

fun readTextFromUri(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
} catch (e: Exception) {
    null
}

/** 紧凑搜索输入框（40dp 高） */
@Composable
fun SmallSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
        modifier = modifier
            .height(40.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
        decorationBox = { inner ->
            Box(
                contentAlignment = androidx.compose.ui.Alignment.CenterStart,
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 14.sp, color = Color(0xFF999999), maxLines = 1)
                }
                inner()
            }
        },
    )
}

/** 下拉筛选按钮 */
@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = if (selected.isEmpty()) label else selected,
                fontSize = 13.sp,
                color = if (selected.isEmpty()) TextLight else Primary,
            )
            Text(" ▾", fontSize = 11.sp, color = TextLight)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(label, fontSize = 14.sp) },
                onClick = { onSelect(""); expanded = false },
            )
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontSize = 14.sp) },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}

/** 空状态 */
@Composable
fun EmptyState(emoji: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(text, color = TextLight, fontSize = 14.sp)
    }
}
