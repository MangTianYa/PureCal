package com.dotcalendar.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

data class HolidayDay(
    val name: String,
    val date: LocalDate,
    val isOffDay: Boolean,
)

data class HolidayPeriod(
    val name: String,
    val start: LocalDate,
    val end: LocalDate,
    val offDays: List<LocalDate>,
    val workdays: List<LocalDate>,
) {
    val days: Int get() = offDays.size
    val isSingleDay: Boolean get() = start == end
    val isOngoing: Boolean
        get() = !LocalDate.now().isBefore(start) && !LocalDate.now().isAfter(end)
}

data class HolidayCacheInfo(
    val source: HolidayDataSource? = null,
    val updatedAt: Long = 0L,
    val cachedYears: List<Int> = emptyList(),
)

/**
 * 中国法定节假日与调休数据。
 *
 * 内置 holiday-cn（MIT）数据保证离线可用；联网更新成功后按年份写入本地缓存，
 * 远程年份会覆盖同年的内置数据。所有对外读取都基于不可变快照，更新通过
 * [dataVersion] 通知界面刷新。
 */
class HolidayRepository(context: Context) {
    private val gson = Gson()
    private val cacheFile = File(context.filesDir, "holidays/cache.json")
    private val baselineByYear: Map<Int, List<HolidayDay>> = loadBundledData(context)

    private val _dataVersion = MutableStateFlow(0)
    val dataVersion: StateFlow<Int> = _dataVersion.asStateFlow()

    private val _years = MutableStateFlow<List<Int>>(emptyList())
    val years: StateFlow<List<Int>> = _years.asStateFlow()

    private val _cacheInfo = MutableStateFlow(HolidayCacheInfo())
    val cacheInfo: StateFlow<HolidayCacheInfo> = _cacheInfo.asStateFlow()

    @Volatile
    private var snapshot = Snapshot(emptyList(), emptyMap(), emptyList())

    private var remoteByYear: Map<Int, List<HolidayDay>> = emptyMap()

    init {
        readCache()?.let { cache ->
            remoteByYear = cache.years.mapNotNull { cachedYear ->
                val days = cachedYear.days.mapNotNull { dto ->
                    runCatching {
                        HolidayDay(
                            name = dto.name.trim().ifBlank { "法定节假日" },
                            date = LocalDate.parse(dto.date),
                            isOffDay = dto.isOffDay,
                        )
                    }.getOrNull()
                        ?.takeIf { it.date.year == cachedYear.year }
                }.distinctBy { it.date }.sortedBy { it.date }

                cachedYear.year.takeIf { days.isNotEmpty() }?.let { it to days }
            }.toMap()

            _cacheInfo.value = HolidayCacheInfo(
                source = remoteSourceFromKey(cache.source),
                updatedAt = cache.updatedAt,
                cachedYears = remoteByYear.keys.sortedDescending(),
            )
        }
        publish()
    }

    @Synchronized
    fun applyRemoteYears(
        source: HolidayDataSource,
        incoming: Map<Int, List<HolidayDay>>,
        updatedAt: Long = System.currentTimeMillis(),
    ) {
        if (source == HolidayDataSource.AUTO || !source.isRemote) return

        val normalized = incoming.mapValues { (year, days) ->
            HolidayNormalizer.normalize(
                days.filter { it.date.year == year }
                    .distinctBy { it.date }
                    .sortedBy { it.date },
            )
        }.filterValues { it.isNotEmpty() }

        if (normalized.isEmpty()) return

        remoteByYear = remoteByYear.toMutableMap().apply {
            putAll(normalized)
        }

        val cache = HolidayCacheDto(
            version = CACHE_VERSION,
            source = source.key,
            updatedAt = updatedAt,
            years = remoteByYear.map { (year, days) ->
                HolidayCacheYearDto(
                    year = year,
                    days = days.map { day ->
                        HolidayDayDto(
                            name = day.name,
                            date = day.date.toString(),
                            isOffDay = day.isOffDay,
                        )
                    },
                )
            }.sortedBy { it.year },
        )
        writeCache(cache)

        _cacheInfo.value = HolidayCacheInfo(
            source = source,
            updatedAt = updatedAt,
            cachedYears = remoteByYear.keys.sortedDescending(),
        )
        publish()
    }

    fun holidayOn(date: LocalDate): HolidayDay? = snapshot.byDate[date]

    /** 将同一节日按连续的放假日拆成若干个假期段。 */
    fun periods(year: Int): List<HolidayPeriod> {
        val yearDays = snapshot.allDays.filter { it.date.year == year }
        return yearDays.groupBy { it.name }.flatMap { (name, group) ->
            val offDays = group.filter { it.isOffDay }.map { it.date }.sorted()
            val workdays = group.filterNot { it.isOffDay }.map { it.date }.sorted()
            splitContiguous(offDays).map { run ->
                HolidayPeriod(
                    name = name,
                    start = run.first(),
                    end = run.last(),
                    offDays = run,
                    workdays = workdays,
                )
            }
        }.sortedBy { it.start }
    }

    fun offDayCount(year: Int): Int =
        snapshot.allDays.count { it.date.year == year && it.isOffDay }

    fun workdayCount(year: Int): Int =
        snapshot.allDays.count { it.date.year == year && !it.isOffDay }

    /** 从某天开始的下一个假期（包含正在进行的假期）。 */
    fun nextPeriod(from: LocalDate): HolidayPeriod? {
        val availableYears = snapshot.years
        if (availableYears.isEmpty()) return null
        return (availableYears.first()..availableYears.last())
            .flatMap { periods(it) }
            .filter { !it.end.isBefore(from) }
            .minByOrNull { it.start }
    }

    @Synchronized
    private fun publish() {
        val combined = baselineByYear.toMutableMap()
        remoteByYear.forEach { (year, days) ->
            if (days.isNotEmpty()) combined[year] = days
        }

        val allDays = combined.values
            .flatten()
            .distinctBy { it.date }
            .sortedBy { it.date }

        snapshot = Snapshot(
            allDays = allDays,
            byDate = allDays.associateBy { it.date },
            years = allDays.map { it.date.year }.distinct().sortedDescending(),
        )
        _years.value = snapshot.years
        _dataVersion.value += 1
    }

    private fun loadBundledData(context: Context): Map<Int, List<HolidayDay>> {
        val files = context.assets.list("holidays")
            ?.filter { it.endsWith(".json", ignoreCase = true) }
            ?.sorted()
            .orEmpty()

        return files.flatMap { file ->
            context.assets.open("holidays/$file").bufferedReader(Charsets.UTF_8).use { reader ->
                val dto = gson.fromJson(reader, HolidayYearDto::class.java)
                dto.days.mapNotNull { day ->
                    runCatching {
                        HolidayDay(
                            name = day.name,
                            date = LocalDate.parse(day.date),
                            isOffDay = day.isOffDay,
                        )
                    }.getOrNull()
                }
            }
        }.groupBy { it.date.year }
            .mapValues { (_, days) ->
                HolidayNormalizer.normalize(days.distinctBy { it.date }.sortedBy { it.date })
            }
    }

    private fun readCache(): HolidayCacheDto? {
        if (!cacheFile.isFile) return null
        return runCatching {
            val cache = gson.fromJson(
                cacheFile.readText(Charsets.UTF_8),
                HolidayCacheDto::class.java,
            ) ?: return@runCatching null
            cache.takeIf { it.version == CACHE_VERSION && it.years.isNotEmpty() }
        }.getOrNull()
    }

    private fun writeCache(cache: HolidayCacheDto) {
        runCatching {
            val directory = cacheFile.parentFile ?: return@runCatching
            if (!directory.exists() && !directory.mkdirs()) return@runCatching

            val temporary = File(directory, "cache.json.tmp")
            temporary.writeText(gson.toJson(cache), Charsets.UTF_8)
            try {
                Files.move(
                    temporary.toPath(),
                    cacheFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: Exception) {
                Files.move(
                    temporary.toPath(),
                    cacheFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        }
    }

    private fun remoteSourceFromKey(key: String?): HolidayDataSource? =
        HolidayDataSource.entries.firstOrNull {
            it.key == key && it.isRemote && it != HolidayDataSource.AUTO
        }

    private fun splitContiguous(dates: List<LocalDate>): List<List<LocalDate>> {
        if (dates.isEmpty()) return emptyList()
        val runs = mutableListOf<MutableList<LocalDate>>()
        var current = mutableListOf(dates.first())
        for (index in 1 until dates.size) {
            if (dates[index] == dates[index - 1].plusDays(1)) {
                current += dates[index]
            } else {
                runs += current
                current = mutableListOf(dates[index])
            }
        }
        runs += current
        return runs
    }

    private data class Snapshot(
        val allDays: List<HolidayDay>,
        val byDate: Map<LocalDate, HolidayDay>,
        val years: List<Int>,
    )

    private companion object {
        const val CACHE_VERSION = 1
    }
}

private data class HolidayYearDto(
    val year: Int = 0,
    val days: List<HolidayDayDto> = emptyList(),
)

private data class HolidayDayDto(
    val name: String = "",
    val date: String = "",
    @SerializedName("isOffDay") val isOffDay: Boolean = false,
)

private data class HolidayCacheDto(
    val version: Int = 0,
    val source: String = "",
    val updatedAt: Long = 0L,
    val years: List<HolidayCacheYearDto> = emptyList(),
)

private data class HolidayCacheYearDto(
    val year: Int = 0,
    val days: List<HolidayDayDto> = emptyList(),
)
