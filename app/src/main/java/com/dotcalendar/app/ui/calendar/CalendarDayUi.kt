package com.dotcalendar.app.ui.calendar

import com.dotcalendar.app.data.HolidayDay
import com.dotcalendar.app.data.HolidayRepository
import com.nlf.calendar.Solar
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap

data class CalendarDayUi(
    val date: LocalDate,
    val inMonth: Boolean,
    val dayNumber: Int,
    val label: String,
    val isOffDay: Boolean,
    val isMakeupWorkday: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isWeekend: Boolean,
)

private data class MonthCacheKey(
    val month: YearMonth,
    val showLunar: Boolean,
    val weekStartsMonday: Boolean,
    val holidayDataVersion: Int,
)

private data class CalendarDayTemplate(
    val date: LocalDate,
    val inMonth: Boolean,
    val dayNumber: Int,
    val label: String,
    val isOffDay: Boolean,
    val isMakeupWorkday: Boolean,
    val isWeekend: Boolean,
)

private val monthTemplateCache = ConcurrentHashMap<MonthCacheKey, List<CalendarDayTemplate>>()

fun buildCalendarDays(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    holidays: HolidayRepository,
    showLunar: Boolean,
    weekStartsMonday: Boolean,
): List<CalendarDayUi> {
    val key = MonthCacheKey(
        month = month,
        showLunar = showLunar,
        weekStartsMonday = weekStartsMonday,
        holidayDataVersion = holidays.dataVersion.value,
    )

    val templates = monthTemplateCache.computeIfAbsent(key) {
        val first = month.atDay(1)
        val leading = if (weekStartsMonday) {
            first.dayOfWeek.value - 1
        } else {
            first.dayOfWeek.value % 7
        }
        val start = first.minusDays(leading.toLong())

        List(42) { index ->
            val date = start.plusDays(index.toLong())
            val holiday = holidays.holidayOn(date)
            CalendarDayTemplate(
                date = date,
                inMonth = YearMonth.from(date) == month,
                dayNumber = date.dayOfMonth,
                label = labelFor(date, holiday, showLunar),
                isOffDay = holiday?.isOffDay == true,
                isMakeupWorkday = holiday?.isOffDay == false,
                isWeekend = date.dayOfWeek.value >= 6,
            )
        }
    }

    return templates.map { template ->
        CalendarDayUi(
            date = template.date,
            inMonth = template.inMonth,
            dayNumber = template.dayNumber,
            label = template.label,
            isOffDay = template.isOffDay,
            isMakeupWorkday = template.isMakeupWorkday,
            isToday = template.date == today,
            isSelected = template.date == selectedDate,
            isWeekend = template.isWeekend,
        )
    }
}

private fun labelFor(date: LocalDate, holiday: HolidayDay?, showLunar: Boolean): String {
    if (holiday != null) {
        return if (holiday.isOffDay) holiday.name else "班"
    }

    val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
    val lunar = solar.lunar

    val festival = lunar.festivals.firstOrNull()
        ?: solar.festivals.firstOrNull()
        ?: lunar.otherFestivals.firstOrNull()

    return when {
        festival != null -> festival
        lunar.jieQi.isNotEmpty() -> lunar.jieQi
        !showLunar -> ""
        lunar.dayInChinese == "初一" -> lunar.monthInChinese + "月"
        else -> lunar.dayInChinese
    }
}
