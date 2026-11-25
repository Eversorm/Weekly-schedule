package com.eversorm.weeklyschedule.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

/**
 * SQLite cannot store a LocalDate directly, so Room uses these two functions to convert
 * dates to a number (days since 1 Jan 1970) and back.
 */
class Converters {
    @TypeConverter fun dateToLong(date: LocalDate?): Long? = date?.toEpochDay()
    @TypeConverter fun longToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}

/**
 * The database itself. "version" must be increased whenever a table changes; Room then
 * needs a migration so existing data on your phone is kept (we'll handle that when needed).
 */
@Database(
    entities = [Activity::class, TemplateSlot::class, Session::class, InitializedWeek::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RoutineDatabase : RoomDatabase() {
    abstract fun dao(): RoutineDao

    companion object {
        fun create(context: Context): RoutineDatabase =
            Room.databaseBuilder(context, RoutineDatabase::class.java, "routine.db").build()
    }
}
