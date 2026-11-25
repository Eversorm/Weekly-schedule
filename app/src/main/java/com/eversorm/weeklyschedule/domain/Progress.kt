package com.eversorm.weeklyschedule.domain

import com.eversorm.weeklyschedule.data.Activity
import com.eversorm.weeklyschedule.data.Session
import com.eversorm.weeklyschedule.data.SessionStatus

/** "Climbing: 1 done, target 1" for one week. */
data class ActivityProgress(val activity: Activity, val done: Int) {
    val target: Int get() = activity.weeklyTarget
    val reached: Boolean get() = target > 0 && done >= target
}

/**
 * Progress for one week, given all activities and that week's sessions.
 * Shows active activities that have a target, plus any activity done at least once that week
 * (so an archived or target-less activity still appears in the week where you did it).
 */
fun weeklyProgress(activities: List<Activity>, weekSessions: List<Session>): List<ActivityProgress> {
    val doneCounts = weekSessions
        .filter { it.status == SessionStatus.DONE }
        .groupingBy { it.activityId }
        .eachCount()
    return activities
        .filter { (!it.archived && it.weeklyTarget > 0) || (doneCounts[it.id] ?: 0) > 0 }
        .map { ActivityProgress(it, doneCounts[it.id] ?: 0) }
}
