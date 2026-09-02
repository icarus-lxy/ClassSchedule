package com.classschedule.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classschedule.app.data.AppSettings
import com.classschedule.app.data.Course
import com.classschedule.app.data.WEEK_TYPE_ALL
import com.classschedule.app.data.WEEK_TYPE_ODD
import com.classschedule.app.data.showsInWeek
import com.classschedule.app.data.timeLabel
import com.classschedule.app.data.weekOf
import com.classschedule.app.data.weekRangeText
import com.classschedule.app.data.weekdayShort
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 每一节在网格里占的高度 */
private val PeriodHeight: Dp = 56.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    courses: List<Course>,
    settings: AppSettings,
    onAddCourse: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onDeleteCourse: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val todayWeek = remember(settings) { weekOf(today, settings) }
    val pagerState = rememberPagerState(initialPage = todayWeek - 1) { settings.totalWeeks }
    val scope = rememberCoroutineScope()
    val currentWeek = pagerState.currentPage + 1

    var showWeekPicker by remember { mutableStateOf(false) }
    var detailCourse by remember { mutableStateOf<Course?>(null) }
    var deleteTarget by remember { mutableStateOf<Course?>(null) }

    Scaffold(
        containerColor = Color(0xFFF6F7FB),
        topBar = {
            Column(Modifier.fillMaxWidth().background(Color.White)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = Color(0xFF5A6472))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage - 1).coerceAtLeast(0)
                            )
                        }
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一周", tint = TextDark)
                    }
                    Column(
                        modifier = Modifier
                            .clickable { showWeekPicker = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "第${currentWeek}周",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            if (currentWeek == todayWeek) "本周" else "点击选周",
                            fontSize = 10.sp,
                            color = HeaderGray
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                (pagerState.currentPage + 1).coerceAtMost(settings.totalWeeks - 1)
                            )
                        }
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一周", tint = TextDark)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE9EEFF))
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(todayWeek - 1) }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("今", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                HorizontalDivider(color = Color(0xFFF0F1F5), thickness = 1.dp)
                WeekdayHeaderRow(week = currentWeek, settings = settings, today = today)
                HorizontalDivider(color = Color(0xFFF0F1F5), thickness = 1.dp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCourse,
                containerColor = Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
            }
        }
    ) { padding ->
        if (courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "还没有课程\n点击右下角 + 添加",
                    color = HeaderGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) { page ->
                WeekPageContent(
                    week = page + 1,
                    courses = courses,
                    settings = settings,
                    today = today,
                    onCourseClick = { detailCourse = it }
                )
            }
        }
    }

    if (showWeekPicker) {
        WeekPickerDialog(
            totalWeeks = settings.totalWeeks,
            currentWeek = currentWeek,
            todayWeek = todayWeek,
            onSelect = { week ->
                showWeekPicker = false
                scope.launch { pagerState.animateScrollToPage(week - 1) }
            },
            onDismiss = { showWeekPicker = false }
        )
    }

    detailCourse?.let { course ->
        CourseDetailDialog(
            course = course,
            settings = settings,
            onEdit = {
                detailCourse = null
                onEditCourse(course.id)
            },
            onDelete = {
                deleteTarget = course
                detailCourse = null
            },
            onDismiss = { detailCourse = null }
        )
    }

    deleteTarget?.let { course ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除课程") },
            text = { Text("确定删除《${course.name}》吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCourse(course.id)
                    deleteTarget = null
                }) { Text("删除", color = DangerRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消", color = HeaderGray) }
            }
        )
    }
}

/** 顶部周一到周日的表头，附带该周具体日期，今天高亮 */
@Composable
private fun WeekdayHeaderRow(week: Int, settings: AppSettings, today: LocalDate) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(36.dp))
        for (day in 1..7) {
            val date = LocalDate.ofEpochDay(settings.startEpochDay + (week - 1) * 7L + (day - 1))
            val isToday = date == today
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    weekdayShort(day),
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) Primary else Color(0xFF7A828E)
                )
                Text(
                    "${date.monthValue}/${date.dayOfMonth}",
                    fontSize = 10.sp,
                    color = if (isToday) Primary else Color(0xFFB4BAC4)
                )
            }
        }
    }
}

/** 一周的网格：左边节次列 + 7 个星期列，课程块按节次定位 */
@Composable
private fun WeekPageContent(
    week: Int,
    courses: List<Course>,
    settings: AppSettings,
    today: LocalDate,
    onCourseClick: (Course) -> Unit
) {
    val gridHeight = PeriodHeight * settings.periodsPerDay
    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 节次列
        Column(
            modifier = Modifier
                .width(36.dp)
                .height(gridHeight)
        ) {
            for (period in 1..settings.periodsPerDay) {
                Column(
                    modifier = Modifier.height(PeriodHeight),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$period",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7A828E)
                    )
                    Text(
                        timeLabel(settings.periodStartMinute(period)),
                        fontSize = 8.sp,
                        color = Color(0xFFB4BAC4)
                    )
                }
            }
        }
        // 周一到周日七列
        for (day in 1..7) {
            val date = LocalDate.ofEpochDay(settings.startEpochDay + (week - 1) * 7L + (day - 1))
            val isToday = date == today
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(gridHeight)
                    .padding(horizontal = 1.dp)
                    .background(
                        if (isToday) Color(0x113D63DD)
                        else Color(0xFFFFFFFF).copy(alpha = 0.35f)
                    )
            ) {
                courses
                    .filter { it.dayOfWeek == day && it.showsInWeek(week) }
                    .forEach { course ->
                        CourseBlock(
                            course = course,
                            modifier = Modifier
                                .fillMaxWidth()
                                .absoluteOffset(y = PeriodHeight * (course.startPeriod - 1) + 2.dp)
                                .height(PeriodHeight * (course.endPeriod - course.startPeriod + 1) - 4.dp)
                                .padding(horizontal = 2.dp),
                            onClick = { onCourseClick(course) }
                        )
                    }
            }
        }
    }
}

/** 课程色块 */
@Composable
private fun CourseBlock(
    course: Course,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val index = ((course.colorIndex % CoursePalette.size) + CoursePalette.size) % CoursePalette.size
    val (bg, fg) = CoursePalette[index]
    val span = course.endPeriod - course.startPeriod + 1
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 5.dp, vertical = 4.dp)
    ) {
        Column {
            Text(
                course.name,
                color = fg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (span >= 2) 4 else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (course.location.isNotBlank()) {
                Text(
                    "@${course.location}",
                    color = fg.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    maxLines = if (span >= 2) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (span >= 2 && course.teacher.isNotBlank()) {
                Text(
                    course.teacher,
                    color = fg.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (span >= 2 && course.weekType != WEEK_TYPE_ALL) {
                Text(
                    if (course.weekType == WEEK_TYPE_ODD) "单周" else "双周",
                    color = fg.copy(alpha = 0.7f),
                    fontSize = 8.sp
                )
            }
        }
    }
}

/** 周次选择弹窗 */
@Composable
private fun WeekPickerDialog(
    totalWeeks: Int,
    currentWeek: Int,
    todayWeek: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("选择周次") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(300.dp)
            ) {
                items(totalWeeks) { index ->
                    val week = index + 1
                    val isTodayWeek = week == todayWeek
                    val isCurrent = week == currentWeek
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isTodayWeek -> Primary
                                    isCurrent -> Color(0xFFDCE4FF)
                                    else -> Color(0xFFF0F1F5)
                                }
                            )
                            .clickable { onSelect(week) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$week",
                            color = if (isTodayWeek) Color.White else TextDark,
                            fontWeight = if (isTodayWeek || isCurrent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    )
}

/** 课程详情弹窗 */
@Composable
private fun CourseDetailDialog(
    course: Course,
    settings: AppSettings,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(course.name, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                DetailLine(
                    "时间",
                    "${weekdayShort(course.dayOfWeek)} 第${course.startPeriod}-${course.endPeriod}节"
                )
                DetailLine("周次", weekRangeText(course))
                if (course.teacher.isNotBlank()) DetailLine("教师", course.teacher)
                if (course.location.isNotBlank()) DetailLine("地点", course.location)
                DetailLine(
                    "钟点",
                    "${timeLabel(settings.periodStartMinute(course.startPeriod))}-${timeLabel(settings.periodEndMinute(course.endPeriod))}"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) { Text("编辑", color = Primary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("删除", color = DangerRed, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, color = HeaderGray, fontSize = 13.sp, modifier = Modifier.width(42.dp))
        Text(value, color = TextDark, fontSize = 13.sp)
    }
}
