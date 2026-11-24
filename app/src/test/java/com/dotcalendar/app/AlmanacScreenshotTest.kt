package com.dotcalendar.app

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dotcalendar.app.almanac.Almanac
import com.dotcalendar.app.almanac.AlmanacRepository
import com.dotcalendar.app.ui.almanac.AlmanacContent
import com.dotcalendar.app.ui.theme.DotCalendarTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AlmanacScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private val almanac = AlmanacRepository().get(LocalDate.of(2026, 11, 3))

    @Test
    fun almanacSheet() {
        paparazzi.snapshot {
            AlmanacPreview(almanac = almanac)
        }
    }

    @Composable
    private fun AlmanacPreview(almanac: Almanac) {
        DotCalendarTheme(darkTheme = false, dynamicColor = false) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 32.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.outline),
                            )
                        }
                        AlmanacContent(
                            almanac = almanac,
                            onDismiss = {},
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
