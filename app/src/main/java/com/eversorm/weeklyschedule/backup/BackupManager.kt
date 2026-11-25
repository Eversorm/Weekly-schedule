package com.eversorm.weeklyschedule.backup

import com.eversorm.weeklyschedule.data.Activity
import com.eversorm.weeklyschedule.data.InitializedWeek
import com.eversorm.weeklyschedule.data.RoutineDao
import com.eversorm.weeklyschedule.data.Session
import com.eversorm.weeklyschedule.data.SessionStatus
import com.eversorm.weeklyschedule.data.TemplateSlot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

/*
 * Backup file format. These classes mirror the database tables, with dates written as
 * text ("2026-11-25") so the file is readable. "formatVersion" lets future versions of
 * the app still read old backup files.
 */

@Serializable
data class BackupFile(
    val formatVersion: Int,
    val activities: List<ActivityJson>,
    val typicalWeek: List<TemplateSlotJson>,
    val sessions: List<SessionJson>,
    val prefilledWeeks: List<String>,
)

@Serializable
data class ActivityJson(
    val id: Long,
    val name: String,
    val weeklyTarget: Int,
    val isSport: Boolean,
    val weekendOnly: Boolean,
    val reminderMinutes: Int? = null,
    val archived: Boolean = false,
)

@Serializable
data class TemplateSlotJson(val activityId: Long, val dayOfWeek: Int)

@Serializable
data class SessionJson(
    val id: Long,
    val activityId: Long,
    val date: String,
    val status: String,
    val originalDate: String? = null,
)

class BackupManager(private val dao: RoutineDao) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // newer backup files with extra fields can still be read
    }

    suspend fun export(output: OutputStream) {
        val file = BackupFile(
            formatVersion = FORMAT_VERSION,
            activities = dao.getAllActivities().map {
                ActivityJson(it.id, it.name, it.weeklyTarget, it.isSport, it.weekendOnly, it.reminderMinutes, it.archived)
            },
            typicalWeek = dao.getTemplate().map { TemplateSlotJson(it.activityId, it.dayOfWeek) },
            sessions = dao.getAllSessions().map {
                SessionJson(it.id, it.activityId, it.date.toString(), it.status.name, it.originalDate?.toString())
            },
            prefilledWeeks = dao.getAllInitializedWeeks().map { it.weekStart.toString() },
        )
        output.writer().use { it.write(json.encodeToString(BackupFile.serializer(), file)) }
    }

    /**
     * Replaces ALL current data with the file's content. The file is fully read and checked
     * first; if anything is wrong, an exception is thrown and the current data is untouched.
     */
    suspend fun import(input: InputStream) {
        val text = input.bufferedReader().use { it.readText() }
        val file = json.decodeFromString(BackupFile.serializer(), text)
        require(file.formatVersion <= FORMAT_VERSION) {
            "This backup was made by a newer version of the app."
        }
        val activities = file.activities.map {
            Activity(it.id, it.name, it.weeklyTarget, it.isSport, it.weekendOnly, it.reminderMinutes, it.archived)
        }
        val ids = activities.map { it.id }.toSet()
        val template = file.typicalWeek
            .filter { it.activityId in ids && it.dayOfWeek in 1..7 }
            .map { TemplateSlot(it.activityId, it.dayOfWeek) }
        val sessions = file.sessions.filter { it.activityId in ids }.map {
            Session(
                id = it.id,
                activityId = it.activityId,
                date = LocalDate.parse(it.date),
                status = SessionStatus.valueOf(it.status),
                originalDate = it.originalDate?.let(LocalDate::parse),
            )
        }
        val weeks = file.prefilledWeeks.map { InitializedWeek(LocalDate.parse(it)) }
        dao.replaceAll(activities, template, sessions, weeks)
    }

    companion object {
        const val FORMAT_VERSION = 1
    }
}
