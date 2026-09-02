package com.classschedule.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 周重复类型 */
const val WEEK_TYPE_ALL = 0
const val WEEK_TYPE_ODD = 1   // 单周（第1、3、5…周）
const val WEEK_TYPE_EVEN = 2  // 双周（第2、4、6…周）

data class Course(
    val id: Long,
    val name: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: Int,    // 1=周一 … 7=周日
    val startPeriod: Int,  // 开始节次，从 1 开始
    val endPeriod: Int,    // 结束节次
    val startWeek: Int,    // 开始周次，从 1 开始
    val endWeek: Int,      // 结束周次
    val weekType: Int,     // WEEK_TYPE_*
    val colorIndex: Int    // 课程色板索引
)

data class AppSettings(
    val startEpochDay: Long,    // 第 1 周周一的日期（epochDay）
    val totalWeeks: Int,        // 学期总周数
    val periodsPerDay: Int,     // 每天节数
    val firstStartMinute: Int,  // 第一节课开始时间（分钟数，8:00 = 480）
    val periodMinutes: Int,     // 每节课时长（分钟）
    val breakMinutes: Int       // 课间时长（分钟）
) {
    fun periodStartMinute(period: Int): Int =
        firstStartMinute + (period - 1) * (periodMinutes + breakMinutes)

    fun periodEndMinute(period: Int): Int = periodStartMinute(period) + periodMinutes
}

/** 这门课在某一周是否上 */
fun Course.showsInWeek(week: Int): Boolean {
    if (week < startWeek || week > endWeek) return false
    return when (weekType) {
        WEEK_TYPE_ODD -> week % 2 == 1
        WEEK_TYPE_EVEN -> week % 2 == 0
        else -> true
    }
}

/** 计算某日期是学期第几周（越界时收敛到边界） */
fun weekOf(date: LocalDate, settings: AppSettings): Int {
    val start = LocalDate.ofEpochDay(settings.startEpochDay)
    val days = ChronoUnit.DAYS.between(start, date)
    val week = (days / 7 + 1).toInt()
    return week.coerceIn(1, settings.totalWeeks)
}

/** 找出与 candidate 时间冲突的课程：同一天、节次重叠、周次有交集 */
fun findConflicts(courses: List<Course>, candidate: Course): List<Course> =
    courses.filter { other ->
        other.id != candidate.id &&
            other.dayOfWeek == candidate.dayOfWeek &&
            other.startPeriod <= candidate.endPeriod &&
            candidate.startPeriod <= other.endPeriod &&
            weeksOverlap(other, candidate)
    }

private fun weeksOverlap(a: Course, b: Course): Boolean {
    val lo = maxOf(a.startWeek, b.startWeek)
    val hi = minOf(a.endWeek, b.endWeek)
    if (lo > hi) return false
    val parity = when {
        a.weekType == WEEK_TYPE_ALL && b.weekType == WEEK_TYPE_ALL -> return true
        a.weekType == WEEK_TYPE_ALL -> b.weekType
        b.weekType == WEEK_TYPE_ALL -> a.weekType
        a.weekType != b.weekType -> return false
        else -> a.weekType
    }
    val needOdd = parity == WEEK_TYPE_ODD
    val first = if ((lo % 2 == 1) == needOdd) lo else lo + 1
    return first <= hi
}

fun weekdayShort(day: Int): String =
    if (day in 1..7) "周" + "一二三四五六日"[day - 1] else ""

fun weekRangeText(course: Course): String {
    val range = if (course.startWeek == course.endWeek) {
        "第${course.startWeek}周"
    } else {
        "第${course.startWeek}-${course.endWeek}周"
    }
    return range + when (course.weekType) {
        WEEK_TYPE_ODD -> "（单周）"
        WEEK_TYPE_EVEN -> "（双周）"
        else -> ""
    }
}

fun timeLabel(minute: Int): String = "%d:%02d".format(minute / 60, minute % 60)

fun nextCourseId(courses: List<Course>): Long = (courses.maxOfOrNull { it.id } ?: 0L) + 1L
