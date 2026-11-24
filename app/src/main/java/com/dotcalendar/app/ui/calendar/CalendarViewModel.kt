package com.dotcalendar.app.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dotcalendar.app.DotCalendarApplication
import com.dotcalendar.app.almanac.Almanac
import com.dotcalendar.app.data.AppSettings
import com.dotcalendar.app.data.HolidayDataSource
import com.dotcalendar.app.data.HolidayDay
import com.dotcalendar.app.data.HolidayPeriod
import com.dotcalendar.app.data.HolidayUpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as DotCalendarApplication).container

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings

    val almanac: StateFlow<Almanac> = _selectedDate
        .map { container.almanacRepository.get(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = container.almanacRepository.get(_selectedDate.value),
        )

    val selectedHoliday: StateFlow<HolidayDay?> = combine(
        _selectedDate,
        container.holidayRepository.dataVersion,
    ) { date, _ ->
        container.holidayRepository.holidayOn(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = container.holidayRepository.holidayOn(_selectedDate.value),
    )

    val holidayYears: StateFlow<List<Int>> = container.holidayRepository.years
    val holidayDataVersion: StateFlow<Int> = container.holidayRepository.dataVersion
    val holidayUpdateStatus: StateFlow<HolidayUpdateStatus> = container.holidayUpdater.status

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _visibleMonth.value = YearMonth.from(date)
    }

    fun onMonthChanged(month: YearMonth) {
        _visibleMonth.value = month
    }

    fun periods(year: Int): List<HolidayPeriod> = container.holidayRepository.periods(year)

    fun offDayCount(year: Int): Int = container.holidayRepository.offDayCount(year)

    fun workdayCount(year: Int): Int = container.holidayRepository.workdayCount(year)

    fun nextPeriod(from: LocalDate): HolidayPeriod? = container.holidayRepository.nextPeriod(from)

    fun zodiacOf(month: YearMonth): String =
        container.almanacRepository.zodiacOfYear(month.atDay(15))

    fun setThemeMode(mode: com.dotcalendar.app.data.ThemeMode) =
        container.settingsRepository.setThemeMode(mode)

    fun setDynamicColor(enabled: Boolean) =
        container.settingsRepository.setDynamicColor(enabled)

    fun setWeekStartsMonday(enabled: Boolean) =
        container.settingsRepository.setWeekStartsMonday(enabled)

    fun setShowLunar(enabled: Boolean) =
        container.settingsRepository.setShowLunar(enabled)

    fun setAutoUpdateHolidays(enabled: Boolean) {
        container.settingsRepository.setAutoUpdateHolidays(enabled)
        if (enabled) container.holidayUpdater.checkNow(force = false)
    }

    fun setHolidaySource(source: HolidayDataSource) {
        container.settingsRepository.setHolidaySource(source)
        if (source.isRemote) {
            container.holidayUpdater.checkNow(force = true)
        }
    }

    fun setJuheApiKey(key: String) {
        container.settingsRepository.setJuheApiKey(key)
        if (container.settingsRepository.settings.value.holidaySource == HolidayDataSource.JUHE) {
            container.holidayUpdater.checkNow(force = true)
        }
    }

    fun checkHolidaysNow() = container.holidayUpdater.checkNow(force = true)

    fun calendarDays(
        month: YearMonth,
        selectedDate: LocalDate,
        today: LocalDate,
        showLunar: Boolean,
        weekStartsMonday: Boolean,
    ): List<CalendarDayUi> = buildCalendarDays(
        month = month,
        selectedDate = selectedDate,
        today = today,
        holidays = container.holidayRepository,
        showLunar = showLunar,
        weekStartsMonday = weekStartsMonday,
    )
}
