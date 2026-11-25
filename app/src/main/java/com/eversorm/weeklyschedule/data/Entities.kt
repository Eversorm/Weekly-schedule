package com.eversorm.weeklyschedule.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/*
 * The database tables. Each class annotated with @Entity becomes one table;
 * each property becomes a column. Room generates the SQL for us.
 */

/** Something you want to do regularly, e.g. "Climbing", 1x per week. */
@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** How many sessions per week you aim for. 0 = no goal, just something you can plan. */
    val weeklyTarget: Int,
    /** Sport sessions are checked by the rescheduling rules (Phase 4). */
    val isSport: Boolean,
    /** E.g. the long run: may only be planned on Saturday or Sunday (Phase 4). */
    val weekendOnly: Boolean,
    /** Reminder time as minutes after midnight (e.g. 18:30 = 1110). Null = no reminder. Used from Phase 2. */
    val reminderMinutes: Int? = null,
    /** Archived activities are hidden, but their past sessions are kept. */
    val archived: Boolean = false,
)

/**
 * Your "typical week": activity X is normally on day Y.
 * Every new week is pre-filled from these slots. dayOfWeek: 1 = Monday ... 7 = Sunday.
 */
@Entity(
    tableName = "template_slots",
    primaryKeys = ["activityId", "dayOfWeek"],
    foreignKeys = [ForeignKey(
        entity = Activity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("activityId")],
)
data class TemplateSlot(
    val activityId: Long,
    val dayOfWeek: Int,
)

enum class SessionStatus { PLANNED, DONE, SKIPPED }

/** One concrete session on a concrete date, e.g. "Climbing on Wed 25 Nov 2026, done". */
@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = Activity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("activityId"), Index("date")],
)
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val date: LocalDate,
    val status: SessionStatus,
    /** If the session was moved to another day (Phase 4), the day it was originally planned. */
    val originalDate: LocalDate? = null,
)

/**
 * Remembers which weeks were already pre-filled from the template, so that a session
 * you delete does not come back the next time you open the app.
 */
@Entity(tableName = "initialized_weeks")
data class InitializedWeek(
    /** The Monday of the week. */
    @PrimaryKey val weekStart: LocalDate,
)
