package com.eversorm.weeklyschedule.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Unit tests run on your computer (no phone needed): right-click the file > Run. */
class WeekDatesTest {
    @Test
    fun weekStartsOnMonday() {
        val monday = LocalDate.of(2026, 11, 23)
        assertEquals(monday, weekStartOf(LocalDate.of(2026, 11, 23))) // Monday itself
        assertEquals(monday, weekStartOf(LocalDate.of(2026, 11, 25))) // Wednesday
        assertEquals(monday, weekStartOf(LocalDate.of(2026, 11, 29))) // Sunday
        assertEquals(LocalDate.of(2026, 11, 30), weekStartOf(LocalDate.of(2026, 11, 30))) // next Monday
    }

    @Test
    fun weekAcrossYearEnd() {
        assertEquals(LocalDate.of(2026, 12, 28), weekStartOf(LocalDate.of(2027, 1, 3)))
    }

    @Test
    fun sevenDaysMondayToSunday() {
        val days = daysOfWeek(LocalDate.of(2026, 11, 23))
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 11, 29), days.last())
    }
}
