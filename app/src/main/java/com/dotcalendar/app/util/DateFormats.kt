package com.dotcalendar.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val monthDayFormatter = DateTimeFormatter.ofPattern("M月d日")

val chineseWeekdays = listOf("一", "二", "三", "四", "五", "六", "日")

fun LocalDate.chineseWeekday(): String = "周" + chineseWeekdays[dayOfWeek.value - 1]

fun LocalDate.chineseMonthDay(): String = format(monthDayFormatter)

fun dateRangeText(start: LocalDate, end: LocalDate): String =
    if (start == end) start.chineseMonthDay() else "${start.chineseMonthDay()} — ${end.chineseMonthDay()}"

fun monthTitle(year: Int, month: Int): String = "$year·${month.toString().padStart(2, '0')}"

val chineseMonths = listOf(
    "一月", "二月", "三月", "四月", "五月", "六月",
    "七月", "八月", "九月", "十月", "十一月", "十二月",
)
