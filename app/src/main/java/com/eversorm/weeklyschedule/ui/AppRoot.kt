package com.eversorm.weeklyschedule.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.eversorm.weeklyschedule.RoutineApp

/** The three screens of the app. */
enum class Screen { Week, Activities, Settings }

/**
 * Decides which screen is shown. With only three screens a simple variable is enough;
 * no navigation library needed. The phone's Back button returns to the week screen.
 */
@Composable
fun AppRoot() {
    var screen by rememberSaveable { mutableStateOf(Screen.Week) }
    BackHandler(enabled = screen != Screen.Week) { screen = Screen.Week }

    when (screen) {
        Screen.Week -> WeekScreen(
            onOpenActivities = { screen = Screen.Activities },
            onOpenSettings = { screen = Screen.Settings },
        )
        Screen.Activities -> ActivitiesScreen(onBack = { screen = Screen.Week })
        Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Week })
    }
}

/** Shortcut to reach the shared objects in RoutineApp from any screen. */
@Composable
fun routineApp(): RoutineApp = LocalContext.current.applicationContext as RoutineApp
