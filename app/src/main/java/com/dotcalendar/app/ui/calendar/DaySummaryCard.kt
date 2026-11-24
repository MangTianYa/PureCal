package com.dotcalendar.app.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotcalendar.app.almanac.Almanac
import com.dotcalendar.app.data.HolidayDay
import com.dotcalendar.app.ui.components.DotPill
import com.dotcalendar.app.ui.components.OutlinePill
import com.dotcalendar.app.ui.theme.HolidayRedContainerDark
import com.dotcalendar.app.ui.theme.HolidayRedContainerLight
import com.dotcalendar.app.ui.theme.HolidayRedDark
import com.dotcalendar.app.ui.theme.HolidayRedLight
import com.dotcalendar.app.ui.theme.JiRedContainerDark
import com.dotcalendar.app.ui.theme.JiRedContainerLight
import com.dotcalendar.app.ui.theme.JiRedDark
import com.dotcalendar.app.ui.theme.JiRedLight
import com.dotcalendar.app.ui.theme.WorkdayBlueContainerDark
import com.dotcalendar.app.ui.theme.WorkdayBlueContainerLight
import com.dotcalendar.app.ui.theme.WorkdayBlueDark
import com.dotcalendar.app.ui.theme.WorkdayBlueLight
import com.dotcalendar.app.ui.theme.YiGreenContainerDark
import com.dotcalendar.app.ui.theme.YiGreenContainerLight
import com.dotcalendar.app.ui.theme.YiGreenDark
import com.dotcalendar.app.ui.theme.YiGreenLight
import com.dotcalendar.app.util.chineseWeekday
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaySummaryCard(
    date: LocalDate,
    almanac: Almanac,
    holiday: HolidayDay?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val holidayContainer = if (isDark) HolidayRedContainerDark else HolidayRedContainerLight
    val holidayContent = if (isDark) HolidayRedDark else HolidayRedLight
    val workdayContainer = if (isDark) WorkdayBlueContainerDark else WorkdayBlueContainerLight
    val workdayContent = if (isDark) WorkdayBlueDark else WorkdayBlueLight

    val yiContainer = if (isDark) YiGreenContainerDark else YiGreenContainerLight
    val yiContent = if (isDark) YiGreenDark else YiGreenLight
    val jiContainer = if (isDark) JiRedContainerDark else JiRedContainerLight
    val jiContent = if (isDark) JiRedDark else JiRedLight

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${date.monthValue}月${date.dayOfMonth}日",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = date.chineseWeekday(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )

                if (holiday != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (holiday.isOffDay) {
                        DotPill(
                            text = "休",
                            contentColor = holidayContent,
                            containerColor = holidayContainer,
                        )
                    } else {
                        DotPill(
                            text = "班",
                            contentColor = workdayContent,
                            containerColor = workdayContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "黄历",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "查看完整黄历",
                        modifier = Modifier.padding(top = 1.dp),
                        tint = colors.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = almanac.monthDay,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${almanac.ganZhiYear} · ${almanac.zodiac}年",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${almanac.ganZhiMonth} ${almanac.ganZhiDay} · ${almanac.constellation} · ${almanac.tianShen} · ${almanac.tianShenType}${almanac.tianShenLuck}日",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val tags = buildList {
                if (almanac.jieQi.isNotBlank()) add("节气 ${almanac.jieQi}")
                almanac.festivals.take(2).forEach { add(it) }
                if (holiday != null) add(holiday.name)
            }.distinct()

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tags.forEach { tag ->
                        OutlinePill(text = tag)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            AlmanacBadgeLine(
                label = "宜",
                badgeContainer = yiContainer,
                badgeContent = yiContent,
                values = almanac.yi,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AlmanacBadgeLine(
                label = "忌",
                badgeContainer = jiContainer,
                badgeContent = jiContent,
                values = almanac.ji,
            )
        }
    }
}

@Composable
private fun AlmanacBadgeLine(
    label: String,
    badgeContainer: androidx.compose.ui.graphics.Color,
    badgeContent: androidx.compose.ui.graphics.Color,
    values: List<String>,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DotPill(
            text = label,
            contentColor = badgeContent,
            containerColor = badgeContainer,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = values.take(7).joinToString("  "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
