package com.eversorm.weeklyschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eversorm.weeklyschedule.ui.AppRoot
import com.eversorm.weeklyschedule.ui.theme.WeeklyScheduleTheme

/**
 * The one and only Android "Activity" (= a window). Everything you see is drawn by
 * Compose inside it; switching screens happens in AppRoot.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // draw behind status/navigation bars; Scaffold adds the needed padding
        setContent {
            WeeklyScheduleTheme {
                AppRoot()
            }
        }
    }
}
