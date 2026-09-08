package app.vercel.Nighty3098.schedule.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query(
        """
        SELECT * FROM lessons
        WHERE groupQuery = :group AND weekIndex = :week AND dayIndex = :day
        ORDER BY number ASC, timeFrom ASC
        """,
    )
    fun observeDay(group: String, week: Int, day: Int): Flow<List<LessonEntity>>

    @Query(
        """
        SELECT * FROM lessons
        WHERE groupQuery = :group AND weekIndex = :week AND dayIndex = :day
        ORDER BY number ASC, timeFrom ASC
        """,
    )
    suspend fun getDay(group: String, week: Int, day: Int): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE groupQuery = :group")
    suspend fun getGroupLessons(group: String): List<LessonEntity>

    @Query("DELETE FROM lessons WHERE groupQuery = :group")
    suspend fun clearGroup(group: String)

    @Query("SELECT COUNT(*) FROM lessons WHERE groupQuery = :group")
    suspend fun countLessons(group: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<LessonEntity>)

    @Transaction
    suspend fun replaceGroup(group: String, lessons: List<LessonEntity>) {
        clearGroup(group)
        if (lessons.isNotEmpty()) insertAll(lessons)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: ScheduleMetaEntity)

    @Query("SELECT * FROM schedule_meta WHERE groupQuery = :group")
    suspend fun getMeta(group: String): ScheduleMetaEntity?
}
