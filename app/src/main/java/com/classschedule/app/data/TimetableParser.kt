package com.classschedule.app.data

/**
 * 把 OCR 出来的文字行还原成课表。
 *
 * 版式差异很大，尽量"能拿到什么就用什么"：
 *  1. 表头「星期几」→ 列边界。允许缺列（只有周一到周六也行），
 *     缺的列按已找到表头的列宽外推补上。
 *  2. 行结构：标签齐全连续时直接用标签位置切行（最准）；
 *     标签缺失/认不出时，改按内容的纵向间隔切行，再用认到的标签给行命名，
 *     剩下的行按"每行几节"续推。
 *  3. 同一格里按纵向间距聚类 → 拆出叠放的多门课（虚线分隔行会自然留在两门课之间）。
 *  4. 相邻行里重复出现的同一门课 → 合并成一门连堂课。
 */
object TimetableParser {

    private val dayRegex = Regex("(?:星期|周)([一二三四五六日天])")
    private val rowLabelRegex = Regex("^\\s*([0-9]{1,4})\\s*节\\s*$")
    private val weekRangeRegex = Regex("(\\d{1,2})\\s*[-~～—－]\\s*(\\d{1,2})\\s*周")
    private val singleWeekRegex = Regex("(\\d{1,2})\\s*周")
    private val roomRegex = Regex("校区\\s*[：:]?\\s*(.{1,16}?)室")
    private val hoursRegex = Regex("(\\d{1,2})\\s*学时")
    private val dashOnlyRegex = Regex("^[-—–_─=·・.。~～\\s]{2,}$")

    /** 课程名与教师之间可能出现的分隔符 */
    private val separators = listOf("：", ":", "；", ";")

    /** 与 ui 里的 CoursePalette 数量保持一致 */
    private const val COLOR_COUNT = 8

    private data class Column(val day: Int, val range: IntRange)

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

    fun parse(lines: List<OcrLine>, totalWeeks: Int, defaultPeriodsPerDay: Int): Outcome {
        val content = lines.filter { it.text.isNotBlank() }
        if (content.isEmpty()) {
            return Outcome(emptyList(), "没有识别到任何文字，换一张更清晰的图试试")
        }

        // 1) 表头「星期几」：取每列最靠上的那次出现，允许缺列
        val headerForDay = LinkedHashMap<Int, OcrLine>()
        for (line in content.sortedBy { it.top }) {
            for (match in dayRegex.findAll(line.text)) {
                val day = dayOfWeek(match.groupValues[1])
                if (day in 1..7 && !headerForDay.containsKey(day)) headerForDay[day] = line
            }
        }
        if (headerForDay.size < 2) {
            val sample = content.take(8).joinToString(" / ") { it.text }
            return Outcome(emptyList(), "没找到「星期几」表头（至少要有 2 列）。识别到的开头几行：$sample")
        }

        val ordered = headerForDay.entries.sortedBy { it.value.centerX }
        val anchorDay = ordered.first().key
        val anchorX = ordered.first().value.centerX
        var colWidth = 120
        if (ordered.size >= 2) {
            val last = ordered.last()
            val daySpan = (last.key - anchorDay).coerceAtLeast(1)
            colWidth = ((last.value.centerX - anchorX) / daySpan).coerceAtLeast(20)
        }
        // 用锚点 + 列宽把 7 天的列中心补齐（缺列也补，超出的自然成为空列）
        val centers = HashMap<Int, Int>()
        for (day in 1..7) centers[day] = anchorX + (day - anchorDay) * colWidth

        val headerBottom = ordered.maxOf { it.value.bottom }

        // 2) 左侧节次标签（可能缺失、可能认不全）
        val firstColumnLeft = (centers[1] ?: anchorX) - colWidth / 2
        val labels = content
            .filter { rowLabelRegex.matches(it.text) && it.centerX < firstColumnLeft }
            .sortedBy { it.centerY }

        val rowPeriods: List<IntRange>
        val rowBounds: List<IntRange>
        var note = ""

        val strict = if (labels.size >= 2) {
            periodsOfLabels(labels.map { rowLabelRegex.find(it.text)!!.groupValues[1] })
        } else {
            null
        }

        if (strict != null) {
            // 标签齐全且连续：按标签位置切行，最准
            val bounds = ArrayList<IntRange>()
            for (i in labels.indices) {
                val top = if (i == 0) labels[0].top - labels[0].boxHeight * 4
                else (labels[i - 1].centerY + labels[i].centerY) / 2
                val bottom = if (i == labels.lastIndex) content.maxOf { it.bottom }
                else (labels[i].centerY + labels[i + 1].centerY) / 2
                if (bottom > top) bounds.add(top..bottom)
            }
            if (bounds.size != labels.size) {
                return Outcome(emptyList(), "行高计算异常，请把识别原文发我")
            }
            rowPeriods = strict
            rowBounds = bounds
        } else {
            // 标签缺失或认不出的情况：靠内容间隔切行
            val bands = splitRowsByGaps(content, firstColumnLeft, headerBottom)
            if (bands.size < 2) {
                val labelTexts = if (labels.isEmpty()) "（一个都没认到）"
                else labels.take(8).joinToString(" / ") { it.text }
                return Outcome(
                    emptyList(),
                    "认不出这张表的行结构。识别到的节次标签：$labelTexts"
                )
            }
            // 认到的标签贴到对应行上，没标签的行按"每行几节"续推
            val known = HashMap<Int, IntRange>()
            for (line in labels) {
                val digits = rowLabelRegex.find(line.text)?.groupValues?.get(1) ?: continue
                val range = rangeOfSingleLabel(digits) ?: continue
                val index = bands.indexOfFirst { line.centerY in it }
                if (index >= 0) known[index] = range
            }
            val perRow = known.values
                .map { it.last - it.first + 1 }
                .groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key ?: 2
            val built = ArrayList<IntRange>()
            var nextStart = known[0]?.first ?: 1
            for (i in bands.indices) {
                val range = known[i] ?: (nextStart..(nextStart + perRow - 1))
                built.add(range)
                nextStart = range.last + 1
            }
            rowPeriods = built
            rowBounds = bands
            val labelTexts = if (labels.isEmpty()) "一个都没认到" else labels.joinToString(" / ") { it.text }
            note = "（节次标签不完整：$labelTexts；我按提示的行位置推断节次，请重点核对）"
        }

        // 3) 每一列的左右边界
        val tableLeft = if (labels.isNotEmpty()) labels.maxOf { it.right } else firstColumnLeft
        val tableRight = content.maxOf { it.right }
        val columns = ArrayList<Column>()
        for (day in 1..7) {
            val center = centers[day] ?: continue
            val prev = centers[day - 1]
            val next = centers[day + 1]
            val left = when {
                day == 1 -> tableLeft
                prev != null -> (prev + center) / 2
                else -> center - colWidth / 2
            }
            val right = when {
                day == 7 -> tableRight
                next != null -> (center + next) / 2
                else -> center + colWidth / 2
            }
            if (right > left) columns.add(Column(day, left..right))
        }

        val firstRowTop = rowBounds.first().first
        val lastRowBottom = rowBounds.last().last

        // 4) 文字行落格子
        val cells = HashMap<String, MutableList<OcrLine>>()
        for (line in content) {
            if (rowLabelRegex.matches(line.text)) continue
            if (line.centerX <= tableLeft) continue
            if (line.centerY < firstRowTop || line.centerY > lastRowBottom) continue
            val column = columns.firstOrNull { line.centerX in it.range } ?: continue
            val row = rowBounds.indexOfFirst { line.centerY in it }
            if (row < 0) continue
            cells.getOrPut("$row|${column.day}") { mutableListOf() }.add(line)
        }

        // 5) 同一格内按纵向间距聚类成一门课
        val raw = mutableListOf<RawCourse>()
        for ((key, group) in cells) {
            val parts = key.split("|")
            val row = parts[0].toInt()
            val day = parts[1].toInt()
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

        // 6) 相邻行里重复出现的同一门课合并成连堂课
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

        // 7) 转成课程数据；标注了"连堂N学时"时按学时补足跨节数
        val periodsPerDay = rowPeriods.last().last
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
            "识别到文字，但没能解析出课程。请点「复制识别原文」把原文发我"
        } else {
            "识别到 ${courses.size} 门课$note，请逐条核对后再导入"
        }
        return Outcome(courses, message)
    }

    /**
     * 没有可用标签时，按内容的纵向间隔切分出行。
     * 虚线分隔行（格子内叠放多门课的分隔）保留在流里，避免把同一行的两门课切成两行。
     */
    private fun splitRowsByGaps(
        content: List<OcrLine>,
        firstColumnLeft: Int,
        headerBottom: Int
    ): List<IntRange> {
        val body = content
            .filter { it.centerX > firstColumnLeft && it.centerY > headerBottom }
            .sortedBy { it.top }
        if (body.isEmpty()) return emptyList()

        val heights = body.map { it.boxHeight }.sorted()
        val median = heights[heights.size / 2].coerceAtLeast(6)
        val gapThreshold = median * 5 / 2

        val bands = mutableListOf<IntRange>()
        var top = body.first().top
        var bottom = body.first().bottom
        for (i in 1 until body.size) {
            val line = body[i]
            if (line.top - bottom > gapThreshold) {
                bands.add(top..bottom)
                top = line.top
                bottom = line.bottom
            } else {
                if (line.top < top) top = line.top
                if (line.bottom > bottom) bottom = line.bottom
            }
        }
        bands.add(top..bottom)
        return bands
    }

    /** 从一格里的一堆文字行拼出一门课 */
    private fun buildRaw(
        day: Int,
        periods: IntRange,
        group: List<OcrLine>,
        totalWeeks: Int
    ): RawCourse? {
        val blob = group.joinToString("") { it.text }
        val head = splitHead(blob)
        val name = cleanName(head.first) ?: return null
        val teacher = head.second
            .removeSuffix("雅安").removeSuffix("成都").removeSuffix("都江堰")
            .trim()

        val roomMatch = roomRegex.find(blob)
        val room = if (roomMatch != null) roomMatch.groupValues[1].trim() + "室" else ""

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

    /** 切出「课程名 / 教师」两部分。分隔符可能是冒号，也可能是分号 */
    private fun splitHead(blob: String): Pair<String, String> {
        val head = blob.substringBefore("校区", blob)
        var index = -1
        for (separator in separators) {
            val at = head.indexOf(separator)
            if (at >= 0 && (index < 0 || at < index)) index = at
        }
        return if (index < 0) {
            head.trim() to ""
        } else {
            head.substring(0, index).trim() to head.substring(index + 1).trim()
        }
    }

    private fun cleanName(raw: String): String? {
        val name = raw.replace("《", "").replace("》", "")
            .removeSuffix("(实验)").removeSuffix("（实验）")
            .trim()
        if (name.length < 2) return null
        if (name.all { it.isDigit() || it == '-' || it == ' ' }) return null
        return name
    }

    /**
     * 标签齐全时：判断它到底是"一行一节"（1节/2节/3节…）还是"一行两节"（12节=第1-2节）。
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

    /** 单个标签 → 节次范围："12节"→1-2，"910节"→9-10，"3节"→3-3 */
    private fun rangeOfSingleLabel(digits: String): IntRange? =
        pairOfLabel(digits) ?: digits.toIntOrNull()?.let { it..it }

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
