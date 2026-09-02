package com.classschedule.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classschedule.app.data.AppSettings
import com.classschedule.app.data.Course
import com.classschedule.app.data.WEEK_TYPE_ALL
import com.classschedule.app.data.WEEK_TYPE_EVEN
import com.classschedule.app.data.WEEK_TYPE_ODD
import com.classschedule.app.data.findConflicts
import com.classschedule.app.data.nextCourseId
import com.classschedule.app.data.weekRangeText
import com.classschedule.app.data.weekdayShort

@Composable
fun CourseEditScreen(
    courses: List<Course>,
    settings: AppSettings,
    courseId: Long?,
    onSave: (Course) -> Unit,
    onDelete: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val existing = courses.firstOrNull { it.id == courseId }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var teacher by remember { mutableStateOf(existing?.teacher ?: "") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var day by remember { mutableStateOf(existing?.dayOfWeek ?: 1) }
    var startPeriod by remember { mutableStateOf(existing?.startPeriod ?: 1) }
    var endPeriod by remember { mutableStateOf(existing?.endPeriod ?: 2) }
    var startWeek by remember { mutableStateOf(existing?.startWeek ?: 1) }
    var endWeek by remember { mutableStateOf(existing?.endWeek ?: settings.totalWeeks) }
    var weekType by remember { mutableStateOf(existing?.weekType ?: WEEK_TYPE_ALL) }
    var colorIndex by remember { mutableStateOf(existing?.colorIndex ?: 0) }
    var colorPicked by remember { mutableStateOf(existing != null) }
    var conflictList by remember { mutableStateOf<List<Course>>(emptyList()) }
    var showConflict by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onCancel)

    fun buildCourse(): Course = Course(
        id = existing?.id ?: nextCourseId(courses),
        name = name.trim(),
        teacher = teacher.trim(),
        location = location.trim(),
        dayOfWeek = day,
        startPeriod = startPeriod,
        endPeriod = maxOf(startPeriod, endPeriod),
        startWeek = minOf(startWeek, endWeek),
        endWeek = maxOf(startWeek, endWeek),
        weekType = weekType,
        colorIndex = if (colorPicked) colorIndex else autoColorIndex(name.trim())
    )

    fun trySave() {
        val course = buildCourse()
        val conflicts = findConflicts(courses, course)
        if (conflicts.isEmpty()) {
            onSave(course)
        } else {
            conflictList = conflicts
            showConflict = true
        }
    }

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
            TextButton(onClick = onCancel) { Text("取消", color = HeaderGray, fontSize = 15.sp) }
            Spacer(Modifier.weight(1f))
            Text(
                if (existing == null) "添加课程" else "编辑课程",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { trySave() }, enabled = name.isNotBlank()) {
                Text("保存", color = Primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        HorizontalDivider(color = Color(0xFFF0F1F5))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SectionLabel("课程名 *")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("如：高等数学") },
                singleLine = true
            )

            SectionLabel("教师 / 教室")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("教师（选填）") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("教室（选填）") },
                    singleLine = true
                )
            }

            SectionLabel("星期")
            WeekdaySelector(selected = day) { day = it }

            SectionLabel("节次（每天共 ${settings.periodsPerDay} 节）")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberStepper(
                    title = "开始节",
                    value = startPeriod,
                    min = 1,
                    max = settings.periodsPerDay,
                    modifier = Modifier.weight(1f)
                ) { value ->
                    startPeriod = value
                    if (endPeriod < value) endPeriod = value
                }
                NumberStepper(
                    title = "结束节",
                    value = endPeriod,
                    min = startPeriod,
                    max = settings.periodsPerDay,
                    modifier = Modifier.weight(1f)
                ) { value -> endPeriod = value }
            }

            SectionLabel("周次（本学期共 ${settings.totalWeeks} 周）")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberStepper(
                    title = "开始周",
                    value = startWeek,
                    min = 1,
                    max = settings.totalWeeks,
                    modifier = Modifier.weight(1f)
                ) { value ->
                    startWeek = value
                    if (endWeek < value) endWeek = value
                }
                NumberStepper(
                    title = "结束周",
                    value = endWeek,
                    min = startWeek,
                    max = settings.totalWeeks,
                    modifier = Modifier.weight(1f)
                ) { value -> endWeek = value }
            }
            Text(
                "同一门课分布在多个时间段（如 1-8 周和 11-16 周）时，分别添加多条即可。",
                fontSize = 12.sp,
                color = HeaderGray,
                modifier = Modifier.padding(top = 6.dp)
            )

            SectionLabel("重复方式")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WeekTypeOption("每周", weekType == WEEK_TYPE_ALL, Modifier.weight(1f)) { weekType = WEEK_TYPE_ALL }
                WeekTypeOption("单周", weekType == WEEK_TYPE_ODD, Modifier.weight(1f)) { weekType = WEEK_TYPE_ODD }
                WeekTypeOption("双周", weekType == WEEK_TYPE_EVEN, Modifier.weight(1f)) { weekType = WEEK_TYPE_EVEN }
            }

            SectionLabel("颜色（默认按课程名自动分配）")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CoursePalette.forEachIndexed { index, (bg, _) ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(bg)
                            .border(
                                width = if (index == colorIndex && colorPicked) 3.dp else 0.dp,
                                color = Primary,
                                shape = CircleShape
                            )
                            .clickable {
                                colorIndex = index
                                colorPicked = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == colorIndex && colorPicked) {
                            Text("✓", color = TextDark, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (existing != null) {
                Spacer(Modifier.height(28.dp))
                OutlinedButton(
                    onClick = { showDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                ) {
                    Text("删除这门课")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showConflict) {
        AlertDialog(
            onDismissRequest = { showConflict = false },
            title = { Text("时间冲突") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("与以下课程时间重叠：", fontSize = 14.sp)
                    conflictList.forEach { c ->
                        Text(
                            "《${c.name}》 ${weekdayShort(c.dayOfWeek)} 第${c.startPeriod}-${c.endPeriod}节 ${weekRangeText(c)}",
                            fontSize = 13.sp,
                            color = DangerRed
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConflict = false
                    onSave(buildCourse())
                }) { Text("仍要保存", color = DangerRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConflict = false }) { Text("返回修改", color = HeaderGray) }
            }
        )
    }

    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("删除课程") },
            text = { Text("确定删除《${existing.name}》吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete(existing.id)
                }) { Text("删除", color = DangerRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消", color = HeaderGray) }
            }
        )
    }
}

/** 按课程名稳定地分配一个色板索引 */
private fun autoColorIndex(name: String): Int =
    if (name.isEmpty()) 0
    else ((name.hashCode() % CoursePalette.size) + CoursePalette.size) % CoursePalette.size

@Composable
private fun WeekdaySelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (d in 1..7) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (d == selected) Primary else Color(0xFFF0F1F5))
                    .clickable { onSelect(d) }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    weekdayShort(d),
                    fontSize = 12.sp,
                    fontWeight = if (d == selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (d == selected) Color.White else Color(0xFF5A6472)
                )
            }
        }
    }
}

@Composable
private fun WeekTypeOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Primary else Color(0xFFF0F1F5))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF5A6472)
        )
    }
}
