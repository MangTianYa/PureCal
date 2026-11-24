package com.dotcalendar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dotcalendar.app.data.ThemeMode
import com.dotcalendar.app.ui.DotCalendarRoot
import com.dotcalendar.app.ui.calendar.CalendarViewModel
import com.dotcalendar.app.ui.theme.DotCalendarTheme
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = application as DotCalendarApplication
            val settings by app.container.settingsRepository.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DotCalendarTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
            ) {
                DotCalendarRoot(
                    viewModel = viewModel<CalendarViewModel>(),
                )
            }
        }
    }
}
