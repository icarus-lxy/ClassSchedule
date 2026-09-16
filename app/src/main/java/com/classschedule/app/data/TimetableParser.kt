package com.classschedule.app.data

/**
 * 把 OCR 出来的文字行还原成课表。
 *
 * 思路（针对"表格线 + 一个格子可能叠放多门不同周次的课"这种教务系统导出版式）：
 *  1. 表头「星期一…星期日」的横坐标 → 7 个列边界
 *  2. 左侧「12节 / 34节 …」标签的纵坐标 → 每一行覆盖的节次
 *  3. 每个文字行按坐标落进"星期 × 行"的格子
 *  4. 同一格里按纵向间距聚类 → 拆出叠放的多门课
 *  5. 相邻行里重复出现的同一门课 → 合并成一门连堂课
 */
object TimetableParser {

    private val dayRegex = Regex("(?:星期|周)([一二三四五六日天])")
    private val rowLabelRegex = Regex("^\\s*([0-9]{1,4})\\s*节\\s*$")
    private val weekRangeRegex = Regex("(\\d{1,2})\\s*[-~～—－]\\s*(\\d{1,2})\\s*周")
    private val singleWeekRegex = Regex("(\\d{1,2})\\s*周")
    private val roomRegex = Regex("校区[^0-9A-Za-z]*([0-9A-Za-z\\-]{1,12})室")
    private val hoursRegex = Regex("(\\d{1,2})\\s*学时")
    private val dashOnlyRegex = Regex("^[-—–_─=·・.。~～\\s]{2,}$")

    // 与 ui 里的 CoursePalette 数量保持一致
    private const val COLOR_COUNT = 8

    private data class RawCourse(
        val day: Int,
        val periods: IntRange,
        val name: String,
        val teacher: String,
        val room: String,
        val startWeek: Int,
        val endWeek: Int,
        val weekType: Int,
        val hours: Int
    )

    data class Outcome(val courses: List<Course>, val message: String)

    fun parse(lines: List<OcrLine>, totalWeeks: Int): Outcome {
        val content = lines.filter { it.text.isNotBlank() }
        if (content.isEmpty()) {
            return Outcome(emptyList(), "没有识别到任何文字，换一张更清晰的图试试")
        }

        // 1) 表头：找到星期一…星期日的横坐标（取最靠上的那一次出现）
        val dayCenters = IntArray(8) { -1 }
        for (line in content.sortedBy { it.top }) {
            for (match in dayRegex.findAll(line.text)) {
                val day = dayOfWeek(match.groupValues[1])
                if (day in 1..7 && dayCenters[day] < 0) dayCenters[day] = line.centerX
            }
        }
        if ((1..7).any { dayCenters[it] < 0 }) {
            return Outcome(emptyList(), "没找到「星期一…星期日」表头，请确认图片是完整课表")
        }

        // 2) 左侧节次标签（如 12节 / 34节 / 56节）
        val labels = content
            .filter { rowLabelRegex.matches(it.text) && it.centerX < dayCenters[1] - 10 }
            .sortedBy { it.centerY }
        if (labels.size < 2) {
            return Outcome(emptyList(), "没找到左侧的节次标签（如 12节 / 34节），请把识别原文发我")
        }
        val rowPeriods = periodsOfLabels(
            labels.map { rowLabelRegex.find(it.text)!!.groupValues[1] }
        ) ?: return Outcome(emptyList(), "节次标签解析失败，请把识别原文发我")
        val periodsPerDay = rowPeriods.last().last

        // 3) 每一行的上下边界：取相邻标签中点
        val rowBounds = ArrayList<IntRange>()
        for (i in labels.indices) {
            val top = if (i == 0) labels[0].top - labels[0].boxHeight * 4
            else (labels[i - 1].centerY + labels[i].centerY) / 2
            val bottom = if (i == labels.lastIndex) content.maxOf { it.bottom }
            else (labels[i].centerY + labels[i + 1].centerY) / 2
            if (bottom > top) rowBounds.add(top..bottom)
        }
        if (rowBounds.size != labels.size) {
            return Outcome(emptyList(), "行高计算异常，请把识别原文发我")
        }

        // 4) 每一列的左右边界
        val tableLeft = labels.maxOf { it.right }
        val tableRight = content.maxOf { it.right }
        val colBounds = ArrayList<IntRange>()
        for (d in 1..7) {
            val left = if (d == 1) tableLeft else (dayCenters[d - 1] + dayCenters[d]) / 2
            val right = if (d == 7) tableRight else (dayCenters[d] + dayCenters[d + 1]) / 2
            colBounds.add(left..right)
        }
        val firstRowTop = rowBounds.first().first
        val lastRowBottom = rowBounds.last().last

        // 5) 文字行落格子
        val cells = HashMap<String, MutableList<OcrLine>>()
        for (line in content) {
            if (rowLabelRegex.matches(line.text)) continue
            if (line.centerX <= tableLeft) continue
            if (line.centerY < firstRowTop || line.centerY > lastRowBottom) continue
            val day = colBounds.indexOfFirst { line.centerX in it }
            if (day < 0) continue
            val row = rowBounds.indexOfFirst { line.centerY in it }
            if (row < 0) continue
            cells.getOrPut("$day|$row") { mutableListOf() }.add(line)
        }

        // 6) 同一格内按纵向间距聚类成一门课
        val raw = mutableListOf<RawCourse>()
        for ((key, group) in cells) {
            val parts = key.split("|")
            val day = parts[0].toInt() + 1
            val row = parts[1].toInt()
            val sorted = group.sortedBy { it.centerY }
            var bucket = mutableListOf<OcrLine>()
            var lastBottom = Int.MIN_VALUE

            fun flush() {
                if (bucket.isNotEmpty()) {
                    buildRaw(day, rowPeriods[row], bucket, totalWeeks)?.let { raw.add(it) }
                    bucket = mutableListOf()
                }
            }

            for (line in sorted) {
                if (dashOnlyRegex.matches(line.text)) {
                    flush()
                    lastBottom = Int.MIN_VALUE
                    continue
                }
                if (bucket.isNotEmpty()) {
                    val avgHeight = bucket.sumOf { it.boxHeight }.toDouble() / bucket.size
                    val gap = line.top - lastBottom
                    if (gap > avgHeight * 1.4) flush()
                }
                bucket.add(line)
                lastBottom = line.bottom
            }
            flush()
        }

        // 7) 相邻行里重复出现的同一门课合并成连堂课
        val merged = mutableListOf<RawCourse>()
        for (course in raw.sortedWith(compareBy({ it.day }, { it.periods.first }, { it.name }))) {
            val last = merged.lastOrNull()
            val sameCourse = last != null &&
                last.day == course.day &&
                last.name == course.name &&
                last.teacher == course.teacher &&
                course.periods.first <= last.periods.last + 1
            if (sameCourse && last != null) {
                merged[merged.lastIndex] = last.copy(
                    periods = last.periods.first..maxOf(last.periods.last, course.periods.last)
                )
            } else {
                merged.add(course)
            }
        }

        // 8) 转成课程数据；标注了"连堂N学时"时按学时补足跨节数
        val courses = merged.mapIndexed { index, rc ->
            val span = rc.periods.last - rc.periods.first + 1
            val end = if (rc.hours > span) rc.periods.first + rc.hours - 1 else rc.periods.last
            Course(
                id = index + 1L,
                name = rc.name,
                teacher = rc.teacher,
                location = rc.room,
                dayOfWeek = rc.day,
                startPeriod = rc.periods.first,
                endPeriod = end.coerceIn(rc.periods.first, periodsPerDay),
                startWeek = rc.startWeek,
                endWeek = rc.endWeek,
                weekType = rc.weekType,
                colorIndex = colorFor(rc.name)
            )
        }.sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod }))

        val message = if (courses.isEmpty()) {
            "识别到文字，但没能解析出课程。请把下面的识别原文发我，我按实际识别结果调规则"
        } else {
            "识别到 ${courses.size} 门课，请逐条核对后再导入"
        }
        return Outcome(courses, message)
    }

    /** 从一格里的一堆文字行拼出一门课 */
    private fun buildRaw(
        day: Int,
        periods: IntRange,
        group: List<OcrLine>,
        totalWeeks: Int
    ): RawCourse? {
        val blob = group.joinToString("") { it.text }
        val name = extractName(blob) ?: return null
        val teacher = extractTeacher(blob)

        val roomMatch = roomRegex.find(blob)
        val room = if (roomMatch != null) roomMatch.groupValues[1] + "室" else ""

        var startWeek = 1
        var endWeek = totalWeeks
        val range = weekRangeRegex.find(blob)
        if (range != null) {
            startWeek = range.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
            endWeek = range.groupValues[2].toIntOrNull()?.coerceIn(startWeek, 40) ?: totalWeeks
        } else {
            val single = singleWeekRegex.find(blob)?.groupValues?.get(1)?.toIntOrNull()
            if (single != null) {
                startWeek = single
                endWeek = single
            }
        }

        val weekType = when {
            blob.contains("单周") -> WEEK_TYPE_ODD
            blob.contains("双周") -> WEEK_TYPE_EVEN
            else -> WEEK_TYPE_ALL
        }
        val hours = hoursRegex.find(blob)?.groupValues?.get(1)?.toIntOrNull()
            ?: (periods.last - periods.first + 1)

        return RawCourse(day, periods, name, teacher, room, startWeek, endWeek, weekType, hours)
    }

    private fun extractName(blob: String): String? {
        val head = blob.substringBefore("校区", blob)
        val raw = head.substringBefore("：").substringBefore(":").trim()
        val name = raw.replace("《", "").replace("》", "")
            .removeSuffix("(实验)").removeSuffix("（实验）")
            .trim()
        if (name.length < 2) return null
        if (name.all { it.isDigit() || it == '-' || it == ' ' }) return null
        return name
    }

    private fun extractTeacher(blob: String): String {
        val head = blob.substringBefore("校区", blob)
        val after = head.substringAfter("：", "").ifEmpty { head.substringAfter(":", "") }
        return after.trim()
            .removeSuffix("雅安").removeSuffix("成都").removeSuffix("都江堰")
            .trim()
    }

    /**
     * 解析左侧行标签。教务系统里一行可能覆盖两节，标签写成 "12节"（第1-2节）、"910节"（第9-10节）；
     * 也可能一行就是一节，标签写成 "1节""2节"。两种都试，用"是否构成连续节次"来判断哪种成立。
     */
    private fun periodsOfLabels(labels: List<String>): List<IntRange>? {
        val singles = labels.mapNotNull { it.toIntOrNull() }
        if (singles.size == labels.size &&
            singles.zipWithNext().all { (a, b) -> b > a } &&
            singles.first() == 1 && singles.last() <= 24
        ) {
            return singles.map { it..it }
        }
        val pairs = labels.mapNotNull { pairOfLabel(it) }
        if (pairs.size == labels.size &&
            pairs.first().first == 1 &&
            pairs.zipWithNext().all { (a, b) -> b.first == a.last + 1 }
        ) {
            return pairs
        }
        return null
    }

    private fun pairOfLabel(digits: String): IntRange? {
        return when (digits.length) {
            2 -> {
                val a = digits[0] - '0'
                val b = digits[1] - '0'
                if (a >= 1 && b == a + 1) a..b else null
            }
            3 -> {
                val a = digits.substring(0, 1).toIntOrNull() ?: return null
                val b = digits.substring(1, 3).toIntOrNull() ?: return null
                if (b == a + 1) a..b else null
            }
            4 -> {
                val a = digits.substring(0, 2).toIntOrNull() ?: return null
                val b = digits.substring(2, 4).toIntOrNull() ?: return null
                if (b == a + 1) a..b else null
            }
            else -> null
        }
    }

    private fun dayOfWeek(char: String): Int = when (char) {
        "一" -> 1
        "二" -> 2
        "三" -> 3
        "四" -> 4
        "五" -> 5
        "六" -> 6
        "日", "天" -> 7
        else -> 0
    }

    private fun colorFor(name: String): Int {
        val hash = name.hashCode() % COLOR_COUNT
        return (hash + COLOR_COUNT) % COLOR_COUNT
    }
}
