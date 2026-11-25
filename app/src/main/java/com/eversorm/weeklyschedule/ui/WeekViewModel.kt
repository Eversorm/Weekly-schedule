package com.eversorm.weeklyschedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eversorm.weeklyschedule.data.Activity
import com.eversorm.weeklyschedule.data.RoutineRepository
import com.eversorm.weeklyschedule.data.Session
import com.eversorm.weeklyschedule.data.SessionStatus
import com.eversorm.weeklyschedule.domain.ActivityProgress
import com.eversorm.weeklyschedule.domain.daysOfWeek
import com.eversorm.weeklyschedule.domain.weekStartOf
import com.eversorm.weeklyschedule.domain.weeklyProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything the week screen needs to draw itself. */
data class WeekUiState(
    val weekStart: LocalDate,
    val today: LocalDate,
    val progress: List<ActivityProgress>,
    val days: List<DayItem>,
    /** Activities you can add to a day (not archived). */
    val activeActivities: List<Activity>,
) {
    val isCurrentWeek: Boolean get() = weekStart == weekStartOf(today)
}

data class DayItem(val date: LocalDate, val sessions: List<SessionItem>)

data class SessionItem(val session: Session, val activityName: String) {
    val isDone: Boolean get() = session.status == SessionStatus.DONE
}

/**
 * The "brain" of the week screen: holds which week is shown, combines the data from the
 * repository into a WeekUiState, and handles taps. The screen only draws the state.
 * A ViewModel survives screen rotation and switching to the other screens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModel(private val repo: RoutineRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())
    private val weekStart = MutableStateFlow(weekStartOf(LocalDate.now()))

    /** Each time the shown week changes: pre-fill it if needed, then watch its sessions. */
    private val weekSessions = weekStart.flatMapLatest { start ->
        flow {
            repo.prepareWeek(start, today.value)
            emitAll(repo.observeWeekSessions(start).map { sessions -> start to sessions })
        }
    }

    /** null while the first data is loading. */
    val state: StateFlow<WeekUiState?> =
        combine(today, weekSessions, repo.activities) { today, (start, sessions), activities ->
            val names = activities.associate { it.id to it.name }
            WeekUiState(
                weekStart = start,
                today = today,
                progress = weeklyProgress(activities, sessions),
                days = daysOfWeek(start).map { date ->
                    DayItem(
                        date = date,
                        sessions = sessions
                            .filter { it.date == date }
                            .map { SessionItem(it, names[it.activityId] ?: "?") },
                    )
                },
                activeActivities = activities.filter { !it.archived },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousWeek() { weekStart.value = weekStart.value.minusWeeks(1) }
    fun nextWeek() { weekStart.value = weekStart.value.plusWeeks(1) }
    fun goToCurrentWeek() { weekStart.value = weekStartOf(today.value) }

    /** Called when the app comes back to the foreground: the date may have changed (e.g. overnight). */
    fun onResume() {
        val now = LocalDate.now()
        if (now == today.value) return
        val wasOnCurrentWeek = weekStart.value == weekStartOf(today.value)
        today.value = now
        if (wasOnCurrentWeek) weekStart.value = weekStartOf(now)
    }

    fun setDone(item: SessionItem, done: Boolean) = viewModelScope.launch {
        repo.setDone(item.session, done)
    }

    fun markDoneToday(activityId: Long) = viewModelScope.launch {
        repo.markDoneToday(activityId, today.value)
    }

    fun addSession(activityId: Long, date: LocalDate) = viewModelScope.launch {
        repo.addPlannedSession(activityId, date)
    }

    fun delete(item: SessionItem) = viewModelScope.launch {
        repo.deleteSession(item.session)
    }
}
