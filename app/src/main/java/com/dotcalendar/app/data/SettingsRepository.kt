package com.dotcalendar.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val weekStartsMonday: Boolean = true,
    val showLunar: Boolean = true,
    val autoUpdateHolidays: Boolean = true,
    val holidaySource: HolidayDataSource = HolidayDataSource.AUTO,
    val juheApiKey: String = "",
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("dot_calendar_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    fun setWeekStartsMonday(enabled: Boolean) = update { it.copy(weekStartsMonday = enabled) }

    fun setShowLunar(enabled: Boolean) = update { it.copy(showLunar = enabled) }

    fun setAutoUpdateHolidays(enabled: Boolean) = update { it.copy(autoUpdateHolidays = enabled) }

    fun setHolidaySource(source: HolidayDataSource) = update { it.copy(holidaySource = source) }

    fun setJuheApiKey(key: String) = update { it.copy(juheApiKey = key.trim()) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putString(KEY_THEME, next.themeMode.name)
            .putBoolean(KEY_DYNAMIC, next.dynamicColor)
            .putBoolean(KEY_WEEK_MONDAY, next.weekStartsMonday)
            .putBoolean(KEY_SHOW_LUNAR, next.showLunar)
            .putBoolean(KEY_AUTO_UPDATE, next.autoUpdateHolidays)
            .putString(KEY_HOLIDAY_SOURCE, next.holidaySource.key)
            .putString(KEY_JUHE_API_KEY, next.juheApiKey)
            .apply()
    }

    private fun load(): AppSettings {
        val mode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)

        return AppSettings(
            themeMode = mode,
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC, true),
            weekStartsMonday = prefs.getBoolean(KEY_WEEK_MONDAY, true),
            showLunar = prefs.getBoolean(KEY_SHOW_LUNAR, true),
            autoUpdateHolidays = prefs.getBoolean(KEY_AUTO_UPDATE, true),
            holidaySource = HolidayDataSource.fromKey(prefs.getString(KEY_HOLIDAY_SOURCE, null)),
            juheApiKey = prefs.getString(KEY_JUHE_API_KEY, "").orEmpty(),
        )
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_WEEK_MONDAY = "week_starts_monday"
        const val KEY_SHOW_LUNAR = "show_lunar"
        const val KEY_AUTO_UPDATE = "auto_update_holidays"
        const val KEY_HOLIDAY_SOURCE = "holiday_source"
        const val KEY_JUHE_API_KEY = "juhe_api_key"
    }
}
