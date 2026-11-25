package com.eversorm.weeklyschedule.data

import java.time.DayOfWeek

/** What the app contains the first time you open it. You can change all of it in "Activities". */
object DefaultData {
    data class DefaultActivity(val activity: Activity, val days: Set<DayOfWeek>)

    val activities = listOf(
        DefaultActivity(
            Activity(name = "German", weeklyTarget = 3, isSport = false, weekendOnly = false),
            setOf(DayOfWeek.MONDAY),
        ),
        DefaultActivity(
            Activity(name = "Climbing", weeklyTarget = 1, isSport = true, weekendOnly = false),
            setOf(DayOfWeek.WEDNESDAY),
        ),
        DefaultActivity(
            Activity(name = "Easy run", weeklyTarget = 1, isSport = true, weekendOnly = false),
            setOf(DayOfWeek.THURSDAY),
        ),
        DefaultActivity(
            Activity(name = "Long run", weeklyTarget = 1, isSport = true, weekendOnly = true),
            setOf(DayOfWeek.SUNDAY),
        ),
    )
}
