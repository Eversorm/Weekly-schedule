package com.eversorm.weeklyschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eversorm.weeklyschedule.data.Activity
import com.eversorm.weeklyschedule.data.SessionStatus
import com.eversorm.weeklyschedule.domain.ActivityProgress
import java.time.LocalDate

/** Main screen: weekly goals at the top, then the 7 days with their sessions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(onOpenActivities: () -> Unit, onOpenSettings: () -> Unit) {
    val repository = routineApp().repository
    val vm: WeekViewModel = viewModel { WeekViewModel(repository) }
    val state by vm.state.collectAsStateWithLifecycle()

    // Each time the app comes back to the foreground, check whether the date changed.
    LifecycleResumeEffect(Unit) {
        vm.onResume()
        onPauseOrDispose { }
    }

    // Which dialog is open (null = none).
    var addingTo by remember { mutableStateOf<LocalDate?>(null) }
    var removing by remember { mutableStateOf<SessionItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Schedule") },
                actions = {
                    IconButton(onClick = onOpenActivities) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Activities")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        val s = state ?: return@Scaffold // still loading: show nothing for a split second

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                WeekHeader(
                    state = s,
                    onPrevious = vm::previousWeek,
                    onNext = vm::nextWeek,
                    onCurrent = vm::goToCurrentWeek,
                )
            }

            item { SectionTitle("Goals") }
            if (s.progress.isEmpty()) {
                item { MutedText("No weekly goals. Set them in Activities.") }
            }
            items(s.progress, key = { "goal-${it.activity.id}" }) { p ->
                GoalRow(
                    progress = p,
                    // "+1" means "done today", so it only makes sense in the current week.
                    showPlusOne = s.isCurrentWeek && !p.activity.archived,
                    onPlusOne = { vm.markDoneToday(p.activity.id) },
                )
            }

            item { SectionTitle("Days") }
            items(s.days, key = { "day-${it.date}" }) { day ->
                DaySection(
                    day = day,
                    isToday = day.date == s.today,
                    onAdd = { addingTo = day.date },
                    onToggle = { item, done -> vm.setDone(item, done) },
                    onRemove = { removing = it },
                )
                HorizontalDivider()
            }
        }

        addingTo?.let { date ->
            AddSessionDialog(
                date = date,
                activities = s.activeActivities,
                onPick = { activity ->
                    vm.addSession(activity.id, date)
                    addingTo = null
                },
                onDismiss = { addingTo = null },
            )
        }

        removing?.let { item ->
            AlertDialog(
                onDismissRequest = { removing = null },
                title = { Text("Remove session?") },
                text = { Text("${item.activityName} on ${formatDay(item.session.date)}") },
                confirmButton = {
                    TextButton(onClick = { vm.delete(item); removing = null }) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { removing = null }) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun WeekHeader(
    state: WeekUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatWeekRange(state.weekStart), style = MaterialTheme.typography.titleLarge)
            if (state.isCurrentWeek) {
                MutedText("This week")
            } else {
                TextButton(onClick = onCurrent) { Text("Back to this week") }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun MutedText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun GoalRow(progress: ActivityProgress, showPlusOne: Boolean, onPlusOne: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(progress.activity.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = if (progress.target > 0) "${progress.done} / ${progress.target}" else "${progress.done} done",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (progress.reached) FontWeight.Bold else FontWeight.Normal,
            color = if (progress.reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (showPlusOne) {
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = onPlusOne) { Text("+1") }
        }
    }
}

@Composable
private fun DaySection(
    day: DayItem,
    isToday: Boolean,
    onAdd: () -> Unit,
    onToggle: (SessionItem, Boolean) -> Unit,
    onRemove: (SessionItem) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatDay(day.date) + if (isToday) "  ·  Today" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add session on ${formatDay(day.date)}")
            }
        }
        if (day.sessions.isEmpty()) {
            MutedText("Nothing planned")
        }
        day.sessions.forEach { item ->
            SessionRow(item, onToggle = { onToggle(item, it) }, onRemove = { onRemove(item) })
        }
    }
}

/** One session: tap anywhere on the row to mark it done / not done. */
@Composable
private fun SessionRow(item: SessionItem, onToggle: (Boolean) -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = item.isDone, role = Role.Checkbox, onValueChange = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isDone, onCheckedChange = null) // the whole row handles the tap
        Text(
            text = item.activityName + if (item.session.status == SessionStatus.SKIPPED) " (skipped)" else "",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove ${item.activityName}")
        }
    }
}

@Composable
private fun AddSessionDialog(
    date: LocalDate,
    activities: List<Activity>,
    onPick: (Activity) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${formatDay(date)}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (activities.isEmpty()) {
                    Text("No activities yet. Create one in Activities.")
                }
                activities.forEach { activity ->
                    TextButton(onClick = { onPick(activity) }, modifier = Modifier.fillMaxWidth()) {
                        Text(activity.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
