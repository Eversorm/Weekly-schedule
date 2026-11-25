package com.eversorm.weeklyschedule.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/* Date texts shown in the app. English, to match the rest of the app's text. */

private val locale = Locale.ENGLISH
private val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", locale)

fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, locale)

/** "Mon 23 Nov" */
fun formatDay(date: LocalDate): String = "${date.dayOfWeek.shortName()} ${date.format(dayMonth)}"

/** "23 – 29 Nov 2026" or "30 Nov – 6 Dec 2026" */
fun formatWeekRange(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    return if (weekStart.month == end.month) {
        "${weekStart.dayOfMonth} – ${end.format(dayMonthYear)}"
    } else {
        "${weekStart.format(dayMonth)} – ${end.format(dayMonthYear)}"
    }
}
