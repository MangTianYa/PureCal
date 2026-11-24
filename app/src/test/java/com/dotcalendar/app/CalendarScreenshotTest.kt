package com.dotcalendar.app

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotcalendar.app.almanac.AlmanacRepository
import com.dotcalendar.app.data.HolidayDay
import com.dotcalendar.app.ui.DotBottomBar
import com.dotcalendar.app.ui.calendar.CalendarDayUi
import com.dotcalendar.app.ui.calendar.DaySummaryCard
import com.dotcalendar.app.ui.calendar.MonthGrid
import com.dotcalendar.app.ui.calendar.MonthHero
import com.dotcalendar.app.ui.components.BrandRow
import com.dotcalendar.app.ui.theme.DotCalendarTheme
import com.nlf.calendar.Solar
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private val almanacRepository = AlmanacRepository()

    @Test
    fun calendarNovember() {
        val month = YearMonth.of(2026, 11)
        val today = LocalDate.of(2026, 11, 3)

        paparazzi.snapshot {
            CalendarPreview(
                month = month,
                days = previewDays(month, today, today, emptyMap()),
                selected = today,
                holiday = null,
            )
        }
    }

    @Test
    fun calendarNovemberDark() {
        val month = YearMonth.of(2026, 11)
        val today = LocalDate.of(2026, 11, 3)

        paparazzi.snapshot {
            CalendarPreview(
                month = month,
                days = previewDays(month, today, today, emptyMap()),
                selected = today,
                holiday = null,
                darkTheme = true,
            )
        }
    }

    @Test
    fun calendarNationalDay() {
        val month = YearMonth.of(2026, 10)
        val today = LocalDate.of(2026, 11, 3)
        val selected = LocalDate.of(2026, 10, 1)
        val holidays = buildMap {
            (1..7).forEach { day ->
                put(LocalDate.of(2026, 10, day), "国庆节" to true)
            }
            put(LocalDate.of(2026, 10, 10), "国庆节" to false)
        }
        val holiday = HolidayDay(name = "国庆节", date = selected, isOffDay = true)

        paparazzi.snapshot {
            CalendarPreview(
                month = month,
                days = previewDays(month, selected, today, holidays),
                selected = selected,
                holiday = holiday,
            )
        }
    }

    @Composable
    private fun CalendarPreview(
        month: YearMonth,
        days: List<CalendarDayUi>,
        selected: LocalDate,
        holiday: HolidayDay?,
        darkTheme: Boolean = false,
    ) {
        val almanac = almanacRepository.get(selected)
        DotCalendarTheme(darkTheme = darkTheme, dynamicColor = false) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    BrandRow(trailing = "日历")
                    MonthHero(
                        month = month,
                        zodiac = almanacRepository.zodiacOfYear(selected),
                        onTodayClick = {},
                    )
                    MonthGrid(
                        days = days,
                        weekStartsMonday = true,
                        onDateClick = {},
                        modifier = Modifier.weight(1f),
                    )
                    DaySummaryCard(
                        date = selected,
                        almanac = almanac,
                        holiday = holiday,
                        onClick = {},
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp),
                    )
                    DotBottomBar(selectedTab = 0, onSelect = {})
                }
            }
        }
    }

    private fun previewDays(
        month: YearMonth,
        selected: LocalDate,
        today: LocalDate,
        holidays: Map<LocalDate, Pair<String, Boolean>>,
    ): List<CalendarDayUi> {
        val first = month.atDay(1)
        val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
        return List(42) { index ->
            val date = start.plusDays(index.toLong())
            val holiday = holidays[date]
            CalendarDayUi(
                date = date,
                inMonth = YearMonth.from(date) == month,
                dayNumber = date.dayOfMonth,
                label = labelFor(date, holiday),
                isOffDay = holiday?.second == true,
                isMakeupWorkday = holiday?.second == false,
                isToday = date == today,
                isSelected = date == selected,
                isWeekend = date.dayOfWeek.value >= 6,
            )
        }
    }

    private fun labelFor(date: LocalDate, holiday: Pair<String, Boolean>?): String {
        if (holiday != null) {
            return if (holiday.second) holiday.first else "班"
        }
        val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunar = solar.lunar
        val festival = lunar.festivals.firstOrNull()
            ?: solar.festivals.firstOrNull()
            ?: lunar.otherFestivals.firstOrNull()
        return when {
            festival != null -> festival
            lunar.jieQi.isNotEmpty() -> lunar.jieQi
            lunar.dayInChinese == "初一" -> lunar.monthInChinese + "月"
            else -> lunar.dayInChinese
        }
    }
}
