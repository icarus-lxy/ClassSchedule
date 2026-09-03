package com.classschedule.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.classschedule.app.data.Course
import com.classschedule.app.data.Store
import com.classschedule.app.ui.ClassScheduleTheme
import com.classschedule.app.ui.CourseEditScreen
import com.classschedule.app.ui.ScheduleScreen
import com.classschedule.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassScheduleTheme {
                ScheduleApp()
            }
        }
    }
}

/** 三个页面：课表主页 / 课程编辑 / 设置 */
private sealed interface Screen {
    data object Main : Screen
    data class EditCourse(val courseId: Long?) : Screen
    data object Settings : Screen
}

@Composable
private fun ScheduleApp() {
    val context = LocalContext.current
    var courses by remember { mutableStateOf<List<Course>>(Store.loadCourses(context)) }
    var settings by remember { mutableStateOf(Store.loadSettings(context)) }
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }

    when (val currentScreen = screen) {
        Screen.Main -> ScheduleScreen(
            courses = courses,
            settings = settings,
            onAddCourse = { screen = Screen.EditCourse(null) },
            onEditCourse = { id -> screen = Screen.EditCourse(id) },
            onDeleteCourse = { id -> courses = Store.deleteCourse(context, id) },
            onOpenSettings = { screen = Screen.Settings }
        )

        is Screen.EditCourse -> CourseEditScreen(
            courses = courses,
            settings = settings,
            courseId = currentScreen.courseId,
            onSave = { course ->
                courses = Store.upsertCourse(context, course)
                screen = Screen.Main
            },
            onDelete = { id ->
                courses = Store.deleteCourse(context, id)
                screen = Screen.Main
            },
            onCancel = { screen = Screen.Main }
        )

        Screen.Settings -> SettingsScreen(
            settings = settings,
            courseCount = courses.size,
            onSave = { newSettings ->
                settings = newSettings
                Store.saveSettings(context, newSettings)
                screen = Screen.Main
            },
            onClearCourses = {
                Store.clearCourses(context)
                courses = emptyList()
            },
            onBack = { screen = Screen.Main }
        )
    }
}
