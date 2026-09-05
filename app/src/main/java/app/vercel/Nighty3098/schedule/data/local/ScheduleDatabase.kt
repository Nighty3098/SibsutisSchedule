package app.vercel.Nighty3098.schedule.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LessonEntity::class, ScheduleMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
}
