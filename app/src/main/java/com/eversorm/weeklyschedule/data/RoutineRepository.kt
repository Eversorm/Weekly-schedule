package com.eversorm.weeklyschedule.data

import com.eversorm.weeklyschedule.domain.weekStartOf
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The single entry point to the data for the rest of the app (screens now; notifications
 * and widget later). Keeps "what the app does" separate from "how it is stored".
 */
class RoutineRepository(private val dao: RoutineDao) {

    /** All activities, including archived ones (needed to show names of old sessions). */
    val activities: Flow<List<Activity>> = dao.observeActivities()

    val template: Flow<List<TemplateSlot>> = dao.observeTemplate()

    fun observeWeekSessions(weekStart: LocalDate): Flow<List<Session>> =
        dao.observeSessions(weekStart, weekStart.plusDays(6))

    /**
     * Makes sure a week is pre-filled from the typical week before it is shown.
     * Past weeks (before the current one) are never pre-filled: they show what really happened.
     */
    suspend fun prepareWeek(weekStart: LocalDate, today: LocalDate) {
        if (weekStart >= weekStartOf(today)) {
            dao.initializeWeekIfNeeded(weekStart)
        } else {
            dao.seedDefaultsIfFirstRun()
        }
    }

    suspend fun setDone(session: Session, done: Boolean) {
        val status = if (done) SessionStatus.DONE else SessionStatus.PLANNED
        dao.updateSession(session.copy(status = status))
    }

    /**
     * The "+1" button: if this activity is planned today, mark that session done;
     * otherwise record an extra session done today.
     */
    suspend fun markDoneToday(activityId: Long, today: LocalDate) {
        val planned = dao.findPlanned(activityId, today)
        if (planned != null) {
            dao.updateSession(planned.copy(status = SessionStatus.DONE))
        } else {
            dao.insertSession(Session(activityId = activityId, date = today, status = SessionStatus.DONE))
        }
    }

    suspend fun addPlannedSession(activityId: Long, date: LocalDate) {
        dao.insertSession(Session(activityId = activityId, date = date, status = SessionStatus.PLANNED))
    }

    suspend fun deleteSession(session: Session) = dao.deleteSession(session)

    suspend fun saveActivity(activity: Activity, typicalDays: Set<DayOfWeek>) {
        dao.saveActivityWithDays(activity, typicalDays)
    }

    suspend fun setArchived(activity: Activity, archived: Boolean) {
        dao.updateActivity(activity.copy(archived = archived))
    }
}
