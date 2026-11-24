package com.dotcalendar.app.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

internal data class RemoteHolidayYear(
    val year: Int,
    val days: List<HolidayDay>,
)

internal class HolidayApiException(message: String) : Exception(message)

internal interface HolidayProvider {
    val source: HolidayDataSource
    suspend fun fetchYear(year: Int, apiKey: String?): RemoteHolidayYear?
}

internal object HolidayProviderFactory {
    private val httpClient = HolidayHttpClient()

    fun create(source: HolidayDataSource): HolidayProvider? = when (source) {
        HolidayDataSource.HI8 -> Hi8Provider(httpClient)
        HolidayDataSource.TIMOR -> TimorProvider(httpClient)
        HolidayDataSource.JUHE -> JuheProvider(httpClient)
        HolidayDataSource.AUTO, HolidayDataSource.OFFLINE -> null
    }
}

private class Hi8Provider(
    private val httpClient: HolidayHttpClient,
) : HolidayProvider {
    override val source = HolidayDataSource.HI8

    override suspend fun fetchYear(year: Int, apiKey: String?): RemoteHolidayYear? {
        val json = httpClient.getJson("https://hi8.com/api/holiday/$year.json")
        return HolidayJsonParser.parseHi8(json, year)
    }
}

private class TimorProvider(
    private val httpClient: HolidayHttpClient,
) : HolidayProvider {
    override val source = HolidayDataSource.TIMOR

    override suspend fun fetchYear(year: Int, apiKey: String?): RemoteHolidayYear? {
        val json = httpClient.getJson("https://timor.tech/api/holiday/year/$year/?type=Y")
        return HolidayJsonParser.parseTimor(json, year)
    }
}

private class JuheProvider(
    private val httpClient: HolidayHttpClient,
) : HolidayProvider {
    override val source = HolidayDataSource.JUHE

    override suspend fun fetchYear(year: Int, apiKey: String?): RemoteHolidayYear? {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) {
            throw HolidayApiException("未配置聚合数据 API Key")
        }
        val encodedKey = URLEncoder.encode(key, Charsets.UTF_8.name())
        val json = httpClient.getJson(
            "https://apis.juhe.cn/fapigx/jiejiari/query" +
                "?key=$encodedKey&date=$year&type=1&mode=0",
        )
        return HolidayJsonParser.parseJuhe(json, year)
    }
}

internal class HolidayHttpClient {
    suspend fun getJson(url: String): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 15_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "DotCalendar/1.0 (Android)")
        }

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val buffer = CharArray(8_192)
                val output = StringBuilder()
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    output.append(buffer, 0, read)
                    if (output.length > MAX_RESPONSE_CHARS) {
                        throw HolidayApiException("接口返回内容过大")
                    }
                }
                output.toString()
            }.orEmpty()

            if (code !in 200..299) {
                throw HolidayApiException("HTTP $code")
            }
            if (body.isBlank()) {
                throw HolidayApiException("接口返回为空")
            }
            body
        } catch (error: CancellationException) {
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_RESPONSE_CHARS = 512 * 1024
    }
}

internal object HolidayJsonParser {
    private val gson = Gson()

    fun parseHi8(json: String, requestedYear: Int): RemoteHolidayYear? {
        val response = gson.fromJson(json, Hi8Response::class.java)
        if (response == null || !response.published) return null

        val days = response.days.mapNotNull { item ->
            runCatching {
                HolidayDay(
                    name = item.name.trim().ifBlank { "法定节假日" },
                    date = LocalDate.parse(item.date),
                    isOffDay = item.isOffDay,
                )
            }.getOrNull()
        }.filter { it.date.year == requestedYear }
            .distinctBy { it.date }
            .sortedBy { it.date }

        return days.takeIf { it.isNotEmpty() }?.let { RemoteHolidayYear(requestedYear, it) }
    }

    fun parseTimor(json: String, requestedYear: Int): RemoteHolidayYear? {
        val response = gson.fromJson(json, TimorResponse::class.java)
            ?: throw HolidayApiException("提莫 API 返回为空")
        if (response.code != 0) {
            throw HolidayApiException("提莫 API 错误码 ${response.code}")
        }

        val entries = response.holiday.orEmpty()
        if (entries.isEmpty()) return null

        val days = entries.mapNotNull { (monthDay, item) ->
            val isOffDay = item.holiday ?: return@mapNotNull null
            val date = parseTimorDate(item.date, monthDay, requestedYear) ?: return@mapNotNull null
            if (date.year != requestedYear) return@mapNotNull null

            val rawName = if (isOffDay) item.name else item.target ?: item.name
            HolidayDay(
                name = rawName.trim().ifBlank { if (isOffDay) "法定节假日" else "调休补班" },
                date = date,
                isOffDay = isOffDay,
            )
        }.distinctBy { it.date }
            .sortedBy { it.date }

        return days.takeIf { it.isNotEmpty() }?.let { RemoteHolidayYear(requestedYear, it) }
    }

    fun parseJuhe(json: String, requestedYear: Int): RemoteHolidayYear? {
        val response = gson.fromJson(json, JuheResponse::class.java)
            ?: throw HolidayApiException("聚合数据返回为空")
        val errorCode = response.errorCode
        if (errorCode != null && errorCode != 0) {
            val reason = response.reason.trim().ifBlank { "错误码 $errorCode" }
            throw HolidayApiException(reason)
        }

        val items = response.result?.list.orEmpty()
        if (items.isEmpty()) return null

        val byDate = linkedMapOf<LocalDate, HolidayDay>()
        items.forEach { item ->
            val name = item.name.trim().ifBlank { "法定节假日" }
            val offDates = extractDates(item.vacation, requestedYear).ifEmpty {
                if (item.isNotWork == 1) {
                    listOfNotNull(parseDateInYear(item.date, requestedYear))
                } else {
                    emptyList()
                }
            }
            val workDates = extractDates(item.remark, requestedYear).ifEmpty {
                if (item.isNotWork == 0 && item.dayCode == 3) {
                    listOfNotNull(parseDateInYear(item.date, requestedYear))
                } else {
                    emptyList()
                }
            }

            offDates.forEach { date ->
                byDate.putIfAbsent(date, HolidayDay(name = name, date = date, isOffDay = true))
            }
            workDates.forEach { date ->
                byDate.putIfAbsent(date, HolidayDay(name = name, date = date, isOffDay = false))
            }
        }

        val days = byDate.values.sortedBy { it.date }
        return days.takeIf { it.isNotEmpty() }?.let { RemoteHolidayYear(requestedYear, it) }
    }

    private fun parseTimorDate(rawDate: String, monthDayKey: String, year: Int): LocalDate? {
        val fromKey = runCatching { LocalDate.parse("$year-$monthDayKey") }.getOrNull()
        if (fromKey != null) return fromKey
        return runCatching { LocalDate.parse(rawDate) }.getOrNull()
    }

    private fun extractDates(element: JsonElement?, requestedYear: Int): List<LocalDate> {
        if (element == null || element.isJsonNull) return emptyList()
        return when {
            element.isJsonArray -> element.asJsonArray.flatMap { child ->
                extractDates(child, requestedYear)
            }
            element.isJsonPrimitive -> element.asString
                .split(DATE_SEPARATORS)
                .mapNotNull { parseDateInYear(it, requestedYear) }
            else -> emptyList()
        }.distinct()
    }

    private fun parseDateInYear(value: String, requestedYear: Int): LocalDate? =
        runCatching { LocalDate.parse(value.trim()) }
            .getOrNull()
            ?.takeIf { it.year == requestedYear }

    private val DATE_SEPARATORS = Regex("[|,，、\\s]+")

    private data class Hi8Response(
        val year: Int = 0,
        val published: Boolean = false,
        val days: List<Hi8Day> = emptyList(),
    )

    private data class Hi8Day(
        val date: String = "",
        val name: String = "",
        val isOffDay: Boolean = false,
    )

    private data class TimorResponse(
        val code: Int = -1,
        val holiday: Map<String, TimorDay>? = null,
    )

    private data class TimorDay(
        val holiday: Boolean? = null,
        val name: String = "",
        val date: String = "",
        val target: String? = null,
    )

    private data class JuheResponse(
        @SerializedName("error_code") val errorCode: Int? = null,
        val reason: String = "",
        val result: JuheResult? = null,
    )

    private data class JuheResult(
        val list: List<JuheHoliday> = emptyList(),
    )

    private data class JuheHoliday(
        val name: String = "",
        val vacation: JsonElement? = null,
        val remark: JsonElement? = null,
        val date: String = "",
        @SerializedName("isnotwork") val isNotWork: Int? = null,
        @SerializedName("daycode") val dayCode: Int? = null,
    )
}
