package com.eversorm.weeklyschedule.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eversorm.weeklyschedule.data.Activity
import com.eversorm.weeklyschedule.data.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/** An activity together with its typical days. */
data class ActivityItem(val activity: Activity, val typicalDays: Set<DayOfWeek>)

class ActivitiesViewModel(private val repo: RoutineRepository) : ViewModel() {
    val items: StateFlow<List<ActivityItem>> =
        combine(repo.activities, repo.template) { activities, slots ->
            activities.map { a ->
                ActivityItem(a, slots.filter { it.activityId == a.id }.map { DayOfWeek.of(it.dayOfWeek) }.toSet())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(activity: Activity, days: Set<DayOfWeek>) = viewModelScope.launch {
        repo.saveActivity(activity, days)
    }

    fun setArchived(activity: Activity, archived: Boolean) = viewModelScope.launch {
        repo.setArchived(activity, archived)
    }
}

/** List of activities with their weekly target and typical days. Tap one to edit it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(onBack: () -> Unit) {
    val repository = routineApp().repository
    val vm: ActivitiesViewModel = viewModel { ActivitiesViewModel(repository) }
    val items by vm.items.collectAsStateWithLifecycle()

    // The item being edited; a "new" item (id = 0) when adding.
    var editing by remember { mutableStateOf<ActivityItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activities") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val active = items.filter { !it.activity.archived }
        val archived = items.filter { it.activity.archived }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Text(
                    "Typical days are copied into each new week the first time you open it. " +
                        "Weeks you already opened are not changed: edit those directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(active, key = { it.activity.id }) { item ->
                ActivityRow(item) { editing = item }
                HorizontalDivider()
            }
            item {
                Button(
                    onClick = {
                        editing = ActivityItem(
                            Activity(name = "", weeklyTarget = 1, isSport = false, weekendOnly = false),
                            emptySet(),
                        )
                    },
                    modifier = Modifier.padding(vertical = 16.dp),
                ) { Text("Add activity") }
            }
            if (archived.isNotEmpty()) {
                item {
                    Text(
                        "Archived",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(archived, key = { it.activity.id }) { item ->
                    ActivityRow(item) { editing = item }
                    HorizontalDivider()
                }
            }
        }

        editing?.let { item ->
            EditActivityDialog(
                item = item,
                onSave = { activity, days -> vm.save(activity, days); editing = null },
                onToggleArchived = { vm.setArchived(item.activity, !item.activity.archived); editing = null },
                onDismiss = { editing = null },
            )
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem, onClick: () -> Unit) {
    val a = item.activity
    val details = listOfNotNull(
        if (a.weeklyTarget > 0) "${a.weeklyTarget}× per week" else "No weekly goal",
        if (a.isSport) "Sport" else null,
        if (a.weekendOnly) "Weekend only" else null,
        if (item.typicalDays.isNotEmpty()) {
            "Usually " + item.typicalDays.sorted().joinToString(", ") { it.shortName() }
        } else null,
    ).joinToString(" · ")

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(a.name, style = MaterialTheme.typography.bodyLarge)
        Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditActivityDialog(
    item: ActivityItem,
    onSave: (Activity, Set<DayOfWeek>) -> Unit,
    onToggleArchived: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = item.activity.id == 0L
    // Local copies of the fields while editing; saved only when you press "Save".
    var name by remember { mutableStateOf(item.activity.name) }
    var target by remember { mutableIntStateOf(item.activity.weeklyTarget) }
    var isSport by remember { mutableStateOf(item.activity.isSport) }
    var weekendOnly by remember { mutableStateOf(item.activity.weekendOnly) }
    var days by remember { mutableStateOf(item.typicalDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New activity" else "Edit activity") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Per week", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (target > 0) target-- }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Text("$target", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { if (target < 14) target++ }) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }

                CheckRow("Sport", isSport) { isSport = it }
                CheckRow("Weekend only", weekendOnly) { weekendOnly = it }

                Text("Typical days", style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in days,
                            onClick = { days = if (day in days) days - day else days + day },
                            label = { Text(day.shortName()) },
                        )
                    }
                }

                if (!isNew) {
                    TextButton(onClick = onToggleArchived) {
                        Text(if (item.activity.archived) "Restore activity" else "Archive activity")
                    }
                    if (!item.activity.archived) {
                        Text(
                            "Archiving hides the activity; its past sessions are kept.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        item.activity.copy(
                            name = name.trim(),
                            weeklyTarget = target,
                            isSport = isSport,
                            weekendOnly = weekendOnly,
                        ),
                        days,
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}
