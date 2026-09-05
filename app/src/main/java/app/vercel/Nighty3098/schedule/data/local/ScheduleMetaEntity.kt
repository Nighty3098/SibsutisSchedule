package app.vercel.Nighty3098.schedule.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Метаданные кэша группы: когда обновляли, чьё расписание, сколько пар. */
@Entity(tableName = "schedule_meta")
data class ScheduleMetaEntity(
    @PrimaryKey val groupQuery: String,
    val title: String = "",
    val updatedAtMillis: Long = 0L,
    val lessonCount: Int = 0,
)
