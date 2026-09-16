package com.classschedule.app.data

import java.time.LocalDate

/**
 * 内置课表：2026学年秋季 学生课表（学号 2402090804 李宇轩，土木2412，沈阳建筑大学）
 * 来源：教务系统导出的课表文件（大节课表，一个大节占两节）。
 */
object SeedData {

    fun settings(): AppSettings = AppSettings(
        startEpochDay = LocalDate.of(2026, 9, 7).toEpochDay(), // 占位，可在设置里改成真实开学日期
        totalWeeks = 18,
        periodsPerDay = 12,
        firstStartMinute = 8 * 60,
        periodMinutes = 45,
        breakMinutes = 15,
        customStartMinutes = listOf(
            8 * 60 + 0,  // 第 1 节 08:00
            9 * 60 + 0,  // 第 2 节 09:00
            10 * 60 + 0,  // 第 3 节 10:00
            10 * 60 + 55,  // 第 4 节 10:55
            13 * 60 + 30,  // 第 5 节 13:30
            14 * 60 + 30,  // 第 6 节 14:30
            15 * 60 + 30,  // 第 7 节 15:30
            16 * 60 + 30,  // 第 8 节 16:30
            18 * 60 + 30,  // 第 9 节 18:30
            19 * 60 + 30,  // 第 10 节 19:30
            20 * 60 + 25,  // 第 11 节 20:25
            21 * 60 + 20  // 第 12 节 21:20
        )
    )

    fun courses(): List<Course> = listOf(
        // 周一 土木工程施工技术 第3-4节 周次 4-15
        c(1, 1, 3, 4, "土木工程施工技术", "赵亮", "丙2201", 4, 15, WEEK_TYPE_ALL, 0),
        // 周一 习近平新时代中国特色社会主义思想概论 第5-6节 周次 6-8
        c(2, 1, 5, 6, "习近平新时代中国特色社会主义思想概论", "赵诗雨", "C1203", 6, 8, WEEK_TYPE_ALL, 4),
        // 周一 大学生就业指导与职业规划 第9-10节 周次 9-12
        c(3, 1, 9, 10, "大学生就业指导与职业规划", "刘裕巍", "B1401", 9, 12, WEEK_TYPE_ALL, 7),
        // 周二 土力学与基础工程 第3-4节 周次 4-17
        c(4, 2, 3, 4, "土力学与基础工程", "徐岩", "丙2201", 4, 17, WEEK_TYPE_ALL, 1),
        // 周二 土木工程施工技术 第5-6节 周次 4-15
        c(5, 2, 5, 6, "土木工程施工技术", "赵亮", "丙2201", 4, 15, WEEK_TYPE_ALL, 0),
        // 周二 习近平新时代中国特色社会主义思想概论 第7-8节 周次 9-14
        c(6, 2, 7, 8, "习近平新时代中国特色社会主义思想概论", "赵诗雨", "B1403", 9, 14, WEEK_TYPE_ALL, 4),
        // 周三 专业外语 第3-4节 周次 4-11
        c(7, 3, 3, 4, "专业外语", "李帼昌", "丙2201", 4, 11, WEEK_TYPE_ALL, 2),
        // 周三 工程力学5 第5-6节 周次 10-10
        c(8, 3, 5, 6, "工程力学5", "孙威", "B4-207电测实验室", 10, 10, WEEK_TYPE_ALL, 3),
        // 周三 工程力学5 第5-6节 周次 12-12
        c(9, 3, 5, 6, "工程力学5", "孙威", "B4-207电测实验室", 12, 12, WEEK_TYPE_ALL, 3),
        // 周四 土力学与基础工程 第3-4节 周次 4-17
        c(10, 4, 3, 4, "土力学与基础工程", "徐岩", "丙2201", 4, 17, WEEK_TYPE_ALL, 1),
        // 周四 合同管理 第5-6节 周次 4-11
        c(11, 4, 5, 6, "合同管理", "王东", "乙2401", 4, 11, WEEK_TYPE_ALL, 5),
        // 周四 文献检索 第7-8节 周次 1-8
        c(12, 4, 7, 8, "文献检索", "曹丽娟", "甲1401", 1, 8, WEEK_TYPE_ALL, 6),
        // 周五 工程力学5 第3-4节 周次 4-13
        c(13, 5, 3, 4, "工程力学5", "王宇", "丙2201", 4, 13, WEEK_TYPE_ALL, 3),
        // 周五 土力学与基础工程 第5-6节 周次 3-3
        c(14, 5, 5, 6, "土力学与基础工程", "田金丰", "B4-203岩土工程第一实验室", 3, 3, WEEK_TYPE_ALL, 1),
        // 周五 土力学与基础工程 第5-6节 周次 6-6
        c(15, 5, 5, 6, "土力学与基础工程", "田金丰", "B4-101岩土工程第二实验室", 6, 6, WEEK_TYPE_ALL, 1),
        // 周五 土力学与基础工程 第5-6节 周次 7-7
        c(16, 5, 5, 6, "土力学与基础工程", "田金丰", "B4-101岩土工程第二实验室", 7, 7, WEEK_TYPE_ALL, 1),
        // 周五 土力学与基础工程 第5-6节 周次 8-8
        c(17, 5, 5, 6, "土力学与基础工程", "田金丰", "B4-101岩土工程第二实验室", 8, 8, WEEK_TYPE_ALL, 1),
        // 周五 习近平新时代中国特色社会主义思想概论 第7-8节 周次 4-16
        c(18, 5, 7, 8, "习近平新时代中国特色社会主义思想概论", "赵诗雨", "B1403", 4, 16, WEEK_TYPE_ALL, 4),
    )

    private fun c(
        id: Long,
        day: Int,
        startPeriod: Int,
        endPeriod: Int,
        name: String,
        teacher: String,
        location: String,
        startWeek: Int,
        endWeek: Int,
        weekType: Int,
        colorIndex: Int
    ) = Course(
        id = id,
        name = name,
        teacher = teacher,
        location = location,
        dayOfWeek = day,
        startPeriod = startPeriod,
        endPeriod = endPeriod,
        startWeek = startWeek,
        endWeek = endWeek,
        weekType = weekType,
        colorIndex = colorIndex
    )
}
