package com.eversorm.weeklyschedule.domain

import com.eversorm.weeklyschedule.data.Activity
import com.eversorm.weeklyschedule.data.Session
import com.eversorm.weeklyschedule.data.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    private val day = LocalDate.of(2026, 11, 25)
    private val german = Activity(id = 1, name = "German", weeklyTarget = 3, isSport = false, weekendOnly = false)
    private val climbing = Activity(id = 2, name = "Climbing", weeklyTarget = 1, isSport = true, weekendOnly = false)
    private val swim = Activity(id = 3, name = "Swim", weeklyTarget = 0, isSport = true, weekendOnly = false)
    private val old = Activity(id = 4, name = "Old", weeklyTarget = 1, isSport = false, weekendOnly = false, archived = true)

    private fun session(activity: Activity, status: SessionStatus) =
        Session(activityId = activity.id, date = day, status = status)

    @Test
    fun countsOnlyDoneSessions() {
        val sessions = listOf(
            session(german, SessionStatus.DONE),
            session(german, SessionStatus.PLANNED),
            session(climbing, SessionStatus.DONE),
        )
        val result = weeklyProgress(listOf(german, climbing), sessions).associateBy { it.activity.id }
        assertEquals(1, result.getValue(1).done)
        assertEquals(1, result.getValue(2).done)
        assertTrue(result.getValue(2).reached)
    }

    @Test
    fun hidesTargetlessAndArchivedUnlessDone() {
        val none = weeklyProgress(listOf(german, swim, old), emptyList())
        assertEquals(listOf(german.id), none.map { it.activity.id })

        val withDone = weeklyProgress(listOf(german, swim, old), listOf(session(swim, SessionStatus.DONE), session(old, SessionStatus.DONE)))
        assertEquals(listOf(german.id, swim.id, old.id), withDone.map { it.activity.id })
    }
}
