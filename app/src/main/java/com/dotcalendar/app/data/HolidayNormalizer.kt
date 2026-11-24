package com.dotcalendar.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 不同接口对同一个假期的命名并不一致。例如提莫 API 会把春节拆成「除夕」「初一」，
 * 把 2025 年国庆与中秋拆成两个名字。这里把连续放假日合并为同一个假期段名称，
 * 并把调休上班日对齐到最近的假期段，保证月视图与节假日页表现一致。
 */
internal object HolidayNormalizer {
    private val springFestivalDays = setOf(
        "除夕", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八",
    )

    fun normalize(days: List<HolidayDay>): List<HolidayDay> {
        val canonical = days
            .map { it.copy(name = canonicalName(it.name, it.isOffDay)) }
            .distinctBy { it.date }
            .sortedBy { it.date }

        val offRuns = contiguousRuns(canonical.filter { it.isOffDay })
        val namedRuns = offRuns.map { run ->
            HolidayRun(
                start = run.first().date,
                end = run.last().date,
                name = combinedName(run.map { it.name }),
            )
        }
        val offNameByDate = namedRuns.flatMap { run ->
            generateSequence(run.start) { date ->
                date.plusDays(1).takeIf { !it.isAfter(run.end) }
            }.map { it to run.name }
        }.toMap()

        return canonical.map { day ->
            if (day.isOffDay) {
                day.copy(name = offNameByDate[day.date] ?: day.name)
            } else {
                val run = namedRuns
                    .filter { namesCompatible(day.name, it.name) }
                    .minByOrNull { distance(day.date, it) }
                if (run != null && distance(day.date, run) <= 21) {
                    day.copy(name = run.name)
                } else {
                    day
                }
            }
        }
    }

    private fun canonicalName(raw: String, isOffDay: Boolean): String {
        val name = raw.trim()
            .removeSuffix("（休）")
            .removeSuffix("(休)")
        val withoutMakeupSuffix = name
            .removeSuffix("前补班")
            .removeSuffix("后补班")
            .removeSuffix("补班")
            .trim()

        return when {
            withoutMakeupSuffix in springFestivalDays || withoutMakeupSuffix.contains("春节") -> "春节"
            withoutMakeupSuffix == "元旦节" || withoutMakeupSuffix == "中国元旦" -> "元旦"
            withoutMakeupSuffix.isNotBlank() -> withoutMakeupSuffix
            isOffDay -> "法定节假日"
            else -> "调休补班"
        }
    }

    private fun combinedName(names: List<String>): String {
        val distinct = names.distinct()
        if (distinct.isEmpty()) return "法定节假日"
        if (distinct.size == 1) return distinct.first()
        if (distinct.any { it.contains("国庆节") && it.contains("中秋节") }) {
            return "国庆节、中秋节"
        }
        if (distinct.contains("国庆节") && distinct.contains("中秋节")) {
            return "国庆节、中秋节"
        }
        return distinct.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?: distinct.first()
    }

    private fun namesCompatible(workdayName: String, runName: String): Boolean =
        workdayName == runName ||
            runName.contains(workdayName) ||
            workdayName.contains(runName)

    private fun distance(date: LocalDate, run: HolidayRun): Long = when {
        date.isBefore(run.start) -> ChronoUnit.DAYS.between(date, run.start)
        date.isAfter(run.end) -> ChronoUnit.DAYS.between(run.end, date)
        else -> 0
    }

    private fun contiguousRuns(offDays: List<HolidayDay>): List<List<HolidayDay>> {
        if (offDays.isEmpty()) return emptyList()
        val runs = mutableListOf<MutableList<HolidayDay>>()
        var current = mutableListOf(offDays.first())
        for (index in 1 until offDays.size) {
            if (offDays[index].date == offDays[index - 1].date.plusDays(1)) {
                current += offDays[index]
            } else {
                runs += current
                current = mutableListOf(offDays[index])
            }
        }
        runs += current
        return runs
    }

    private data class HolidayRun(
        val start: LocalDate,
        val end: LocalDate,
        val name: String,
    )
}
