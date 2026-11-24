package com.dotcalendar.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotcalendar.app.ui.theme.HolidayRedDark
import com.dotcalendar.app.ui.theme.HolidayRedLight
import com.dotcalendar.app.ui.theme.WorkdayBlueDark
import com.dotcalendar.app.ui.theme.WorkdayBlueLight
import com.dotcalendar.app.util.chineseWeekdays
import java.time.LocalDate

@Composable
fun MonthGrid(
    days: List<CalendarDayUi>,
    weekStartsMonday: Boolean,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekLabels = if (weekStartsMonday) {
        chineseWeekdays
    } else {
        listOf("日") + chineseWeekdays.dropLast(1)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            weekLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    week.forEach { day ->
                        DayCell(
                            day = day,
                            onClick = { onDateClick(day.date) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDayUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val holidayColor = if (isDark) HolidayRedDark else HolidayRedLight
    val workdayColor = if (isDark) WorkdayBlueDark else WorkdayBlueLight

    val numberColor = when {
        day.isSelected -> colors.onPrimary
        day.isToday -> colors.onPrimaryContainer
        !day.inMonth -> colors.onSurface.copy(alpha = 0.32f)
        else -> colors.onSurface
    }

    val labelColor = when {
        day.isOffDay -> holidayColor
        day.isMakeupWorkday -> workdayColor
        !day.inMonth -> colors.onSurfaceVariant.copy(alpha = 0.35f)
        else -> colors.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics {
                selected = day.isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 2.dp),
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (day.isSelected) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                    )
                } else if (day.isToday) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                    )
                }

                Text(
                    text = day.dayNumber.toString(),
                    color = numberColor,
                    fontWeight = if (day.isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 16.sp,
                )
            }

            if (day.label.isNotEmpty()) {
                Text(
                    text = day.label,
                    color = labelColor,
                    fontWeight = if (day.isOffDay || day.isMakeupWorkday) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}
