package com.dotcalendar.app.ui.holidays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotcalendar.app.data.HolidayPeriod
import com.dotcalendar.app.ui.calendar.CalendarViewModel
import com.dotcalendar.app.ui.components.BrandRow
import com.dotcalendar.app.ui.components.DotPill
import com.dotcalendar.app.ui.components.OutlinePill
import com.dotcalendar.app.ui.theme.HolidayRedContainerDark
import com.dotcalendar.app.ui.theme.HolidayRedContainerLight
import com.dotcalendar.app.ui.theme.HolidayRedDark
import com.dotcalendar.app.ui.theme.HolidayRedLight
import com.dotcalendar.app.ui.theme.WorkdayBlueContainerDark
import com.dotcalendar.app.ui.theme.WorkdayBlueContainerLight
import com.dotcalendar.app.ui.theme.WorkdayBlueDark
import com.dotcalendar.app.ui.theme.WorkdayBlueLight
import com.dotcalendar.app.util.chineseMonthDay
import com.dotcalendar.app.util.chineseWeekday
import com.dotcalendar.app.util.dateRangeText
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HolidaysScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier,
) {
    val today by produceState(initialValue = LocalDate.now()) {
        while (true) {
            value = LocalDate.now()
            delay(60_000)
        }
    }

    val availableYears by viewModel.holidayYears.collectAsStateWithLifecycle()
    val dataVersion by viewModel.holidayDataVersion.collectAsStateWithLifecycle()
    val updateStatus by viewModel.holidayUpdateStatus.collectAsStateWithLifecycle()
    val years = availableYears.ifEmpty { listOf(today.year) }
    var selectedYear by rememberSaveable {
        mutableIntStateOf(if (today.year in years) today.year else years.first())
    }

    LaunchedEffect(years) {
        if (selectedYear !in years) {
            selectedYear = if (today.year in years) today.year else years.first()
        }
    }

    HolidaysContent(
        today = today,
        years = years,
        selectedYear = selectedYear,
        periods = remember(selectedYear, dataVersion) { viewModel.periods(selectedYear) },
        offDayCount = remember(selectedYear, dataVersion) { viewModel.offDayCount(selectedYear) },
        workdayCount = remember(selectedYear, dataVersion) { viewModel.workdayCount(selectedYear) },
        nextPeriod = remember(today, dataVersion) { viewModel.nextPeriod(today) },
        onSelectYear = { selectedYear = it },
        dataSourceLabel = updateStatus.lastSuccessSource?.displayName ?: "内置 holiday-cn",
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HolidaysContent(
    today: LocalDate,
    years: List<Int>,
    selectedYear: Int,
    periods: List<HolidayPeriod>,
    offDayCount: Int,
    workdayCount: Int,
    nextPeriod: HolidayPeriod?,
    onSelectYear: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dataSourceLabel: String = "内置 holiday-cn",
) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            BrandRow(trailing = "节假日")
        }

        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)) {
                Text(
                    text = "节假日与调休",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "法定放假安排 · 调休补班 · 国务院公告",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                years.forEach { year ->
                    YearPill(
                        year = year,
                        selected = year == selectedYear,
                        onClick = { onSelectYear(year) },
                    )
                }
            }
        }

        item {
            NextHolidayCard(
                period = nextPeriod,
                today = today,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    value = offDayCount.toString(),
                    label = "天休假",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = workdayCount.toString(),
                    label = "天补班",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "全年安排",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$selectedYear · ${periods.size} 个假期",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        items(periods, key = { "${it.name}-${it.start}" }) { period ->
            HolidayPeriodCard(
                period = period,
                today = today,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp),
            )
        }

        item {
            Text(
                text = "数据覆盖 ${years.minOrNull()}–${years.maxOrNull()} 年 · 上次来源 $dataSourceLabel\n可在「设置 → API 数据源」切换；法定节假日与调休以国务院办公厅公告为准。",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 16.sp,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
internal fun YearPill(
    year: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
    ) {
        Text(
            text = "${year}年",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun NextHolidayCard(
    period: HolidayPeriod?,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Celebration,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = colors.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "下一个假期",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (period == null) {
                Text(
                    text = "待公布",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "下一年度放假安排以国务院公告为准",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.7f),
                )
                return@Column
            }

            val ongoing = !today.isBefore(period.start) && !today.isAfter(period.end)
            val daysUntil = ChronoUnit.DAYS.between(today, period.start).coerceAtLeast(0)
            val daysLeft = ChronoUnit.DAYS.between(today, period.end).coerceAtLeast(0)

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (ongoing) "进行中" else daysUntil.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (!ongoing) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "天后",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = period.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (ongoing) {
                    "假期中 · 还剩 $daysLeft 天 · ${dateRangeText(period.start, period.end)}"
                } else {
                    "${dateRangeText(period.start, period.end)} · 共 ${period.days} 天"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
internal fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HolidayPeriodCard(
    period: HolidayPeriod,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val ongoing = !today.isBefore(period.start) && !today.isAfter(period.end)

    val holidayContainer = if (isDark) HolidayRedContainerDark else HolidayRedContainerLight
    val holidayContent = if (isDark) HolidayRedDark else HolidayRedLight
    val workdayContainer = if (isDark) WorkdayBlueContainerDark else WorkdayBlueContainerLight
    val workdayContent = if (isDark) WorkdayBlueDark else WorkdayBlueLight

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = period.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                if (ongoing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    DotPill(
                        text = "今天",
                        contentColor = colors.onPrimary,
                        containerColor = colors.primary,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                DotPill(
                    text = "${period.days} 天",
                    contentColor = holidayContent,
                    containerColor = holidayContainer,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = dateRangeText(period.start, period.end),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = "${period.start.chineseWeekday()} — ${period.end.chineseWeekday()}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
            )

            if (period.workdays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "调休补班",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    period.workdays.forEach { day ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = workdayContainer,
                        ) {
                            Text(
                                text = "${day.chineseMonthDay()} ${day.chineseWeekday()} · 班",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = workdayContent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
