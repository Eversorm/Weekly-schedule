package com.eversorm.weeklyschedule.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

/*
 * DAO = "Data Access Object": all database queries in one place.
 * Room generates the implementation at build time (that's what KSP does).
 *
 * - Functions returning Flow<...> are "live": they emit a new value every time
 *   the data changes, so the screen updates automatically.
 * - "suspend" functions run in the background without freezing the screen.
 */
@Dao
abstract class RoutineDao {

    // ---- Activities ----

    @Query("SELECT * FROM activities ORDER BY id")
    abstract fun observeActivities(): Flow<List<Activity>>

    @Query("SELECT COUNT(*) FROM activities")
    abstract suspend fun countActivities(): Int

    @Insert
    abstract suspend fun insertActivity(activity: Activity): Long

    @Update
    abstract suspend fun updateActivity(activity: Activity)

    // ---- Typical week (template) ----

    @Query("SELECT * FROM template_slots")
    abstract fun observeTemplate(): Flow<List<TemplateSlot>>

    @Query("SELECT * FROM template_slots")
    abstract suspend fun getTemplate(): List<TemplateSlot>

    @Query("DELETE FROM template_slots WHERE activityId = :activityId")
    abstract suspend fun clearTemplateFor(activityId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTemplateSlots(slots: List<TemplateSlot>)

    /** Saves an activity and its typical days together (all or nothing). Returns the activity id. */
    @Transaction
    open suspend fun saveActivityWithDays(activity: Activity, days: Set<DayOfWeek>): Long {
        val id = if (activity.id == 0L) insertActivity(activity) else {
            updateActivity(activity)
            activity.id
        }
        clearTemplateFor(id)
        insertTemplateSlots(days.map { TemplateSlot(id, it.value) })
        return id
    }

    // ---- Sessions ----

    @Query("SELECT * FROM sessions WHERE date BETWEEN :from AND :to ORDER BY date, id")
    abstract fun observeSessions(from: LocalDate, to: LocalDate): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE activityId = :activityId AND date = :date AND status = 'PLANNED' LIMIT 1")
    abstract suspend fun findPlanned(activityId: Long, date: LocalDate): Session?

    @Insert
    abstract suspend fun insertSession(session: Session): Long

    @Insert
    abstract suspend fun insertSessions(sessions: List<Session>)

    @Update
    abstract suspend fun updateSession(session: Session)

    @Delete
    abstract suspend fun deleteSession(session: Session)

    // ---- Week pre-filling ----

    @Query("SELECT EXISTS(SELECT 1 FROM initialized_weeks WHERE weekStart = :weekStart)")
    abstract suspend fun isWeekInitialized(weekStart: LocalDate): Boolean

    @Query("SELECT COUNT(*) FROM initialized_weeks")
    abstract suspend fun countInitializedWeeks(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertInitializedWeek(week: InitializedWeek)

    /**
     * The very first time the app runs (empty database), creates your default activities
     * and typical week. Never runs again afterwards, even if you archive everything.
     */
    @Transaction
    open suspend fun seedDefaultsIfFirstRun() {
        if (countActivities() > 0 || countInitializedWeeks() > 0) return
        for (default in DefaultData.activities) {
            saveActivityWithDays(default.activity, default.days)
        }
    }

    /**
     * Copies the typical week into the given week, once. The only thing the app ever
     * does on its own: after that, the week is only changed by you.
     */
    @Transaction
    open suspend fun initializeWeekIfNeeded(weekStart: LocalDate) {
        seedDefaultsIfFirstRun()
        if (isWeekInitialized(weekStart)) return
        val activeIds = getActiveActivityIds().toSet()
        val sessions = getTemplate()
            .filter { it.activityId in activeIds }
            .map { slot ->
                Session(
                    activityId = slot.activityId,
                    date = weekStart.plusDays((slot.dayOfWeek - 1).toLong()),
                    status = SessionStatus.PLANNED,
                )
            }
        insertSessions(sessions)
        insertInitializedWeek(InitializedWeek(weekStart))
    }

    @Query("SELECT id FROM activities WHERE archived = 0")
    abstract suspend fun getActiveActivityIds(): List<Long>

    // ---- Backup (export / import) ----

    @Query("SELECT * FROM activities") abstract suspend fun getAllActivities(): List<Activity>
    @Query("SELECT * FROM sessions") abstract suspend fun getAllSessions(): List<Session>
    @Query("SELECT * FROM initialized_weeks") abstract suspend fun getAllInitializedWeeks(): List<InitializedWeek>

    @Insert abstract suspend fun insertActivities(activities: List<Activity>)
    @Insert abstract suspend fun insertInitializedWeeks(weeks: List<InitializedWeek>)

    @Query("DELETE FROM sessions") abstract suspend fun deleteAllSessions()
    @Query("DELETE FROM template_slots") abstract suspend fun deleteAllTemplateSlots()
    @Query("DELETE FROM activities") abstract suspend fun deleteAllActivities()
    @Query("DELETE FROM initialized_weeks") abstract suspend fun deleteAllInitializedWeeks()

    /** Replaces everything in the database with the given data (all or nothing). */
    @Transaction
    open suspend fun replaceAll(
        activities: List<Activity>,
        template: List<TemplateSlot>,
        sessions: List<Session>,
        weeks: List<InitializedWeek>,
    ) {
        deleteAllSessions()
        deleteAllTemplateSlots()
        deleteAllActivities()
        deleteAllInitializedWeeks()
        insertActivities(activities)
        insertTemplateSlots(template)
        insertSessions(sessions)
        insertInitializedWeeks(weeks)
    }
}
