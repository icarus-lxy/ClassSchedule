package com.classschedule.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 本地存储：课程与设置都保存在 SharedPreferences 的 JSON 字符串里。
 * 单用户、纯本地，数据量极小，无需引入数据库。
 */
object Store {
    private const val PREFS_NAME = "class_schedule_store"
    private const val KEY_COURSES = "courses_json"
    private const val KEY_SETTINGS = "settings_json"

    fun loadCourses(context: Context): MutableList<Course> {
        val raw = prefs(context).getString(KEY_COURSES, null) ?: return mutableListOf()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<Course>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Course(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        teacher = o.optString("teacher", ""),
                        location = o.optString("location", ""),
                        dayOfWeek = o.getInt("dayOfWeek"),
                        startPeriod = o.getInt("startPeriod"),
                        endPeriod = o.getInt("endPeriod"),
                        startWeek = o.getInt("startWeek"),
                        endWeek = o.getInt("endWeek"),
                        weekType = o.getInt("weekType"),
                        colorIndex = o.optInt("colorIndex", 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveCourses(context: Context, courses: List<Course>) {
        val array = JSONArray()
        for (c in courses) {
            val o = JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            o.put("teacher", c.teacher)
            o.put("location", c.location)
            o.put("dayOfWeek", c.dayOfWeek)
            o.put("startPeriod", c.startPeriod)
            o.put("endPeriod", c.endPeriod)
            o.put("startWeek", c.startWeek)
            o.put("endWeek", c.endWeek)
            o.put("weekType", c.weekType)
            o.put("colorIndex", c.colorIndex)
            array.put(o)
        }
        prefs(context).edit().putString(KEY_COURSES, array.toString()).apply()
    }

    /** 新增或更新一门课，返回最新课程列表 */
    fun upsertCourse(context: Context, course: Course): MutableList<Course> {
        val list = loadCourses(context)
        val index = list.indexOfFirst { it.id == course.id }
        if (index >= 0) list[index] = course else list.add(course)
        saveCourses(context, list)
        return list
    }

    fun deleteCourse(context: Context, id: Long): MutableList<Course> {
        val list = loadCourses(context)
        list.removeAll { it.id == id }
        saveCourses(context, list)
        return list
    }

    fun clearCourses(context: Context) {
        saveCourses(context, emptyList())
    }

    fun loadSettings(context: Context): AppSettings {
        val raw = prefs(context).getString(KEY_SETTINGS, null) ?: return defaultSettings()
        return try {
            val o = JSONObject(raw)
            AppSettings(
                startEpochDay = o.getLong("startEpochDay"),
                totalWeeks = o.getInt("totalWeeks"),
                periodsPerDay = o.getInt("periodsPerDay"),
                firstStartMinute = o.getInt("firstStartMinute"),
                periodMinutes = o.getInt("periodMinutes"),
                breakMinutes = o.getInt("breakMinutes")
            )
        } catch (_: Exception) {
            defaultSettings()
        }
    }

    fun saveSettings(context: Context, settings: AppSettings) {
        val o = JSONObject()
        o.put("startEpochDay", settings.startEpochDay)
        o.put("totalWeeks", settings.totalWeeks)
        o.put("periodsPerDay", settings.periodsPerDay)
        o.put("firstStartMinute", settings.firstStartMinute)
        o.put("periodMinutes", settings.periodMinutes)
        o.put("breakMinutes", settings.breakMinutes)
        prefs(context).edit().putString(KEY_SETTINGS, o.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

/** 首次启动的默认设置：本周期一为第 1 周周一，之后在设置里改成真实开学日期 */
fun defaultSettings(): AppSettings {
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    return AppSettings(
        startEpochDay = monday.toEpochDay(),
        totalWeeks = 20,
        periodsPerDay = 12,
        firstStartMinute = 8 * 60,
        periodMinutes = 45,
        breakMinutes = 10
    )
}
