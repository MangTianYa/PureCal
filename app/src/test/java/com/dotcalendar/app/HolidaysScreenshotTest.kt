package com.dotcalendar.app

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dotcalendar.app.data.HolidayPeriod
import com.dotcalendar.app.ui.DotBottomBar
import com.dotcalendar.app.ui.holidays.HolidaysContent
import com.dotcalendar.app.ui.theme.DotCalendarTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HolidaysScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private val today = LocalDate.of(2026, 11, 3)

    @Test
    fun holidays2026() {
        paparazzi.snapshot {
            HolidaysPreview(today = today, nextPeriod = null)
        }
    }

    @Test
    fun holidaysUpcoming() {
        paparazzi.snapshot {
            HolidaysPreview(
                today = LocalDate.of(2026, 9, 15),
                nextPeriod = period("中秋节", "2026-09-25", "2026-09-27", "2026-09-20"),
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun HolidaysPreview(today: LocalDate, nextPeriod: HolidayPeriod?) {
        DotCalendarTheme(darkTheme = false, dynamicColor = false) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        HolidaysContent(
                            today = today,
                            years = listOf(2026, 2025, 2024),
                            selectedYear = 2026,
                            periods = periods2026,
                            offDayCount = 33,
                            workdayCount = 6,
                            nextPeriod = nextPeriod,
                            onSelectYear = {},
                        )
                    }
                    DotBottomBar(selectedTab = 1, onSelect = {})
                }
            }
        }
    }

    private val periods2026 = listOf(
        period("元旦", "2026-01-01", "2026-01-03", "2026-01-04"),
        period("春节", "2026-02-15", "2026-02-23", "2026-02-14", "2026-02-28"),
        period("清明节", "2026-04-04", "2026-04-06"),
        period("劳动节", "2026-05-01", "2026-05-05", "2026-05-09"),
        period("端午节", "2026-06-19", "2026-06-21"),
        period("中秋节", "2026-09-25", "2026-09-27", "2026-09-20"),
        period("国庆节", "2026-10-01", "2026-10-07", "2026-10-10"),
    )

    private fun period(
        name: String,
        start: String,
        end: String,
        vararg workdays: String,
    ): HolidayPeriod {
        val startDate = LocalDate.parse(start)
        val endDate = LocalDate.parse(end)
        val offDays = generateSequence(startDate) { date ->
            date.plusDays(1).takeIf { !it.isAfter(endDate) }
        }.toList()
        return HolidayPeriod(
            name = name,
            start = startDate,
            end = endDate,
            offDays = offDays,
            workdays = workdays.map(LocalDate::parse),
        )
    }
}
