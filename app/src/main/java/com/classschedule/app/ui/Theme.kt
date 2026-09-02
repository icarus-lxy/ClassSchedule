package com.classschedule.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 全局常用色 */
val Primary = Color(0xFF3D63DD)
val TextDark = Color(0xFF23262E)
val HeaderGray = Color(0xFF9AA1AC)
val DangerRed = Color(0xFFD64545)

/** 课程色板：色块背景 to 色块文字 */
val CoursePalette: List<Pair<Color, Color>> = listOf(
    Color(0xFFFFF2C6) to Color(0xFF7A5800), // 暖黄
    Color(0xFFD8EAFF) to Color(0xFF1C5D9E), // 天蓝
    Color(0xFFDEF4E0) to Color(0xFF237032), // 薄荷绿
    Color(0xFFFFDEE7) to Color(0xFFA83F58), // 樱花粉
    Color(0xFFEAE1FF) to Color(0xFF5F3F9E), // 薰衣草
    Color(0xFFFFE6D1) to Color(0xFFA85E14), // 蜜橘
    Color(0xFFD6F0EF) to Color(0xFF0E6B6B), // 青碧
    Color(0xFFEFF6D2) to Color(0xFF5B7A0E)  // 青柠
)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    onSurface = TextDark
)

@Composable
fun ClassScheduleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
