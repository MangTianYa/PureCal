package com.dotcalendar.app

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dotcalendar.app.data.HolidayDataSource
import com.dotcalendar.app.ui.settings.HolidaySourcePickerContent
import com.dotcalendar.app.ui.theme.DotCalendarTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun holidaySourcePicker() {
        paparazzi.snapshot {
            SettingsPreview(darkTheme = false)
        }
    }

    @Test
    fun holidaySourcePickerDark() {
        paparazzi.snapshot {
            SettingsPreview(darkTheme = true)
        }
    }

    @Composable
    private fun SettingsPreview(darkTheme: Boolean) {
        DotCalendarTheme(darkTheme = darkTheme, dynamicColor = false) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "API 数据源",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "所有来源、地址与使用条件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HolidaySourcePickerContent(
                        selected = HolidayDataSource.AUTO,
                        juheKeyConfigured = false,
                        onSelect = {},
                        onOpenDocs = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
