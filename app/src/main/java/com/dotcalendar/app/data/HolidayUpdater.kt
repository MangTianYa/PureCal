package com.dotcalendar.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.concurrent.TimeUnit

data class HolidayUpdateStatus(
    val checking: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val lastSuccessSource: HolidayDataSource? = null,
    val lastSuccessAt: Long = 0L,
    val lastCheckedAt: Long = 0L,
    val updatedYears: List<Int> = emptyList(),
)

/**
 * 负责联网拉取节假日数据。启动时最多每 24 小时检查一次；手动检查不受限制。
 * 所有失败都不会清空已有数据，界面继续使用内置或缓存版本。
 */
class HolidayUpdater(
    context: Context,
    private val holidayRepository: HolidayRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val meta: SharedPreferences =
        context.getSharedPreferences("dot_calendar_holiday_update", Context.MODE_PRIVATE)
    private val updateMutex = Mutex()

    private val cachedInfo = holidayRepository.cacheInfo.value
    private val _status = MutableStateFlow(
        HolidayUpdateStatus(
            lastSuccessSource = cachedInfo.source,
            lastSuccessAt = cachedInfo.updatedAt,
            lastCheckedAt = meta.getLong(KEY_LAST_ATTEMPT, 0L),
        ),
    )
    val status: StateFlow<HolidayUpdateStatus> = _status.asStateFlow()

    fun startAutoCheck() {
        scope.launch {
            val settings = settingsRepository.settings.value
            if (!settings.autoUpdateHolidays || !settings.holidaySource.isRemote) return@launch
            if (!isStale()) return@launch
            update(force = false)
        }
    }

    fun checkNow(force: Boolean = true) {
        if (_status.value.checking) return
        scope.launch {
            update(force = force)
        }
    }

    private suspend fun update(force: Boolean) {
        updateMutex.withLock {
            val settings = settingsRepository.settings.value
            val selectedSource = settings.holidaySource
            if (!selectedSource.isRemote) {
                _status.value = _status.value.copy(
                    checking = false,
                    message = "离线模式已开启，不会发起网络请求",
                    isError = false,
                )
                return
            }
            if (!force && !isStale()) return
            if (_status.value.checking) return

            val now = System.currentTimeMillis()
            meta.edit().putLong(KEY_LAST_ATTEMPT, now).apply()
            _status.value = _status.value.copy(
                checking = true,
                message = "正在检查节假日更新…",
                isError = false,
                lastCheckedAt = now,
            )

            val currentYear = LocalDate.now().year
            val nextYear = currentYear + 1
            val candidates = candidateProviders(selectedSource)
            val failures = mutableListOf<String>()

            if (candidates.isEmpty()) {
                _status.value = _status.value.copy(
                    checking = false,
                    message = "当前没有可用的在线数据源",
                    isError = true,
                )
                return
            }

            for (provider in candidates) {
                try {
                    val current = provider.fetchYear(currentYear, settings.juheApiKey)
                    if (current == null || current.days.isEmpty()) {
                        failures += "${provider.source.displayName}：暂无 $currentYear 年数据"
                        continue
                    }

                    val next = try {
                        provider.fetchYear(nextYear, settings.juheApiKey)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Throwable) {
                        null
                    }

                    val incoming = buildMap {
                        put(currentYear, current.days)
                        next?.days?.takeIf { it.isNotEmpty() }?.let { put(nextYear, it) }
                    }
                    val updatedAt = System.currentTimeMillis()
                    holidayRepository.applyRemoteYears(provider.source, incoming, updatedAt)

                    val updatedYears = incoming.keys.sorted()
                    val nextNote = if (next?.days.isNullOrEmpty()) {
                        "$nextYear 年安排尚未公布或暂不可用"
                    } else {
                        "已包含 $nextYear 年安排"
                    }
                    _status.value = HolidayUpdateStatus(
                        checking = false,
                        message = "已通过 ${provider.source.displayName} 更新 " +
                            "${updatedYears.joinToString("、")} 年数据；$nextNote",
                        isError = false,
                        lastSuccessSource = provider.source,
                        lastSuccessAt = updatedAt,
                        lastCheckedAt = now,
                        updatedYears = updatedYears,
                    )
                    return
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    failures += "${provider.source.displayName}：${friendlyError(error)}"
                }
            }

            _status.value = _status.value.copy(
                checking = false,
                message = "更新失败：${failures.joinToString("；")}。已继续使用本地缓存。",
                isError = true,
                lastCheckedAt = now,
            )
        }
    }

    private fun candidateProviders(selected: HolidayDataSource): List<HolidayProvider> {
        val sources = when (selected) {
            HolidayDataSource.AUTO -> {
                val keyConfigured = settingsRepository.settings.value.juheApiKey.isNotBlank()
                buildList {
                    add(HolidayDataSource.HI8)
                    add(HolidayDataSource.TIMOR)
                    if (keyConfigured) add(HolidayDataSource.JUHE)
                }
            }
            HolidayDataSource.OFFLINE -> emptyList()
            else -> listOf(selected)
        }
        return sources.mapNotNull(HolidayProviderFactory::create)
    }

    private fun isStale(): Boolean {
        val lastAttempt = meta.getLong(KEY_LAST_ATTEMPT, 0L)
        return System.currentTimeMillis() - lastAttempt >= CHECK_INTERVAL_MILLIS
    }

    private fun friendlyError(error: Throwable): String {
        val raw = error.message?.trim().orEmpty()
        return when {
            raw.isBlank() -> "网络或数据错误"
            raw.length > 100 -> raw.take(100) + "…"
            else -> raw
        }
    }

    private companion object {
        val CHECK_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(24)
        const val KEY_LAST_ATTEMPT = "last_attempt"
    }
}
