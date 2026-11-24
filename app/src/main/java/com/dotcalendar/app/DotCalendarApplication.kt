package com.dotcalendar.app

import android.app.Application
import android.content.Context
import com.dotcalendar.app.almanac.AlmanacRepository
import com.dotcalendar.app.data.HolidayRepository
import com.dotcalendar.app.data.HolidayUpdater
import com.dotcalendar.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DotCalendarApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.holidayUpdater.startAutoCheck()
    }
}

class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val holidayRepository = HolidayRepository(context)
    val almanacRepository = AlmanacRepository()
    val settingsRepository = SettingsRepository(context)
    val holidayUpdater = HolidayUpdater(
        context = context,
        holidayRepository = holidayRepository,
        settingsRepository = settingsRepository,
        scope = applicationScope,
    )
}
