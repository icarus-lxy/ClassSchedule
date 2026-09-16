package com.classschedule.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classschedule.app.data.Course
import com.classschedule.app.data.OcrEngine
import com.classschedule.app.data.OcrLine
import com.classschedule.app.data.TimetableParser
import com.classschedule.app.data.weekRangeText
import com.classschedule.app.data.weekdayShort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 从图片识别课表：选图 → 本地识别 → 逐条核对 → 导入。
 * 识别结果一律先给用户确认，不会直接写进课表。
 */
@Composable
fun ImportScreen(
    totalWeeks: Int,
    periodsPerDay: Int,
    onImport: (List<Course>, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var recognizing by remember { mutableStateOf(false) }
    var lines by remember { mutableStateOf<List<OcrLine>>(emptyList()) }
    var parsed by remember { mutableStateOf<List<Course>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var hasResult by remember { mutableStateOf(false) }
    var showRaw by remember { mutableStateOf(false) }
    var withCoords by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var askReplace by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            recognizing = true
            message = ""
            scope.launch {
                val recognized = withContext(Dispatchers.IO) {
                    try {
                        val bitmap = OcrEngine.loadBitmap(context, uri)
                        if (bitmap == null) null else OcrEngine.recognizeLines(bitmap)
                    } catch (_: Exception) {
                        null
                    }
                }
                recognizing = false
                if (recognized == null) {
                    message = "识别失败：图片读不出来或识别过程出错，换一张再试"
                    return@launch
                }
                lines = recognized
                val outcome = TimetableParser.parse(recognized, totalWeeks, periodsPerDay)
                parsed = outcome.courses
                message = outcome.message
                hasResult = true
                if (outcome.courses.isEmpty()) showRaw = true
            }
        }
    }

    fun pickImage() {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("返回", color = HeaderGray, fontSize = 15.sp) }
            Spacer(Modifier.weight(1f))
            Text("识别导入课表", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(56.dp))
        }
        HorizontalDivider(color = Color(0xFFF0F1F5))

        when {
            recognizing -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(Modifier.height(12.dp))
                        Text("正在识别…", color = HeaderGray, fontSize = 14.sp)
                    }
                }
            }

            !hasResult -> {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("从课表图片导入", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "选一张课表截图或照片（最好是教务系统导出的原图：整张表都在图里、字迹清晰）。\n\n" +
                            "识别完全在手机本地完成，不上传任何数据；识别结果会先列出来给你逐条核对，确认后才写入课表。",
                        fontSize = 13.sp,
                        color = HeaderGray,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { pickImage() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("选择课表图片") }
                    if (message.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(message, fontSize = 13.sp, color = DangerRed)
                    }
                }
            }

            else -> {
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        message,
                        fontSize = 13.sp,
                        color = if (parsed.isEmpty()) DangerRed else Color(0xFF3A3F49)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(parsed) { course ->
                            ImportedCourseRow(
                                course = course,
                                onDelete = { parsed = parsed.filterNot { it.id == course.id } }
                            )
                        }
                    }
                    if (showRaw) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "识别原文（共 ${lines.size} 行）",
                                fontSize = 12.sp,
                                color = HeaderGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text("显示坐标", fontSize = 12.sp, color = HeaderGray)
                            Spacer(Modifier.width(6.dp))
                            Switch(checked = withCoords, onCheckedChange = { withCoords = it })
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF6F7FB))
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                OcrEngine.rawTextOf(lines, withCoords),
                                fontSize = 11.sp,
                                color = Color(0xFF3A3F49)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, OcrEngine.rawTextOf(lines, true))
                                copied = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (copied) "已复制，直接粘贴发我即可" else "复制识别原文（含坐标）",
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRaw = !showRaw },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (showRaw) "隐藏识别原文" else "查看识别原文", fontSize = 13.sp) }
                        OutlinedButton(
                            onClick = { pickImage() },
                            modifier = Modifier.weight(1f)
                        ) { Text("换一张图", fontSize = 13.sp) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (parsed.isNotEmpty()) onImport(parsed, false) },
                            modifier = Modifier.weight(1f),
                            enabled = parsed.isNotEmpty()
                        ) { Text("追加到现有课表", fontSize = 13.sp) }
                        Button(
                            onClick = { if (parsed.isNotEmpty()) askReplace = true },
                            modifier = Modifier.weight(1f),
                            enabled = parsed.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("替换全部", fontSize = 13.sp) }
                    }
                }
            }
        }
    }

    if (askReplace) {
        AlertDialog(
            onDismissRequest = { askReplace = false },
            title = { Text("替换全部课表") },
            text = {
                Text("将用识别出的 ${parsed.size} 门课覆盖当前课表，原有课程会被删除且无法撤销。确定吗？")
            },
            confirmButton = {
                TextButton(onClick = {
                    askReplace = false
                    onImport(parsed, true)
                }) { Text("替换", color = DangerRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { askReplace = false }) { Text("取消", color = HeaderGray) }
            }
        )
    }
}

@Composable
private fun ImportedCourseRow(course: Course, onDelete: () -> Unit) {    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "${weekdayShort(course.dayOfWeek)} 第${course.startPeriod}-${course.endPeriod}节  ${course.name}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            val detail = listOfNotNull(
                course.teacher.takeIf { it.isNotBlank() },
                course.location.takeIf { it.isNotBlank() },
                weekRangeText(course)
            ).joinToString(" · ")
            Text(detail, fontSize = 12.sp, color = HeaderGray)
        }
        TextButton(onClick = onDelete) { Text("删除", color = DangerRed, fontSize = 13.sp) }
    }
}

/** 把识别原文放到剪贴板，方便直接粘给我排查 */
private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("课表识别原文", text))
}
