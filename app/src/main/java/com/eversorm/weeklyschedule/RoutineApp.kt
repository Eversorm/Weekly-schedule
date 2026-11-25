package com.eversorm.weeklyschedule

import android.app.Application
import com.eversorm.weeklyschedule.backup.BackupManager
import com.eversorm.weeklyschedule.data.RoutineDatabase
import com.eversorm.weeklyschedule.data.RoutineRepository

/**
 * Created by Android once, when the app process starts (declared in AndroidManifest.xml
 * as android:name=".RoutineApp"). Holds the single database and repository instances
 * that all screens (and later the widget and notifications) share.
 *
 * "by lazy" = created the first time it is used, not before.
 */
class RoutineApp : Application() {
    val database by lazy { RoutineDatabase.create(this) }
    val repository by lazy { RoutineRepository(database.dao()) }
    val backupManager by lazy { BackupManager(database.dao()) }
}
