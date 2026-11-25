package com.eversorm.weeklyschedule.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/*
 * Pure date logic, no Android code: easy to unit-test (see app/src/test).
 * Weeks always start on Monday.
 */

/** The Monday of the week containing [date]. */
fun weekStartOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** The 7 dates Monday..Sunday of the week starting at [weekStart]. */
fun daysOfWeek(weekStart: LocalDate): List<LocalDate> =
    (0L..6L).map { weekStart.plusDays(it) }
