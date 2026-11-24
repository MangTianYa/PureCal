package com.dotcalendar.app

import com.dotcalendar.app.data.HolidayApiException
import com.dotcalendar.app.data.HolidayDataSource
import com.dotcalendar.app.data.HolidayJsonParser
import com.dotcalendar.app.data.HolidayNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayProviderParsingTest {
    @Test
    fun hi8ParsesOfficialYear() {
        val year = HolidayJsonParser.parseHi8(hi8Json, 2026)

        assertEquals(39, year?.days?.size)
        assertEquals(33, year?.days?.count { it.isOffDay })
        assertEquals(6, year?.days?.count { !it.isOffDay })
        assertEquals("春节", year?.days?.first { it.date == LocalDate.of(2026, 2, 15) }?.name)
    }

    @Test
    fun hi8UnpublishedYearReturnsNull() {
        val json = """{"year":2027,"published":false,"days":[]}"""
        assertNull(HolidayJsonParser.parseHi8(json, 2027))
    }

    @Test
    fun timorNormalizesSpringFestivalAndCombinedNationalDay() {
        val year = HolidayJsonParser.parseTimor(timor2025Json, 2025)
            ?: error("Timor response should parse")

        val normalized = HolidayNormalizer.normalize(year.days)
        assertEquals(28, normalized.count { it.isOffDay })
        assertEquals(5, normalized.count { !it.isOffDay })
        assertEquals("春节", normalized.first { it.date == LocalDate.of(2025, 1, 28) }.name)
        assertEquals("春节", normalized.first { it.date == LocalDate.of(2025, 2, 4) }.name)
        assertEquals(
            "国庆节、中秋节",
            normalized.first { it.date == LocalDate.of(2025, 10, 6) }.name,
        )
        assertEquals(
            "国庆节、中秋节",
            normalized.first { it.date == LocalDate.of(2025, 9, 28) }.name,
        )
    }

    @Test
    fun timorUsesMonthDayKeyWhenDateIsMissing() {
        val json = """
            {"code":0,"holiday":{
              "10-10":{"holiday":false,"name":"国庆节后补班","target":"国庆节"},
              "10-01":{"holiday":true,"name":"国庆节"}
            }}
        """.trimIndent()

        val year = HolidayJsonParser.parseTimor(json, 2026) ?: error("Should parse")
        assertEquals(2, year.days.size)
        assertEquals(
            LocalDate.of(2026, 10, 10),
            year.days.first { !it.isOffDay }.date,
        )
    }

    @Test
    fun juheParsesArrayAndPipeSeparatedDates() {
        val json = """
            {
              "reason":"success",
              "error_code":0,
              "result":{
                "list":[
                  {"name":"元旦","vacation":"2024-01-01","remark":"","date":"2024-01-01"},
                  {"name":"春节","vacation":["2024-02-10","2024-02-11"],"remark":"2024-02-04|2024-02-18"},
                  {"name":"跨年","vacation":"2023-12-31","remark":""}
                ]
              }
            }
        """.trimIndent()

        val year = HolidayJsonParser.parseJuhe(json, 2024) ?: error("Should parse")
        assertEquals(5, year.days.size)
        assertTrue(year.days.none { it.date.year != 2024 })
        assertEquals("春节", year.days.first { it.date == LocalDate.of(2024, 2, 4) }.name)
        assertEquals(false, year.days.first { it.date == LocalDate.of(2024, 2, 4) }.isOffDay)
    }

    @Test(expected = HolidayApiException::class)
    fun juheSurfacesApiError() {
        HolidayJsonParser.parseJuhe(
            """{"resultcode":"101","reason":"错误的请求KEY","result":null,"error_code":10001}""",
            2026,
        )
    }

    @Test
    fun sourceKeysAreStable() {
        assertEquals(HolidayDataSource.HI8, HolidayDataSource.fromKey("hi8"))
        assertEquals(HolidayDataSource.AUTO, HolidayDataSource.fromKey("missing"))
        assertEquals(5, HolidayDataSource.entries.size)
        assertTrue(HolidayDataSource.JUHE.requiresApiKey)
        assertTrue(HolidayDataSource.entries.all { it.endpointLabel.isNotBlank() })
        assertTrue(HolidayDataSource.entries.all { it.terms.isNotBlank() })
    }

    private val hi8Json = """
        {"year":2026,"published":true,"days":[
          {"date":"2026-01-01","name":"元旦","isOffDay":true},
          {"date":"2026-01-02","name":"元旦","isOffDay":true},
          {"date":"2026-01-03","name":"元旦","isOffDay":true},
          {"date":"2026-01-04","name":"元旦","isOffDay":false},
          {"date":"2026-02-14","name":"春节","isOffDay":false},
          {"date":"2026-02-15","name":"春节","isOffDay":true},
          {"date":"2026-02-16","name":"春节","isOffDay":true},
          {"date":"2026-02-17","name":"春节","isOffDay":true},
          {"date":"2026-02-18","name":"春节","isOffDay":true},
          {"date":"2026-02-19","name":"春节","isOffDay":true},
          {"date":"2026-02-20","name":"春节","isOffDay":true},
          {"date":"2026-02-21","name":"春节","isOffDay":true},
          {"date":"2026-02-22","name":"春节","isOffDay":true},
          {"date":"2026-02-23","name":"春节","isOffDay":true},
          {"date":"2026-02-28","name":"春节","isOffDay":false},
          {"date":"2026-04-04","name":"清明节","isOffDay":true},
          {"date":"2026-04-05","name":"清明节","isOffDay":true},
          {"date":"2026-04-06","name":"清明节","isOffDay":true},
          {"date":"2026-05-01","name":"劳动节","isOffDay":true},
          {"date":"2026-05-02","name":"劳动节","isOffDay":true},
          {"date":"2026-05-03","name":"劳动节","isOffDay":true},
          {"date":"2026-05-04","name":"劳动节","isOffDay":true},
          {"date":"2026-05-05","name":"劳动节","isOffDay":true},
          {"date":"2026-05-09","name":"劳动节","isOffDay":false},
          {"date":"2026-06-19","name":"端午节","isOffDay":true},
          {"date":"2026-06-20","name":"端午节","isOffDay":true},
          {"date":"2026-06-21","name":"端午节","isOffDay":true},
          {"date":"2026-09-20","name":"国庆节","isOffDay":false},
          {"date":"2026-09-25","name":"中秋节","isOffDay":true},
          {"date":"2026-09-26","name":"中秋节","isOffDay":true},
          {"date":"2026-09-27","name":"中秋节","isOffDay":true},
          {"date":"2026-10-01","name":"国庆节","isOffDay":true},
          {"date":"2026-10-02","name":"国庆节","isOffDay":true},
          {"date":"2026-10-03","name":"国庆节","isOffDay":true},
          {"date":"2026-10-04","name":"国庆节","isOffDay":true},
          {"date":"2026-10-05","name":"国庆节","isOffDay":true},
          {"date":"2026-10-06","name":"国庆节","isOffDay":true},
          {"date":"2026-10-07","name":"国庆节","isOffDay":true},
          {"date":"2026-10-10","name":"国庆节","isOffDay":false}
        ]}
    """.trimIndent()

    private val timor2025Json = """
        {"code":0,"holiday":{
          "01-01":{"holiday":true,"name":"元旦","date":"2025-01-01"},
          "01-26":{"holiday":false,"name":"春节前补班","target":"春节","date":"2025-01-26"},
          "01-28":{"holiday":true,"name":"除夕","date":"2025-01-28"},
          "01-29":{"holiday":true,"name":"初一","date":"2025-01-29"},
          "01-30":{"holiday":true,"name":"初二","date":"2025-01-30"},
          "01-31":{"holiday":true,"name":"初三","date":"2025-01-31"},
          "02-01":{"holiday":true,"name":"初四","date":"2025-02-01"},
          "02-02":{"holiday":true,"name":"初五","date":"2025-02-02"},
          "02-03":{"holiday":true,"name":"初六","date":"2025-02-03"},
          "02-04":{"holiday":true,"name":"初七","date":"2025-02-04"},
          "02-08":{"holiday":false,"name":"春节后补班","target":"春节","date":"2025-02-08"},
          "04-04":{"holiday":true,"name":"清明节","date":"2025-04-04"},
          "04-05":{"holiday":true,"name":"清明节","date":"2025-04-05"},
          "04-06":{"holiday":true,"name":"清明节","date":"2025-04-06"},
          "04-27":{"holiday":false,"name":"劳动节前补班","target":"劳动节","date":"2025-04-27"},
          "05-01":{"holiday":true,"name":"劳动节","date":"2025-05-01"},
          "05-02":{"holiday":true,"name":"劳动节","date":"2025-05-02"},
          "05-03":{"holiday":true,"name":"劳动节","date":"2025-05-03"},
          "05-04":{"holiday":true,"name":"劳动节","date":"2025-05-04"},
          "05-05":{"holiday":true,"name":"劳动节","date":"2025-05-05"},
          "05-31":{"holiday":true,"name":"端午节","date":"2025-05-31"},
          "06-01":{"holiday":true,"name":"端午节","date":"2025-06-01"},
          "06-02":{"holiday":true,"name":"端午节","date":"2025-06-02"},
          "09-28":{"holiday":false,"name":"国庆节前补班","target":"国庆节","date":"2025-09-28"},
          "10-01":{"holiday":true,"name":"国庆节","date":"2025-10-01"},
          "10-02":{"holiday":true,"name":"国庆节","date":"2025-10-02"},
          "10-03":{"holiday":true,"name":"国庆节","date":"2025-10-03"},
          "10-04":{"holiday":true,"name":"国庆节","date":"2025-10-04"},
          "10-05":{"holiday":true,"name":"国庆节","date":"2025-10-05"},
          "10-06":{"holiday":true,"name":"中秋节","date":"2025-10-06"},
          "10-07":{"holiday":true,"name":"国庆节","date":"2025-10-07"},
          "10-08":{"holiday":true,"name":"国庆节","date":"2025-10-08"},
          "10-11":{"holiday":false,"name":"国庆节后补班","target":"国庆节","date":"2025-10-11"}
        }}
    """.trimIndent()
}
