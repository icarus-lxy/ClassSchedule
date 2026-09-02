package com.classschedule.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 表单小节标题 */
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = Color(0xFF7A828E),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

/** 数值步进器：- 12 + */
@Composable
fun NumberStepper(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    step: Int = 1,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF6F7FB))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 13.sp,
            color = Color(0xFF3A3F49),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF))
                .clickable { if (value > min) onValueChange((value - step).coerceAtLeast(min)) },
            contentAlignment = Alignment.Center
        ) {
            Text("−", fontSize = 18.sp, color = Primary)
        }
        Text(
            "$value",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp)
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF))
                .clickable { if (value < max) onValueChange((value + step).coerceAtMost(max)) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 18.sp, color = Primary)
        }
    }
}

/** 设置页里的一个条目：左边标题、右边当前值，整行可点 */
@Composable
fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF6F7FB))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 14.sp,
            color = Color(0xFF3A3F49),
            modifier = Modifier.weight(1f)
        )
        Text(value, fontSize = 14.sp, color = Primary, fontWeight = FontWeight.Bold)
    }
}
