package com.dotcalendar.app.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotcalendar.app.data.HolidayDataSource
import com.dotcalendar.app.data.ThemeMode
import com.dotcalendar.app.ui.calendar.CalendarViewModel
import com.dotcalendar.app.ui.components.BrandRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val years by viewModel.holidayYears.collectAsStateWithLifecycle()
    val updateStatus by viewModel.holidayUpdateStatus.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    var showSourcePicker by rememberSaveable { mutableStateOf(false) }
    var showJuheKeyEditor by rememberSaveable { mutableStateOf(false) }

    val lastSuccessText = updateStatus.lastSuccessSource?.let { source ->
        if (updateStatus.lastSuccessAt > 0) {
            "${source.displayName} · ${formatUpdateTime(updateStatus.lastSuccessAt)}"
        } else {
            null
        }
    } ?: "尚未联网更新"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            BrandRow(trailing = "设置")
        }

        item {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp)) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "外观 · 日历 · 数据与更新",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        item {
            SettingsCard(
                title = "外观",
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp),
            ) {
                Text(
                    text = "主题模式",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        SelectablePill(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "跟随系统"
                                ThemeMode.LIGHT -> "浅色"
                                ThemeMode.DARK -> "深色"
                            },
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalSettingRow(
                    title = "动态取色",
                    subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "使用 Material You 随壁纸动态取色，关闭后使用 Pixel 经典蓝色主题"
                    } else {
                        "需要 Android 12 及以上系统"
                    },
                    checked = settings.dynamicColor,
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }
        }

        item {
            SettingsCard(
                title = "日历",
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp),
            ) {
                Text(
                    text = "每周起始日",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SelectablePill(
                        text = "周一",
                        selected = settings.weekStartsMonday,
                        onClick = { viewModel.setWeekStartsMonday(true) },
                        modifier = Modifier.weight(1f),
                    )
                    SelectablePill(
                        text = "周日",
                        selected = !settings.weekStartsMonday,
                        onClick = { viewModel.setWeekStartsMonday(false) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalSettingRow(
                    title = "月视图显示农历",
                    subtitle = "在日期下方显示农历、节气与节日",
                    checked = settings.showLunar,
                    onCheckedChange = viewModel::setShowLunar,
                )
            }
        }

        item {
            SettingsCard(
                title = "数据与联网",
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp),
            ) {
                HorizontalSettingRow(
                    title = "联网自动更新",
                    subtitle = if (settings.holidaySource.isRemote) {
                        "启动时检查，最多每 24 小时一次；失败自动回退本地"
                    } else {
                        "离线模式不联网；切换在线数据源后可开启"
                    },
                    checked = settings.holidaySource.isRemote && settings.autoUpdateHolidays,
                    enabled = settings.holidaySource.isRemote,
                    onCheckedChange = viewModel::setAutoUpdateHolidays,
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "API 数据源",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "${settings.holidaySource.displayName} · ${settings.holidaySource.badge}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    ActionPill(
                        text = "切换",
                        onClick = { showSourcePicker = true },
                    )
                }

                if (settings.holidaySource == HolidayDataSource.JUHE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "聚合数据 API Key",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface,
                            )
                            Text(
                                text = if (settings.juheApiKey.isBlank()) {
                                    "未配置，更新会失败；可先申请免费额度"
                                } else {
                                    "已配置 · 仅保存在本机"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (settings.juheApiKey.isBlank()) {
                                    colors.error
                                } else {
                                    colors.onSurfaceVariant
                                },
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        ActionPill(
                            text = if (settings.juheApiKey.isBlank()) "配置" else "修改",
                            onClick = { showJuheKeyEditor = true },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                DataRow(
                    "数据覆盖",
                    if (years.isNotEmpty()) "${years.min()}–${years.max()} 年" else "未加载",
                )
                DataRow("上次成功", lastSuccessText)
                DataRow("农历与黄历", "lunar-java 1.7.7 · 本地计算")
                DataRow("来源备注", "Hi8 / 提莫 / 聚合数据 / holiday-cn")

                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceContainerHigh,
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (updateStatus.checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = if (updateStatus.isError) Icons.Default.CalendarMonth else Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (updateStatus.isError) colors.error else colors.primary,
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (updateStatus.checking) "正在更新中…" else "更新状态",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = updateStatus.message ?: when {
                                !settings.holidaySource.isRemote ->
                                    "离线模式已开启，使用内置与缓存数据。"
                                settings.autoUpdateHolidays ->
                                    "启动时会自动检查，最多每 24 小时一次。"
                                else ->
                                    "自动更新已关闭，可手动检查更新。"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp,
                            color = if (updateStatus.isError) colors.error else colors.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionPill(
                                text = if (updateStatus.checking) "检查中…" else "立即检查更新",
                                onClick = viewModel::checkHolidaysNow,
                                enabled = settings.holidaySource.isRemote && !updateStatus.checking,
                                filled = true,
                                loading = updateStatus.checking,
                            )
                            ActionPill(
                                text = "来源说明",
                                onClick = { showSourcePicker = true },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "节假日每年由国务院办公厅公布。选择页会列出每个 API 的地址、免费额度与使用条件；全部失败时继续使用内置或缓存数据。",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
        }

        item {
            SettingsCard(
                title = "关于",
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = colors.primary,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "日历",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                        Text(
                            text = "Version ${com.dotcalendar.app.BuildConfig.VERSION_NAME} · Material 3 Google Pixel 风格",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showLicenses = true },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceContainerHigh,
                ) {
                    Text(
                        text = "开源许可",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "黄历内容属中国传统民俗文化，仅供参考。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "MATERIAL DESIGN 3 · GOOGLE PIXEL STYLE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            containerColor = colors.surfaceContainerHigh,
            title = {
                Text(
                    text = "开源许可",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = """
                        lunar-java
                        日历、农历、黄历算法
                        MIT License · Copyright 6tail

                        holiday-cn
                        内置中国法定节假日数据
                        MIT License · Copyright NateScarlet

                        Hi8 节假日 API
                        https://hi8.com/t/jiejiari-api
                        免费使用，需署名并链接来源

                        提莫 API
                        https://timor.tech/api/holiday
                        免费公共接口，数据由站长维护

                        聚合数据
                        https://www.juhe.cn/docs/api/id/736
                        商业 API，免费额度以官方套餐为准

                        Jetpack Compose / Material 3
                        Apache License 2.0
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) {
                    Text("关闭")
                }
            },
        )
    }

    if (showSourcePicker) {
        HolidaySourceDialog(
            selected = settings.holidaySource,
            juheKeyConfigured = settings.juheApiKey.isNotBlank(),
            onSelect = { source ->
                showSourcePicker = false
                viewModel.setHolidaySource(source)
                if (source == HolidayDataSource.JUHE && settings.juheApiKey.isBlank()) {
                    showJuheKeyEditor = true
                }
            },
            onDismiss = { showSourcePicker = false },
        )
    }

    if (showJuheKeyEditor) {
        JuheKeyDialog(
            currentKey = settings.juheApiKey,
            onSave = { key ->
                showJuheKeyEditor = false
                viewModel.setJuheApiKey(key)
            },
            onDismiss = { showJuheKeyEditor = false },
        )
    }
}

@Composable
private fun HolidaySourceDialog(
    selected: HolidayDataSource,
    juheKeyConfigured: Boolean,
    onSelect: (HolidayDataSource) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainerHigh,
        title = {
            Column {
                Text(
                    text = "API 数据源",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "所有来源、地址与使用条件",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        },
        text = {
            HolidaySourcePickerContent(
                selected = selected,
                juheKeyConfigured = juheKeyConfigured,
                onSelect = onSelect,
                onOpenDocs = { url -> uriHandler.openUri(url) },
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
    )
}

@Composable
fun HolidaySourcePickerContent(
    selected: HolidayDataSource,
    juheKeyConfigured: Boolean,
    onSelect: (HolidayDataSource) -> Unit,
    onOpenDocs: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HolidayDataSource.entries.forEach { source ->
            HolidaySourceOption(
                source = source,
                selected = selected == source,
                juheKeyConfigured = juheKeyConfigured,
                onClick = { onSelect(source) },
                onOpenDocs = onOpenDocs,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "来源备注",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary,
        )
        Text(
            text = "在线数据均来自第三方接口，最终以国务院办公厅公告为准。" +
                "内置兜底数据来自 holiday-cn（MIT）。聚合数据 Key 仅保存在本机，不随日历数据上传。",
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 14.sp,
            color = colors.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun HolidaySourceOption(
    source: HolidayDataSource,
    selected: Boolean,
    juheKeyConfigured: Boolean,
    onClick: () -> Unit,
    onOpenDocs: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = source.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (selected) colors.primary else colors.surfaceContainerHigh,
                ) {
                    Text(
                        text = source.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选",
                        modifier = Modifier.size(18.dp),
                        tint = colors.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = source.summary,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.9f) else colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "接口 / 来源：${source.endpointLabel}",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant.copy(alpha = 0.8f),
            )
            Text(
                text = source.terms,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = if (selected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant.copy(alpha = 0.8f),
            )
            if (source.requiresApiKey) {
                Text(
                    text = if (juheKeyConfigured) "API Key：已配置" else "API Key：尚未配置",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = if (juheKeyConfigured) colors.onSurfaceVariant else colors.error,
                )
            }
            source.docsUrl?.let { url ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "查看接口文档",
                    modifier = Modifier.clickable { onOpenDocs(url) },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

@Composable
private fun JuheKeyDialog(
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var key by rememberSaveable(currentKey) { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceContainerHigh,
        title = {
            Text(
                text = "聚合数据 API Key",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("接口 Key") },
                    placeholder = { Text("粘贴聚合数据申请的 Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "在聚合数据控制台申请节假日接口后获得。Key 仅保存在本机设置中，不会写入节假日缓存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(key) },
                enabled = key.isNotBlank(),
            ) {
                Text("保存并检查")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun ActionPill(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    filled: Boolean = false,
    loading: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = when {
        !enabled -> colors.surfaceContainerHighest.copy(alpha = 0.5f)
        filled -> colors.primary
        else -> colors.surfaceContainerHigh
    }
    val contentColor = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.45f)
        filled -> colors.onPrimary
        else -> colors.primary
    }

    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = contentColor,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HorizontalSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.45f),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(84.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val updateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日 HH:mm").withZone(ZoneId.systemDefault())

private fun formatUpdateTime(epochMillis: Long): String =
    runCatching { updateTimeFormatter.format(Instant.ofEpochMilli(epochMillis)) }
        .getOrDefault("")
