package com.classschedule.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classschedule.app.data.AppSettings
import com.classschedule.app.data.timeLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    courseCount: Int,
    onSave: (AppSettings) -> Unit,
    onClearCourses: () -> Unit,
    onBack: () -> Unit
) {
    var startEpochDay by remember { mutableStateOf(settings.startEpochDay) }
    var totalWeeks by remember { mutableStateOf(settings.totalWeeks) }
    var periodsPerDay by remember { mutableStateOf(settings.periodsPerDay) }
    var firstHour by remember { mutableStateOf(settings.firstStartMinute / 60) }
    var firstMinute by remember { mutableStateOf(settings.firstStartMinute % 60) }
    var periodMinutes by remember { mutableStateOf(settings.periodMinutes) }
    var breakMinutes by remember { mutableStateOf(settings.breakMinutes) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    fun buildSettings() = AppSettings(
        startEpochDay = startEpochDay,
        totalWeeks = totalWeeks,
        periodsPerDay = periodsPerDay,
        firstStartMinute = firstHour * 60 + firstMinute,
        periodMinutes = periodMinutes,
        breakMinutes = breakMinutes
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("返回", color = HeaderGray, fontSize = 15.sp) }
            Spacer(Modifier.weight(1f))
            Text("设置", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onSave(buildSettings()) }) {
                Text("保存", color = Primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        HorizontalDivider(color = Color(0xFFF0F1F5))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionLabel("学期")
            SettingRow(
                title = "开学日期（第 1 周周一）",
                value = formatDate(startEpochDay)
            ) { showDatePicker = true }
            Spacer(Modifier.height(10.dp))
            NumberStepper("总周数", totalWeeks, 1, 30) { totalWeeks = it }
            Spacer(Modifier.height(10.dp))
            NumberStepper("每天节数", periodsPerDay, 4, 20) { periodsPerDay = it }

            SectionLabel("上课时间")
            SettingRow(
                title = "第一节课开始",
                value = timeLabel(firstHour * 60 + firstMinute)
            ) { showTimePicker = true }
            Spacer(Modifier.height(10.dp))
            NumberStepper("每节课时长（分钟）", periodMinutes, 30, 60, step = 5) { periodMinutes = it }
            Spacer(Modifier.height(10.dp))
            NumberStepper("课间时长（分钟）", breakMinutes, 5, 30, step = 5) { breakMinutes = it }
            Text(
                "按「第一节课开始 + 课长 + 课间」自动推算每节时间。示例：第1节 ${
                    timeLabel(buildSettings().periodStartMinute(1))
                } - ${timeLabel(buildSettings().periodEndMinute(1))}，第2节 ${
                    timeLabel(buildSettings().periodStartMinute(2))
                } - ${timeLabel(buildSettings().periodEndMinute(2))}",
                fontSize = 12.sp,
                color = HeaderGray,
                modifier = Modifier.padding(top = 8.dp)
            )

            SectionLabel("数据")
            SettingRow("清空所有课程", "当前 $courseCount 门") { showClear = true }
            Text(
                "课程数据仅保存在手机本地（无任何网络权限），卸载应用或清除数据会一并删除。",
                fontSize = 12.sp,
                color = HeaderGray,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startEpochDay * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        // 对齐到所在周的周一
                        val monday = picked.minusDays((picked.dayOfWeek.value - 1).toLong())
                        startEpochDay = monday.toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("确定", color = Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消", color = HeaderGray) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = firstHour,
            initialMinute = firstMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("第一节课开始时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    firstHour = timePickerState.hour
                    firstMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("确定", color = Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消", color = HeaderGray) }
            }
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清空课程") },
            text = { Text("将删除全部 $courseCount 门课程，且无法恢复。确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showClear = false
                    onClearCourses()
                }) { Text("全部删除", color = DangerRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("取消", color = HeaderGray) }
            }
        )
    }
}

private fun formatDate(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return "${d.year}年${d.monthValue}月${d.dayOfMonth}日"
}
